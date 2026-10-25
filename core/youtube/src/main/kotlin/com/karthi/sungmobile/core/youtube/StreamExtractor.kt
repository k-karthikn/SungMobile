package com.karthi.sungmobile.core.youtube

import com.karthi.sungmobile.core.model.*
import com.karthi.sungmobile.core.youtube.catalog.toSong
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.*

internal class StreamExtractor {
    private val service = ServiceList.YouTube
    fun resolve(track: CatalogItem): ResolvedStream = resolve(track, download = false)
    fun resolveDownload(track: CatalogItem): ResolvedStream = resolve(track, download = true)

    private fun resolve(track: CatalogItem, download: Boolean): ResolvedStream {
        if (track.kind != CatalogKind.SONG) throw SourceException(SourceFailure.UNSUPPORTED)
        val link = YouTubeLink.parse(track.url) as? YouTubeLink.Song
            ?: throw SourceException(SourceFailure.INVALID_LINK)
        val info = StreamInfo.getInfo(service, link.url)
        val stream = info.audioStreams.filter {
            it.isUrl && it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP &&
                it.content.startsWith("https://") &&
                (it.audioTrackType == null || it.audioTrackType == AudioTrackType.ORIGINAL) &&
                (!download || isDownloadableAudioMime(it.format?.mimeType))
        }.maxByOrNull { it.averageBitrate }
            ?: throw SourceException(SourceFailure.UNSUPPORTED)
        val expiry = stream.content.toHttpUrlOrNull()?.queryParameter("expire")?.toLongOrNull()
            ?.takeIf { it <= Long.MAX_VALUE / 1000 }?.times(1000)
        return ResolvedStream(info.toSong(), stream.content, stream.format?.mimeType,
            mapOf("User-Agent" to USER_AGENT), stream.averageBitrate, expiry)
    }

}

/** WebM/Opus plays in Media3 but some Android MediaStore providers reject it as Music. */
internal fun isDownloadableAudioMime(mime: String?): Boolean = mime?.lowercase() in setOf(
    "audio/mp4", "audio/x-m4a", "audio/mpeg", "audio/mp3", "audio/aac", "audio/flac")
