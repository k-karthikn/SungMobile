package com.karthi.sungmobile.core.youtube.catalog

import com.karthi.sungmobile.core.model.*
import com.karthi.sungmobile.core.model.catalog.CatalogDetails
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory

internal class CatalogExtractor {
    private val service = ServiceList.YouTube
    private val pages = CatalogPages()
    fun search(query: String, kind: CatalogKind): CatalogPage {
        require(query.isNotBlank() && query.length <= 500)
        val filter = when (kind) {
            CatalogKind.SONG -> YoutubeSearchQueryHandlerFactory.MUSIC_SONGS
            CatalogKind.ALBUM -> YoutubeSearchQueryHandlerFactory.MUSIC_ALBUMS
            CatalogKind.ARTIST -> YoutubeSearchQueryHandlerFactory.MUSIC_ARTISTS
            CatalogKind.PLAYLIST -> YoutubeSearchQueryHandlerFactory.MUSIC_PLAYLISTS
        }
        val extractor = service.getSearchExtractor(query.trim(), listOf(filter), "")
        extractor.fetchPage()
        return pages.page(query, extractor.initialPage, kind) { extractor.getPage(it) }
    }
    fun nextPage(token: PageToken) = pages.next(token)
    fun clear() = pages.clear()
    fun related(seed: CatalogItem, limit: Int): List<CatalogItem> {
        val link = YouTubeLink.parse(seed.url) as? YouTubeLink.Song ?: return emptyList()
        val info = StreamInfo.getInfo(service, link.url)
        return info.relatedItems.orEmpty().mapNotNull { it.toCatalogItem(CatalogKind.SONG) }
            .filter { it.kind == CatalogKind.SONG && it.id != seed.id &&
                (it.durationMs ?: 0) in 60_000..900_000 &&
                runCatching { YouTubeLink.parse(it.url) is YouTubeLink.Song }.getOrDefault(false) }
            .distinctBy { it.id }.take(limit.coerceIn(0, 5))
    }
    fun open(url: String): CatalogPage = when (val link = YouTubeLink.parse(url)) {
        is YouTubeLink.Song -> {
            val info = StreamInfo.getInfo(service, link.url)
            val song = info.toSong()
            CatalogPage(info.name, listOf(song), details = CatalogDetails(link.url, CatalogKind.SONG, song.subtitle, song.artworkUrl, 1))
        }
        is YouTubeLink.Playlist -> {
            val extractor = service.getPlaylistExtractor(link.url)
            extractor.fetchPage()
            val details = CatalogDetails(link.url, CatalogKind.PLAYLIST,
                runCatching { extractor.uploaderName }.getOrDefault(""),
                runCatching { extractor.thumbnails.maxByOrNull { it.height }?.url }.getOrNull(),
                runCatching { extractor.streamCount.takeIf { it >= 0 } }.getOrNull())
            pages.page(extractor.name, extractor.initialPage, CatalogKind.SONG, details) { extractor.getPage(it) }
        }
        is YouTubeLink.Artist -> {
            val channel = ChannelInfo.getInfo(service, link.url)
            val details = CatalogDetails(link.url, CatalogKind.ARTIST, artworkUrl = channel.avatars.maxByOrNull { it.height }?.url, channelVideos = true)
            val tab = channel.tabs.firstOrNull { it.contentFilters.contains("videos") }
            if (tab == null) CatalogPage(channel.name, emptyList(), notice = CatalogNotice.ArtistBrowseUnavailable(channel.name), details = details)
            else {
                val extractor = service.getChannelTabExtractor(tab)
                extractor.fetchPage()
                pages.page("${channel.name} · channel videos", extractor.initialPage, CatalogKind.SONG, details) { extractor.getPage(it) }
            }
        }
        is YouTubeLink.Browse -> {
            if (link.browseId.startsWith("UC")) open("https://www.youtube.com/channel/${link.browseId}")
            else throw SourceException(SourceFailure.UNSUPPORTED)
        }
    }
}
