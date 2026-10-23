package com.karthi.sungmobile.feature.downloads

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.karthi.sungmobile.MainActivity
import com.karthi.sungmobile.SungApplication
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.CatalogKind
import com.karthi.sungmobile.core.model.YouTubeLink
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Every attempt resolves a fresh URL; failed attempts leave no published audio. */
class AudioDownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private val container = (context.applicationContext as SungApplication).container
    private val repository = container.downloads

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val id = inputData.getString(VIDEO_ID)?.takeIf { VIDEO.matches(it) } ?: return@withContext Result.failure()
        val row = repository.row(id) ?: return@withContext Result.success()
        if (row.state == "CANCELED" || row.state == "COMPLETE") return@withContext Result.success()
        if (Build.VERSION.SDK_INT < 29 && row.destination == null) {
            repository.update(id, "FAILED", error = "Choose a download folder on this Android version")
            return@withContext Result.failure()
        }
        if (!allowedNetwork()) {
            repository.update(id, "PAUSED", error = "Waiting for allowed network")
            return@withContext Result.retry()
        }
        val notificationId = 31_000 + (id.hashCode() and 0x3fff)
        row.uri?.let { runCatching { applicationContext.contentResolver.delete(Uri.parse(it), null, null) } }
        repository.update(id, "RUNNING", bytes = 0, clearUri = true)
        var outputUri: Uri? = null
        var stage: File? = null
        var published = false
        try {
            setForeground(foreground(notificationId, row.title, 0))
            val url = YouTubeLink.Song(id).url
            val track = CatalogItem(url, CatalogKind.SONG, row.title, row.artist, url)
            val stream = container.catalog.resolve(track)
            val connection = (URL(stream.url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000; readTimeout = 25_000; instanceFollowRedirects = true
                stream.headers.forEach { (key, value) -> setRequestProperty(key, value) }
            }
            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) throw HttpError(connection.responseCode)
                val expected = connection.contentLengthLong.takeIf { it > 0 }
                val options = container.settings.deviceAudio.first()
                val budget = options.budgetBytes - repository.usedBytes()
                if (budget <= 0 || (expected != null && expected > budget)) throw IllegalStateException("Download budget reached")
                val required = (expected ?: 256L * 1024 * 1024) *
                    (if (row.destination == null && Build.VERSION.SDK_INT >= 29) 1 else 2)
                if (applicationContext.cacheDir.usableSpace < required + FREE_RESERVE)
                    throw IllegalStateException("Not enough free storage")
                val responseType = connection.contentType?.substringBefore(';')?.trim()
                if (responseType?.startsWith("text/") == true) throw IllegalStateException("Server returned text, not audio")
                val mime = stream.mimeType?.substringBefore(';')?.trim()?.takeIf { it.startsWith("audio/") }
                    ?: "audio/mp4"
                val filename = filename(row.title, id, mime)
                if (row.destination == null && Build.VERSION.SDK_INT >= 29) {
                    outputUri = createPendingMedia(filename, mime)
                    repository.update(id, "RUNNING", uri = outputUri.toString())
                } else {
                    stage = File(applicationContext.cacheDir, "sung-$id.part")
                }
                val sink = if (outputUri != null) applicationContext.contentResolver.openOutputStream(outputUri!!, "w")
                    ?: throw IllegalStateException("Cannot open download destination")
                else stage!!.outputStream()
                var copied = 0L
                connection.inputStream.use { source -> sink.use { target ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        if (isStopped) throw CancellationException("Download canceled")
                        val size = source.read(buffer)
                        if (size < 0) break
                        copied += size
                        if (copied > budget || applicationContext.cacheDir.usableSpace < FREE_RESERVE)
                            throw IllegalStateException("Storage limit reached")
                        target.write(buffer, 0, size)
                        if (copied % (512 * 1024) < size) {
                            if (!allowedNetwork()) throw NetworkPaused()
                            repository.update(id, "RUNNING", bytes = copied)
                            setForeground(foreground(notificationId, row.title,
                                expected?.let { (copied * 100 / it).toInt().coerceIn(0, 100) } ?: 0))
                        }
                    }
                    target.flush()
                } }
                if (copied <= 0 || (expected != null && copied != expected))
                    throw IllegalStateException("Incomplete audio response")
                validateAudio(outputUri, stage)
                if (isStopped || repository.row(id)?.state == "CANCELED")
                    throw CancellationException("Download canceled")
                if (outputUri != null) publishMedia(outputUri!!)
                else outputUri = publishTree(row.destination, stage!!, filename, mime)
                if (isStopped || repository.row(id)?.state == "CANCELED")
                    throw CancellationException("Download canceled")
                repository.update(id, "COMPLETE", uri = outputUri.toString(), bytes = copied)
                published = repository.row(id)?.state == "COMPLETE"
                if (!published) throw CancellationException("Download canceled")
                container.deviceMusic.scan()
                Result.success()
            } finally { connection.disconnect() }
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) {
                if (repository.row(id)?.state != "CANCELED")
                    repository.update(id, "PAUSED", error = "Download interrupted", clearUri = true)
            }
            throw cancelled
        } catch (_: NetworkPaused) {
            repository.update(id, "PAUSED", error = "Waiting for allowed network", clearUri = true)
            Result.retry()
        } catch (error: HttpError) {
            if (error.code in setOf(403, 410) && runAttemptCount < 2) {
                repository.update(id, "PAUSED", error = "Refreshing expired stream", clearUri = true)
                Result.retry()
            } else {
                repository.update(id, "FAILED", error = "Server returned HTTP ${error.code}", clearUri = true)
                Result.failure()
            }
        } catch (_: IOException) {
            if (runAttemptCount < 3) {
                repository.update(id, "PAUSED", error = "Network interrupted; retrying", clearUri = true)
                Result.retry()
            } else {
                repository.update(id, "FAILED", error = "Network failed after retries", clearUri = true)
                Result.failure()
            }
        } catch (error: Exception) {
            repository.update(id, "FAILED", error = error.message?.take(160) ?: "Download failed", clearUri = true)
            Result.failure()
        } finally {
            stage?.delete()
            if (!published) outputUri?.let { runCatching { applicationContext.contentResolver.delete(it, null, null) } }
        }
    }

    private suspend fun allowedNetwork(): Boolean {
        val manager = applicationContext.getSystemService(ConnectivityManager::class.java)
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return false
        val wifiOnly = container.settings.deviceAudio.first().wifiOnly
        return !wifiOnly || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun createPendingMedia(name: String, mime: String): Uri {
        val collection = MediaStore.Audio.Media.getContentUri("external_primary")
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, name)
            put(MediaStore.Audio.Media.MIME_TYPE, mime)
            put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/SungMobile/")
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        return applicationContext.contentResolver.insert(collection, values)
            ?: throw IllegalStateException("Cannot create Music/SungMobile file")
    }

    private fun publishMedia(uri: Uri) {
        val changed = applicationContext.contentResolver.update(uri, ContentValues().apply {
            put(MediaStore.Audio.Media.IS_PENDING, 0)
        }, null, null)
        if (changed != 1) throw IllegalStateException("Could not publish audio file")
    }

    private fun publishTree(treeRaw: String?, file: File, name: String, mime: String): Uri {
        val tree = treeRaw?.let(Uri::parse) ?: throw IllegalStateException("Choose a download folder on this Android version")
        val root = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val temporary = DocumentsContract.createDocument(applicationContext.contentResolver, root,
            mime, ".$name.part") ?: throw IllegalStateException("Cannot write to selected folder")
        try {
            applicationContext.contentResolver.openOutputStream(temporary, "w")?.use { output ->
                file.inputStream().use { it.copyTo(output) }
            } ?: throw IllegalStateException("Cannot copy to selected folder")
            return DocumentsContract.renameDocument(applicationContext.contentResolver, temporary, name)
                ?: throw IllegalStateException("Selected folder cannot publish the file")
        } catch (error: Exception) {
            runCatching { DocumentsContract.deleteDocument(applicationContext.contentResolver, temporary) }
            throw error
        }
    }

    private fun validateAudio(uri: Uri?, file: File?) {
        val reader = MediaMetadataRetriever()
        try {
            if (uri != null) reader.setDataSource(applicationContext, uri)
            else reader.setDataSource(file?.absolutePath ?: throw IllegalStateException("Missing audio file"))
            val duration = reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0
            if (duration <= 0) throw IllegalStateException("Downloaded file is not playable audio")
        } finally { reader.release() }
    }

    private fun foreground(id: Int, title: String, progress: Int): ForegroundInfo {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Audio downloads", NotificationManager.IMPORTANCE_LOW))
        val intent = PendingIntent.getActivity(applicationContext, 0, Intent(applicationContext, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = Notification.Builder(applicationContext, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Downloading audio")
            .setContentText(title)
            .setContentIntent(intent)
            .setOngoing(true)
            .setProgress(100, progress, progress == 0)
            .build()
        return if (Build.VERSION.SDK_INT >= 29) ForegroundInfo(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            else ForegroundInfo(id, notification)
    }

    private fun filename(title: String, id: String, mime: String): String {
        val clean = title.replace(Regex("[^A-Za-z0-9 _.-]"), "_").trim().take(90).ifBlank { "Audio" }
        val extension = when (mime) { "audio/webm" -> "webm"; "audio/mpeg" -> "mp3";
            "audio/ogg" -> "ogg"; "audio/aac" -> "aac"; else -> "m4a" }
        return "$clean-$id.$extension"
    }

    private class NetworkPaused : Exception()
    private class HttpError(val code: Int) : Exception()

    companion object {
        const val VIDEO_ID = "videoId"
        private const val CHANNEL = "sung_downloads"
        private const val FREE_RESERVE = 256L * 1024 * 1024
        private val VIDEO = Regex("[A-Za-z0-9_-]{11}")
    }
}
