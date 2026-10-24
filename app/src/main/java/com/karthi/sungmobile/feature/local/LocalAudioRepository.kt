package com.karthi.sungmobile.feature.local

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Media as AudioMedia
import androidx.core.content.ContextCompat
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.CatalogKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal const val SUNG_MUSIC_PATH = "Music/SungMobile/"

data class LocalSong(val track: CatalogItem, val album: String?, val identity: String)
data class LocalScan(val songs: List<LocalSong>, val needsAudioPermission: Boolean,
    val unreadableFolders: Int = 0)

/** No inventory is stored. Every opening/resume reads MediaStore and the user's persisted trees. */
class LocalAudioRepository(private val context: Context) {
    private val resolver get() = context.contentResolver
    private val preferences = context.getSharedPreferences("local_audio_folders", Context.MODE_PRIVATE)

    fun hasAudioPermission(): Boolean = ContextCompat.checkSelfPermission(context, audioPermission()) ==
        PackageManager.PERMISSION_GRANTED

    fun audioPermission(): String = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE

    fun addedFolders(): List<Uri> = preferences.getStringSet("trees", emptySet()).orEmpty()
        .mapNotNull { runCatching { Uri.parse(it) }.getOrNull() }.sortedBy(Uri::toString)

    fun addFolder(uri: Uri) {
        resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        preferences.edit().putStringSet("trees", addedFolders().map(Uri::toString).toSet() + uri.toString()).apply()
    }

    fun removeFolder(uri: Uri) {
        preferences.edit().putStringSet("trees", addedFolders().map(Uri::toString).toSet() - uri.toString()).apply()
        runCatching { resolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }

    suspend fun scan(): LocalScan = withContext(Dispatchers.IO) {
        val songs = LinkedHashMap<String, LocalSong>()
        var denied = false
        var unreadable = 0
        try { scanDefault(songs) } catch (_: SecurityException) { denied = true }
        addedFolders().forEach { tree ->
            try { scanTree(tree, songs) } catch (_: Exception) { unreadable++ }
        }
        LocalScan(songs.values.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.track.title }),
            !hasAudioPermission() || denied, unreadable)
    }

    private fun scanDefault(result: MutableMap<String, LocalSong>) {
        val columns = mutableListOf(AudioMedia._ID, AudioMedia.DISPLAY_NAME, AudioMedia.TITLE, AudioMedia.ARTIST,
            AudioMedia.ALBUM, AudioMedia.DURATION, AudioMedia.MIME_TYPE)
        val selection: String
        val args: Array<String>
        if (Build.VERSION.SDK_INT >= 29) {
            columns += MediaStore.MediaColumns.RELATIVE_PATH
            selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? AND ${MediaStore.MediaColumns.IS_PENDING}=0"
            args = arrayOf("$SUNG_MUSIC_PATH%")
        } else {
            // API 26–28 has no RELATIVE_PATH. DATA is used only to identify this folder;
            // the path is never opened or passed to playback.
            columns += AudioMedia.DATA
            val folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                .resolve("SungMobile").absolutePath.trimEnd('/')
            selection = "${AudioMedia.DATA} LIKE ?"
            args = arrayOf("$folder/%")
        }
        resolver.query(AudioMedia.EXTERNAL_CONTENT_URI, columns.toTypedArray(), selection,
            args, "${AudioMedia.TITLE} COLLATE NOCASE ASC")?.use { cursor ->
            val id = cursor.getColumnIndexOrThrow(AudioMedia._ID)
            val display = cursor.getColumnIndexOrThrow(AudioMedia.DISPLAY_NAME)
            val title = cursor.getColumnIndexOrThrow(AudioMedia.TITLE)
            val artist = cursor.getColumnIndexOrThrow(AudioMedia.ARTIST)
            val album = cursor.getColumnIndexOrThrow(AudioMedia.ALBUM)
            val duration = cursor.getColumnIndexOrThrow(AudioMedia.DURATION)
            val mime = cursor.getColumnIndexOrThrow(AudioMedia.MIME_TYPE)
            val legacyPath = if (Build.VERSION.SDK_INT < 29) cursor.getColumnIndexOrThrow(AudioMedia.DATA) else -1
            while (cursor.moveToNext()) {
                val mediaType = cursor.getString(mime).orEmpty()
                if (!mediaType.startsWith("audio/", ignoreCase = true)) continue
                val mediaId = cursor.getLong(id)
                val uri = ContentUris.withAppendedId(AudioMedia.EXTERNAL_CONTENT_URI, mediaId)
                val identity = if (legacyPath >= 0) cursor.getString(legacyPath)?.let { "path:$it" }
                    ?: "media:$mediaId" else "media:$mediaId"
                val name = cursor.getString(display).orEmpty()
                val item = CatalogItem("local:$uri", CatalogKind.SONG,
                    cursor.getString(title)?.takeIf(String::isNotBlank) ?: filenameTitle(name),
                    cursor.getString(artist)?.takeUnless { it.isBlank() || it == "<unknown>" } ?: "Unknown artist",
                    uri.toString(), durationMs = cursor.getLong(duration).takeIf { it > 0 })
                result[identity] = LocalSong(item,
                    cursor.getString(album)?.takeUnless { it.isBlank() || it == "<unknown>" }, identity)
            }
        }
    }

    private fun scanTree(tree: Uri, result: MutableMap<String, LocalSong>) {
        val directories = ArrayDeque<String>().apply { add(DocumentsContract.getTreeDocumentId(tree)) }
        val visited = HashSet<String>()
        while (directories.isNotEmpty() && visited.size < 20_000) {
            val directory = directories.removeFirst()
            if (!visited.add(directory)) continue
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, directory)
            resolver.query(children, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE),
                null, null, null)?.use { cursor ->
                val id = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val name = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mime = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                while (cursor.moveToNext()) {
                    val documentId = cursor.getString(id) ?: continue
                    val mediaType = cursor.getString(mime).orEmpty()
                    if (mediaType == DocumentsContract.Document.MIME_TYPE_DIR) {
                        directories.add(documentId)
                        continue
                    }
                    if (!mediaType.startsWith("audio/", ignoreCase = true)) continue
                    val uri = DocumentsContract.buildDocumentUriUsingTree(tree, documentId)
                    val mediaUri = if (Build.VERSION.SDK_INT >= 29)
                        runCatching { MediaStore.getMediaUri(context, uri) }.getOrNull() else null
                    val identity = mediaUri?.let { mapped -> runCatching {
                        "media:${ContentUris.parseId(mapped)}"
                    }.getOrNull() } ?: legacyTreeIdentity(documentId)
                        ?: "${uri.authority}:$documentId"
                    if (identity in result) continue
                    val metadata = readMetadata(uri)
                    val item = CatalogItem("local:$uri", CatalogKind.SONG,
                        metadata.title ?: filenameTitle(cursor.getString(name).orEmpty()),
                        metadata.artist ?: "Unknown artist", uri.toString(),
                        durationMs = metadata.durationMs)
                    result[identity] = LocalSong(item, metadata.album, identity)
                }
            }
        }
    }

    private data class Metadata(val title: String?, val artist: String?, val album: String?, val durationMs: Long?)

    private fun readMetadata(uri: Uri): Metadata = runCatching {
        MediaMetadataRetriever().use { retriever ->
            retriever.setDataSource(context, uri)
            fun field(key: Int) = retriever.extractMetadata(key)?.takeIf(String::isNotBlank)
            Metadata(field(MediaMetadataRetriever.METADATA_KEY_TITLE),
                field(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                field(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                field(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull())
        }
    }.getOrDefault(Metadata(null, null, null, null))
}

internal fun filenameTitle(name: String): String = name.substringBeforeLast('.', name).ifBlank { "Untitled" }

/** API 26–28: document IDs from the primary storage provider can match DATA metadata for deduplication. */
private fun legacyTreeIdentity(documentId: String): String? {
    if (Build.VERSION.SDK_INT >= 29 || !documentId.startsWith("primary:")) return null
    val relative = documentId.substringAfter(':').trimStart('/')
    if (relative.contains("..")) return null
    return "path:${Environment.getExternalStorageDirectory().resolve(relative).absolutePath}"
}
