package com.karthi.sungmobile.core.youtube

import com.karthi.sungmobile.core.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.AudioTrackType
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.io.Closeable
import java.io.IOException
import java.util.UUID

/** Only this module knows about extractor DTOs, continuations and expiring media URLs. */
class YouTubeSource : CatalogSource, StreamResolver, Closeable {
    private val runner = ExtractorRunner()
    private val service = ServiceList.YouTube
    private val continuations = object : LinkedHashMap<PageToken, () -> CatalogPage>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<PageToken, () -> CatalogPage>) = size > 32
    }

    init { initializeExtractor() }

    override suspend fun search(query: String, kind: CatalogKind): CatalogPage = extract {
        require(query.isNotBlank() && query.length <= 500)
        val filter = when (kind) {
            CatalogKind.SONG -> YoutubeSearchQueryHandlerFactory.MUSIC_SONGS
            CatalogKind.ALBUM -> YoutubeSearchQueryHandlerFactory.MUSIC_ALBUMS
            CatalogKind.ARTIST -> YoutubeSearchQueryHandlerFactory.MUSIC_ARTISTS
            CatalogKind.PLAYLIST -> YoutubeSearchQueryHandlerFactory.MUSIC_PLAYLISTS
        }
        val extractor = service.getSearchExtractor(query.trim(), listOf(filter), "")
        extractor.fetchPage()
        page(query, extractor.initialPage, kind) { extractor.getPage(it) }
    }

    override suspend fun nextPage(token: PageToken): CatalogPage = extract {
        val continuation = synchronized(continuations) { continuations[token] }
            ?: throw SourceException(SourceFailure.EXPIRED_PAGE)
        continuation()
    }

    override suspend fun open(url: String): CatalogPage = extract {
        when (val link = YouTubeLink.parse(url)) {
            is YouTubeLink.Song -> {
                val info = StreamInfo.getInfo(service, link.url)
                CatalogPage(info.name, listOf(info.toSong()))
            }
            is YouTubeLink.Playlist -> {
                val extractor = service.getPlaylistExtractor(link.url)
                extractor.fetchPage()
                page(extractor.name, extractor.initialPage, CatalogKind.SONG) { extractor.getPage(it) }
            }
            is YouTubeLink.Artist -> {
                val channel = ChannelInfo.getInfo(service, link.url)
                val tab = channel.tabs.firstOrNull { it.contentFilters.contains("videos") }
                if (tab == null) {
                    return@extract CatalogPage(channel.name, emptyList(),
                        notice = CatalogNotice.ArtistBrowseUnavailable(channel.name))
                }
                val extractor = service.getChannelTabExtractor(tab)
                extractor.fetchPage()
                page("${channel.name} · channel videos", extractor.initialPage, CatalogKind.SONG) {
                    extractor.getPage(it)
                }
            }
        }
    }

    override suspend fun resolve(track: CatalogItem): ResolvedStream = extract {
        if (track.kind != CatalogKind.SONG) throw SourceException(SourceFailure.UNSUPPORTED)
        val link = YouTubeLink.parse(track.url) as? YouTubeLink.Song
            ?: throw SourceException(SourceFailure.INVALID_LINK)
        val info = StreamInfo.getInfo(service, link.url)
        val stream = info.audioStreams.filter {
            it.isUrl && it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP &&
                it.content.startsWith("https://") &&
                (it.audioTrackType == null || it.audioTrackType == AudioTrackType.ORIGINAL)
        }.maxByOrNull { it.averageBitrate }
            ?: throw SourceException(SourceFailure.UNSUPPORTED)
        val expiry = stream.content.toHttpUrlOrNull()?.queryParameter("expire")?.toLongOrNull()
            ?.takeIf { it <= Long.MAX_VALUE / 1000 }?.times(1000)
        ResolvedStream(info.toSong(), stream.content, stream.format?.mimeType,
            mapOf("User-Agent" to USER_AGENT), stream.averageBitrate, expiry)
    }

    private fun <T : InfoItem> page(
        title: String,
        result: InfoItemsPage<T>,
        kind: CatalogKind,
        fetch: (Page) -> InfoItemsPage<T>,
    ): CatalogPage {
        val items = result.items.mapNotNull { it.toCatalogItem(kind) }.distinctBy { it.id }
        if (items.isEmpty() && result.errors.isNotEmpty()) {
            throw SourceException(SourceFailure.EXTRACTION, result.errors.first())
        }
        val nextPage = result.nextPage
        val token = if (nextPage != null && Page.isValid(nextPage)) {
            PageToken(UUID.randomUUID().toString()).also { token ->
                synchronized(continuations) {
                    continuations[token] = { page(title, fetch(nextPage), kind, fetch) }
                }
            }
        } else null
        return CatalogPage(title, items, token, result.errors.isNotEmpty())
    }

    private suspend fun <T> extract(block: () -> T): T = try {
        // Limit the entire extraction, which can make several individual HTTP requests.
        val result = withTimeoutOrNull(60_000) { Result.success(runner.run(block)) }
            ?: throw SourceException(SourceFailure.NETWORK)
        result.getOrThrow()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: SourceException) {
        throw error
    } catch (error: Exception) {
        val failure = when (error) {
            is ReCaptchaException -> SourceFailure.AUTHENTICATION_REQUIRED
            is ContentNotAvailableException -> SourceFailure.UNAVAILABLE
            is HttpStatusException -> SourceFailure.RATE_LIMITED
            is IOException -> SourceFailure.NETWORK
            else -> SourceFailure.EXTRACTION
        }
        throw SourceException(failure, error)
    }

    override fun close() {
        runner.close()
        synchronized(continuations) { continuations.clear() }
    }

    companion object {
        private var initialized = false
        @Synchronized private fun initializeExtractor() {
            if (!initialized) {
                NewPipe.init(ExtractorDownloader(), Localization("en", "IN"), ContentCountry("IN"))
                initialized = true
            }
        }
    }
}

private fun InfoItem.toCatalogItem(requestedKind: CatalogKind): CatalogItem? = when (this) {
    is StreamInfoItem -> CatalogItem(url, CatalogKind.SONG, name, uploaderName.orEmpty(), url,
        thumbnails.maxByOrNull { it.height }?.url, duration.takeIf { it > 0 }?.times(1000))
    is PlaylistInfoItem -> CatalogItem(url,
        if (requestedKind == CatalogKind.ALBUM) CatalogKind.ALBUM else CatalogKind.PLAYLIST,
        name, uploaderName.orEmpty(), url, thumbnails.maxByOrNull { it.height }?.url)
    is ChannelInfoItem -> CatalogItem(url, CatalogKind.ARTIST, name, "YouTube artist / channel", url,
        thumbnails.maxByOrNull { it.height }?.url)
    else -> null
}

private fun StreamInfo.toSong() = CatalogItem(url, CatalogKind.SONG, name, uploaderName.orEmpty(), url,
    thumbnails.maxByOrNull { it.height }?.url, duration.takeIf { it > 0 }?.times(1000))
