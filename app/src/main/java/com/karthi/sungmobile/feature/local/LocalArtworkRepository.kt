package com.karthi.sungmobile.feature.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A bounded local-art cache. Audio scanning only looks up files; extraction runs afterward. */
class LocalArtworkRepository(private val context: Context) {
    private val resolver get() = context.contentResolver
    private val directory get() = File(context.filesDir, "local-artwork").apply { mkdirs() }

    fun cached(audio: Uri): String? = imageFile(audio).takeIf(File::isFile)?.let(::shareUri)

    suspend fun resolve(audio: Uri, sidecar: Uri? = null, recheckSidecar: Boolean = false): String? = withContext(Dispatchers.IO) {
        if (recheckSidecar && sidecar != null) {
            val updated = runCatching { resolver.openInputStream(sidecar)?.use(::readBounded) }
                .getOrNull()?.let(::decodeBounded)?.let { store(audio, it) }
            if (updated != null) return@withContext updated
        }
        cached(audio)?.let { return@withContext it }
        val sidecarBitmap = sidecar?.let { runCatching { resolver.openInputStream(it)?.use(::readBounded) }
            .getOrNull()?.let(::decodeBounded) }
        val embeddedBitmap = if (sidecarBitmap == null) runCatching {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(context, audio)
                retriever.embeddedPicture?.takeIf { it.size <= MAX_IMAGE_BYTES }
            }?.let(::decodeBounded)
        }.getOrNull() else null
        val bitmap = sidecarBitmap ?: embeddedBitmap ?: if (Build.VERSION.SDK_INT >= 29) {
            runCatching { resolver.loadThumbnail(audio, Size(512, 512), null) }.getOrNull()
        } else null
        bitmap?.let { store(audio, it) }
    }

    /** Best effort: the audio download succeeds even if its online thumbnail is unavailable. */
    suspend fun saveDownloaded(audio: Uri, artworkUrl: String?) = withContext(Dispatchers.IO) {
        if (cached(audio) != null || artworkUrl.isNullOrBlank()) return@withContext
        val url = runCatching { URL(artworkUrl) }.getOrNull() ?: return@withContext
        if (url.protocol != "https") return@withContext
        val connection = (url.openConnection() as? HttpURLConnection) ?: return@withContext
        try {
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000
            if (connection.responseCode !in 200..299) return@withContext
            if (connection.url.protocol != "https") return@withContext
            val bitmap = connection.inputStream.use(::readBounded)?.let(::decodeBounded)
            bitmap?.let { store(audio, it) }
        } finally { connection.disconnect() }
    }

    private fun imageFile(audio: Uri): File {
        val digest = MessageDigest.getInstance("SHA-256").digest(artworkCacheIdentity(audio).toByteArray())
        val key = digest.take(16).joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
        return File(directory, "$key.jpg")
    }

    private fun shareUri(file: File): String =
        FileProvider.getUriForFile(context, "${context.packageName}.artwork", file)
            .buildUpon().appendQueryParameter("v", file.lastModified().toString()).build().toString()

    private fun store(audio: Uri, bitmap: Bitmap): String? {
        val scaled = if (bitmap.width > 512 || bitmap.height > 512) {
            val factor = minOf(512f / bitmap.width, 512f / bitmap.height)
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * factor).toInt().coerceAtLeast(1),
                (bitmap.height * factor).toInt().coerceAtLeast(1), true)
        } else bitmap
        val bytes = ByteArrayOutputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 82, out)
            out.toByteArray()
        }
        if (bytes.isEmpty() || bytes.size > MAX_POSTER_BYTES) return null
        val target = imageFile(audio)
        val temporary = File(directory, "${UUID.randomUUID()}.tmp")
        return try {
            temporary.writeBytes(bytes)
            if (!temporary.renameTo(target)) return null
            prune()
            shareUri(target)
        } finally { temporary.delete() }
    }

    private fun prune() {
        val images = directory.listFiles()?.filter { it.extension == "jpg" }
            ?.sortedByDescending(File::lastModified) ?: return
        var total = 0L
        images.forEach { file ->
            total += file.length()
            if (total > MAX_CACHE_BYTES) file.delete()
        }
    }

    private fun decodeBounded(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth !in 1..8192 || bounds.outHeight !in 1..8192) return null
        val sample = BitmapFactory.Options().apply {
            inSampleSize = generateSequence(1) { it * 2 }.first { divisor ->
                bounds.outWidth / divisor <= 1024 && bounds.outHeight / divisor <= 1024
            }
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, sample)
    }

    private fun readBounded(input: InputStream): ByteArray? {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (output.size() + count > MAX_IMAGE_BYTES) return null
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    private companion object {
        const val MAX_IMAGE_BYTES = 6 * 1024 * 1024
        const val MAX_POSTER_BYTES = 512 * 1024
        const val MAX_CACHE_BYTES = 64L * 1024 * 1024
    }
}

internal fun artworkCacheIdentity(audio: Uri): String {
    val segments = audio.pathSegments
    // Downloads publish through external_primary; library scans use the external alias.
    return if (audio.authority == "media" && segments.size == 4 &&
        segments[1] == "audio" && segments[2] == "media") {
        val volume = if (segments[0] == "external_primary") "external" else segments[0]
        "media:$volume:${segments[3]}"
    } else audio.toString()
}
