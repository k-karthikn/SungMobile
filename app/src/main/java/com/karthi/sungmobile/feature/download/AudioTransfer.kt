package com.karthi.sungmobile.feature.download

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.StreamResolver
import com.karthi.sungmobile.feature.local.SUNG_MUSIC_PATH
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/** One request, one pending MediaStore row. No history, inventory, or destination preference. */
internal class AudioTransfer(private val context: Context, private val streams: StreamResolver) {
    @Volatile private var connection: HttpURLConnection? = null
    @Volatile private var cancelled = false
    val isCancelled: Boolean get() = cancelled

    fun cancel() { cancelled = true; connection?.disconnect() }

    suspend fun run(track: CatalogItem, progress: (Long, Long?) -> Unit): Uri = withContext(Dispatchers.IO) {
        check(Build.VERSION.SDK_INT >= 29) { "Downloads require Android 10 or newer" }
        check(!cancelled)
        val stream = streams.resolve(track)
        currentCoroutineContext().ensureActive()
        check(!cancelled)
        val url = URL(stream.url)
        require(url.protocol == "https") { "An HTTPS audio stream is required" }
        val http = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 25_000
            instanceFollowRedirects = true
            setRequestProperty("Accept-Encoding", "identity")
            stream.headers.forEach { (key, value) -> setRequestProperty(key, value) }
        }
        connection = http
        var mediaUri: Uri? = null
        var published = false
        try {
            http.connect()
            if (http.responseCode != HttpURLConnection.HTTP_OK)
                throw IOException("Audio server returned ${http.responseCode}")
            val mime = stream.mimeType?.substringBefore(';')?.trim()?.takeIf { it.startsWith("audio/") }
                ?: http.contentType?.substringBefore(';')?.trim()?.takeIf { it.startsWith("audio/") }
                ?: throw IOException("The source did not provide audio")
            val extension = when (mime.lowercase()) {
                "audio/mp4", "audio/x-m4a" -> "m4a"
                "audio/webm" -> "webm"
                "audio/ogg", "audio/opus" -> "ogg"
                "audio/mpeg", "audio/mp3" -> "mp3"
                "audio/aac" -> "aac"
                "audio/flac" -> "flac"
                else -> throw IOException("Unsupported audio type: $mime")
            }
            val filename = safeAudioFilename(track.title, extension)
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, filename)
                put(MediaStore.Audio.Media.TITLE, track.title.take(200))
                put(MediaStore.Audio.Media.ARTIST, track.subtitle.take(200))
                put(MediaStore.Audio.Media.MIME_TYPE, mime)
                put(MediaStore.Audio.Media.RELATIVE_PATH, SUNG_MUSIC_PATH)
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            mediaUri = resolver.insert(MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values)
                ?: throw IOException("Could not create the audio file")
            val total = http.contentLengthLong.takeIf { it > 0 }
            var copied = 0L
            var lastUpdate = 0L
            val output = resolver.openOutputStream(mediaUri, "w") ?: throw IOException("Could not write the audio file")
            output.use { destination ->
                http.inputStream.use { source ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        if (cancelled) throw IOException("Download cancelled")
                        val count = source.read(buffer)
                        if (count < 0) break
                        destination.write(buffer, 0, count)
                        copied += count
                        val now = android.os.SystemClock.elapsedRealtime()
                        if (now - lastUpdate >= 500L) { progress(copied, total); lastUpdate = now }
                    }
                    destination.flush()
                }
            }
            if (cancelled) throw IOException("Download cancelled")
            if (copied <= 0 || (total != null && copied != total)) throw IOException("Incomplete audio transfer")
            currentCoroutineContext().ensureActive()
            progress(copied, total)
            if (resolver.update(mediaUri, ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) },
                    null, null) != 1) throw IOException("Could not publish the audio file")
            published = true
            mediaUri
        } finally {
            http.disconnect()
            connection = null
            if (!published) mediaUri?.let { runCatching { context.contentResolver.delete(it, null, null) } }
        }
    }
}

internal fun safeAudioFilename(title: String, extension: String): String {
    val stem = title.trim().replace(Regex("[^\\p{L}\\p{N} ._()-]"), "_")
        .trim(' ', '.').take(80).ifEmpty { "Song" }
    return "$stem-${UUID.randomUUID().toString().take(8)}.$extension"
}
