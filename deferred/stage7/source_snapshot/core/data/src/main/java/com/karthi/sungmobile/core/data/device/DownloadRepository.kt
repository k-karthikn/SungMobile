package com.karthi.sungmobile.core.data.device

import android.content.Context
import android.net.Uri
import com.karthi.sungmobile.core.data.db.DownloadEntity
import com.karthi.sungmobile.core.data.db.SungDatabase
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.YouTubeLink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class AudioDownload(val videoId: String, val title: String, val artist: String,
    val state: String, val uri: String?, val bytes: Long, val error: String?, val destination: String?)
data class DownloadInventory(val items: List<AudioDownload> = emptyList(), val usedBytes: Long = 0)

/** Durable IDs and content URIs only. Resolved YouTube URLs never enter Room. */
class DownloadRepository(private val context: Context, private val database: SungDatabase) {
    private val dao = database.deviceAudioDao()
    private val mutable = MutableStateFlow(DownloadInventory())
    val state = mutable.asStateFlow()

    suspend fun refresh() = withContext(Dispatchers.IO) {
        mutable.value = DownloadInventory(dao.downloads().map { it.toModel() }, dao.completedBytes())
    }

    suspend fun queue(track: CatalogItem, destination: String?): String = withContext(Dispatchers.IO) {
        val id = (YouTubeLink.parse(track.url) as? YouTubeLink.Song)?.videoId
            ?: throw IllegalArgumentException("Only YouTube songs can be downloaded")
        val old = dao.download(id)
        if (old?.state == "COMPLETE" || old?.state == "RUNNING" || old?.state == "QUEUED") return@withContext id
        dao.putDownload(DownloadEntity().apply {
            videoId = id; title = track.title.take(300); artist = track.subtitle.take(300)
            state = "QUEUED"; this.destination = destination; updatedAt = System.currentTimeMillis()
        })
        refresh()
        id
    }

    suspend fun row(videoId: String): AudioDownload? = withContext(Dispatchers.IO) { dao.download(videoId)?.toModel() }

    suspend fun update(videoId: String, state: String, uri: String? = null, bytes: Long? = null,
        error: String? = null, clearUri: Boolean = false) = withContext(Dispatchers.IO) {
        val old = dao.download(videoId) ?: return@withContext
        if (old.state == "CANCELED" && state != "QUEUED") return@withContext
        old.state = state; old.error = error; old.updatedAt = System.currentTimeMillis()
        if (clearUri) old.uri = null else if (uri != null) old.uri = uri
        if (bytes != null) old.bytes = bytes
        dao.putDownload(old)
        refresh()
    }

    suspend fun completedUri(videoId: String): Uri? = withContext(Dispatchers.IO) {
        val row = dao.download(videoId)?.takeIf { it.state == "COMPLETE" } ?: return@withContext null
        val uri = row.uri?.let(Uri::parse) ?: return@withContext null
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { return@withContext uri }
        } catch (_: Exception) { }
        row.state = "MISSING"; row.error = "Downloaded file is missing or access was revoked"
        dao.putDownload(row); refresh()
        null
    }

    suspend fun remove(videoId: String, deleteFile: Boolean = true) = withContext(Dispatchers.IO) {
        val row = dao.download(videoId) ?: return@withContext
        if (deleteFile) row.uri?.let { uri ->
            try { context.contentResolver.delete(Uri.parse(uri), null, null) }
            catch (_: Exception) {
                row.state = "FAILED"; row.error = "Could not delete this device file. Check folder access."
                dao.putDownload(row); refresh(); return@withContext
            }
        }
        dao.removeDownload(videoId)
        refresh()
    }

    suspend fun usedBytes(): Long = withContext(Dispatchers.IO) { dao.completedBytes() }

    private fun DownloadEntity.toModel() = AudioDownload(videoId, title, artist, state, uri, bytes,
        error, destination)
}
