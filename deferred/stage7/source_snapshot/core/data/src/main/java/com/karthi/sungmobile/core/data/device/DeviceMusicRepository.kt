package com.karthi.sungmobile.core.data.device

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import com.karthi.sungmobile.core.data.db.*
import com.karthi.sungmobile.core.data.settings.DeviceAudioSettings
import com.karthi.sungmobile.core.data.settings.SettingsRepository
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.CatalogKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class DeviceSong(val track: CatalogItem, val folder: String, val available: Boolean, val bytes: Long)
data class DeviceFolder(val uri: String, val name: String, val exclude: Boolean)
data class DeviceMusicState(val songs: List<DeviceSong> = emptyList(),
    val folders: List<DeviceFolder> = emptyList(), val settings: DeviceAudioSettings = DeviceAudioSettings(),
    val scanning: Boolean = false, val error: String? = null, val lastScanCount: Int? = null)

/** Device audio is indexed by content URI; no raw file path becomes a playback identity. */
class DeviceMusicRepository(private val context: Context, private val database: SungDatabase,
    private val settings: SettingsRepository) {
    private val dao = database.deviceAudioDao()
    private val lock = Mutex()
    private val mutable = MutableStateFlow(DeviceMusicState())
    val state = mutable.asStateFlow()

    fun hasAudioPermission(): Boolean = context.checkSelfPermission(if (Build.VERSION.SDK_INT >= 33)
        Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    suspend fun refresh() = withContext(Dispatchers.IO) {
        mutable.value = mutable.value.copy(songs = dao.tracks().map { it.toSong() },
            folders = dao.folders().map { DeviceFolder(it.uri, it.name, it.rule == "EXCLUDE") },
            settings = settings.deviceAudio.first())
    }

    suspend fun setSettings(value: DeviceAudioSettings) {
        settings.setDeviceAudio(value)
        refresh()
    }

    suspend fun addFolder(uri: Uri, name: String, exclude: Boolean) = withContext(Dispatchers.IO) {
        dao.putFolder(DeviceFolderEntity().apply {
            this.uri = uri.toString(); this.name = name.take(120)
            rule = if (exclude) "EXCLUDE" else "INCLUDE"
        })
        refresh()
    }

    suspend fun removeFolder(uri: String) = withContext(Dispatchers.IO) {
        dao.removeFolder(uri)
        dao.markMissing(uri)
        refresh()
    }

    suspend fun scan() = lock.withLock {
        mutable.value = mutable.value.copy(scanning = true, error = null)
        try {
            withContext(Dispatchers.IO) {
                val options = settings.deviceAudio.first()
                val rules = dao.folders()
                val exclusions = rules.filter { it.rule == "EXCLUDE" }.map { it.uri }
                var count = 0
                if (options.scanMusic) {
                    if (!hasAudioPermission()) {
                        dao.markMissing(DEFAULT_FOLDER)
                        mutable.value = mutable.value.copy(error = "Allow audio access to scan Music.")
                    } else {
                        val songs = scanMusic(exclusions)
                        replaceFolder(DEFAULT_FOLDER, songs)
                        count += songs.size
                    }
                } else dao.markMissing(DEFAULT_FOLDER)
                rules.filter { it.rule == "INCLUDE" }.forEach { rule ->
                    try {
                        val songs = scanTree(Uri.parse(rule.uri), exclusions)
                        replaceFolder(rule.uri, songs)
                        count += songs.size
                    } catch (_: SecurityException) {
                        dao.markMissing(rule.uri)
                        mutable.value = mutable.value.copy(error = "Access lost for ${rule.name}. Choose the folder again.")
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        dao.markMissing(rule.uri)
                        mutable.value = mutable.value.copy(error = "Could not scan ${rule.name}.")
                    }
                }
                val active = rules.filter { it.rule == "INCLUDE" }.map { it.uri }.toSet() +
                    (if (options.scanMusic) setOf(DEFAULT_FOLDER) else emptySet())
                dao.tracks().map { it.folderKey }.toSet().filter { it !in active }.forEach(dao::markMissing)
                refresh()
                mutable.value = mutable.value.copy(lastScanCount = count)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            mutable.value = mutable.value.copy(error = "Device music scan failed. Try Scan now.")
        } finally {
            mutable.value = mutable.value.copy(scanning = false)
        }
    }

    private fun replaceFolder(key: String, songs: List<DeviceTrackEntity>) {
        database.runInTransaction {
            dao.markMissing(key)
            if (songs.isNotEmpty()) dao.putTracks(songs)
        }
    }

    private suspend fun scanMusic(exclusions: List<String>): List<DeviceTrackEntity> {
        val base = if (Build.VERSION.SDK_INT >= 29)
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val columns = mutableListOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE, MediaStore.Audio.Media.DATE_MODIFIED)
        columns += if (Build.VERSION.SDK_INT >= 29) MediaStore.Audio.Media.RELATIVE_PATH else MediaStore.Audio.Media.DATA
        if (Build.VERSION.SDK_INT >= 29) columns += MediaStore.Audio.Media.IS_PENDING
        val result = ArrayList<DeviceTrackEntity>()
        context.contentResolver.query(base, columns.toTypedArray(), null, null, null)?.use { cursor ->
            val indexes = columns.map(cursor::getColumnIndexOrThrow)
            while (cursor.moveToNext() && result.size < MAX_SONGS) {
                currentCoroutineContext().ensureActive()
                if (Build.VERSION.SDK_INT >= 29 && cursor.getInt(indexes[7]) != 0) continue
                val location = cursor.getString(indexes[6]) ?: continue
                val path = if (Build.VERSION.SDK_INT >= 29) location
                    else "Music/${location.substringAfter("/Music/", "")}" 
                if (Build.VERSION.SDK_INT >= 29 && !path.startsWith("Music/", true)) continue
                if (Build.VERSION.SDK_INT < 29 && !location.contains("/Music/", true)) continue
                if (excludedMediaPath(path, exclusions)) continue
                val id = cursor.getLong(indexes[0])
                val uri = ContentUris.withAppendedId(base, id).toString()
                result += DeviceTrackEntity().apply {
                    this.uri = uri
                    title = cursor.getString(indexes[1])?.takeIf { it.isNotBlank() && it != "<unknown>" }
                        ?: "Audio $id"
                    artist = cursor.getString(indexes[2])?.takeIf { it != "<unknown>" }.orEmpty()
                    durationMs = cursor.getLong(indexes[3]).coerceAtLeast(0)
                    sizeBytes = cursor.getLong(indexes[4]).coerceAtLeast(0)
                    modifiedAt = cursor.getLong(indexes[5])
                    folderKey = DEFAULT_FOLDER
                    available = true
                }
            }
        }
        return result
    }

    private fun excludedMediaPath(path: String, exclusions: List<String>): Boolean = exclusions.any { raw ->
        val id = runCatching { DocumentsContract.getTreeDocumentId(Uri.parse(raw)) }.getOrNull().orEmpty()
        val relative = id.substringAfter(':', "").trimEnd('/')
        relative.isNotBlank() && (path.equals(relative, true) || path.startsWith("$relative/", true))
    }

    private suspend fun scanTree(tree: Uri, exclusions: List<String>): List<DeviceTrackEntity> {
        val root = DocumentsContract.getTreeDocumentId(tree)
        val ignored = exclusions.mapNotNull { raw ->
            val excluded = Uri.parse(raw)
            if (excluded.authority == tree.authority)
                runCatching { DocumentsContract.getTreeDocumentId(excluded) }.getOrNull()
            else null
        }
        val pending = ArrayDeque<String>().apply { add(root) }
        val result = ArrayList<DeviceTrackEntity>()
        var visited = 0
        while (pending.isNotEmpty() && visited < MAX_DOCUMENTS && result.size < MAX_SONGS) {
            currentCoroutineContext().ensureActive()
            val parent = pending.removeFirst()
            if (ignored.any { parent == it || parent.startsWith("$it/") }) continue
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parent)
            context.contentResolver.query(children, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE, DocumentsContract.Document.COLUMN_LAST_MODIFIED),
                null, null, null)?.use { cursor ->
                while (cursor.moveToNext() && visited++ < MAX_DOCUMENTS && result.size < MAX_SONGS) {
                    currentCoroutineContext().ensureActive()
                    val id = cursor.getString(0) ?: continue
                    if (ignored.any { id == it || id.startsWith("$it/") }) continue
                    val name = cursor.getString(1).orEmpty()
                    val mime = cursor.getString(2).orEmpty()
                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) { pending.add(id); continue }
                    if (!supportedAudio(name, mime)) continue
                    val uri = DocumentsContract.buildDocumentUriUsingTree(tree, id)
                    val duration = runCatching {
                        val reader = MediaMetadataRetriever()
                        try {
                            reader.setDataSource(context, uri)
                            reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0
                        } finally { reader.release() }
                    }.getOrDefault(0)
                    result += DeviceTrackEntity().apply {
                        this.uri = uri.toString()
                        title = name.substringBeforeLast('.').ifBlank { name }
                        artist = ""
                        folderKey = tree.toString()
                        durationMs = duration.coerceAtLeast(0)
                        sizeBytes = cursor.getLong(3).coerceAtLeast(0)
                        modifiedAt = cursor.getLong(4)
                        available = true
                    }
                }
            }
        }
        return result
    }

    private fun supportedAudio(name: String, mime: String): Boolean =
        mime.startsWith("audio/") || name.substringAfterLast('.', "").lowercase() in
            setOf("mp3", "m4a", "aac", "ogg", "opus", "flac", "wav", "webm")

    private fun DeviceTrackEntity.toSong() = DeviceSong(CatalogItem(uri, CatalogKind.SONG,
        title, artist, uri, durationMs = durationMs.takeIf { it > 0 }), folderKey, available, sizeBytes)

    companion object {
        const val DEFAULT_FOLDER = "Music/"
        private const val MAX_SONGS = 20_000
        private const val MAX_DOCUMENTS = 30_000
    }
}
