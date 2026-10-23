package com.karthi.sungmobile.playback

import android.content.Context
import android.net.Uri
import com.karthi.sungmobile.core.data.device.DownloadRepository
import com.karthi.sungmobile.core.model.*

/** Select a verified local URI before resolving a transient YouTube stream. */
class DeviceAwareStreamResolver(private val context: Context, private val youtube: StreamResolver,
    private val downloads: DownloadRepository) : StreamResolver {
    override suspend fun resolve(track: CatalogItem): ResolvedStream {
        if (track.source == TrackSource.DEVICE) {
            val uri = Uri.parse(track.url)
            if (uri.scheme != "content") throw SourceException(SourceFailure.UNSUPPORTED)
            try { context.contentResolver.openFileDescriptor(uri, "r")?.use { }
                ?: throw SourceException(SourceFailure.UNAVAILABLE) }
            catch (error: SourceException) { throw error }
            catch (_: Exception) { throw SourceException(SourceFailure.UNAVAILABLE) }
            return ResolvedStream(track, track.url, null, emptyMap(), 0, null)
        }
        val id = (runCatching { YouTubeLink.parse(track.url) }.getOrNull() as? YouTubeLink.Song)?.videoId
        if (id != null) downloads.completedUri(id)?.let { uri ->
            return ResolvedStream(track, uri.toString(), null, emptyMap(), 0, null)
        }
        return youtube.resolve(track)
    }
}
