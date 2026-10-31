package com.karthi.sungmobile.core.data.local

import android.Manifest
import android.content.ContentUris
import android.content.Context
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
import java.util.Locale

/** Reads audio rows and user-granted SAF trees without owning scan caching or UI state. */
internal class LocalAudioScanner(
    private val context: Context,
    private val artwork: LocalArtworkRepository,
) {
    private val resolver get() = context.contentResolver

    fun hasAudioPermission(): Boolean = ContextCompat.checkSelfPermission(context, audioPermission()) ==
        PackageManager.PERMISSION_GRANTED

    fun audioPermission(): String = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE

    fun scan(trees: List<Uri>): LocalScan {
        val songs = LinkedHashMap<String, LocalSong>()
        var permissionDenied = false
        var unreadableFolders = 0
        try {
            scanDefault(songs)
        } catch (_: SecurityException) {
            permissionDenied = true
        }
        trees.forEach { tree ->
            try {
                scanTree(tree, songs)
            } catch (_: Exception) {
                unreadableFolders++
            }
        }
        return LocalScan(
            songs.values.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.track.title }),
            !hasAudioPermission() || permissionDenied,
            unreadableFolders,
        )
    }

    private fun scanDefault(result: MutableMap<String, LocalSong>) {
        val columns = mutableListOf(AudioMedia._ID, AudioMedia.DISPLAY_NAME, AudioMedia.TITLE, AudioMedia.ARTIST,
            AudioMedia.ALBUM, AudioMedia.DURATION, AudioMedia.MIME_TYPE)
        // Match Audilux: local songs means the device audio library, including Music/songs
        // and files indexed from other folders. The old RELATIVE_PATH filter silently hid
        // everything outside Music/SungMobile.
        val selection = if (Build.VERSION.SDK_INT >= 29) "${MediaStore.MediaColumns.IS_PENDING}=0" else null
        resolver.query(AudioMedia.EXTERNAL_CONTENT_URI, columns.toTypedArray(), selection,
            null, "${AudioMedia.TITLE} COLLATE NOCASE ASC")?.use { cursor ->
            val id = cursor.getColumnIndexOrThrow(AudioMedia._ID)
            val display = cursor.getColumnIndexOrThrow(AudioMedia.DISPLAY_NAME)
            val title = cursor.getColumnIndexOrThrow(AudioMedia.TITLE)
            val artist = cursor.getColumnIndexOrThrow(AudioMedia.ARTIST)
            val album = cursor.getColumnIndexOrThrow(AudioMedia.ALBUM)
            val duration = cursor.getColumnIndexOrThrow(AudioMedia.DURATION)
            val mime = cursor.getColumnIndexOrThrow(AudioMedia.MIME_TYPE)
            while (cursor.moveToNext()) {
                val mediaType = cursor.getString(mime).orEmpty()
                if (!mediaType.startsWith("audio/", ignoreCase = true)) continue
                val mediaId = cursor.getLong(id)
                val uri = ContentUris.withAppendedId(AudioMedia.EXTERNAL_CONTENT_URI, mediaId)
                val identity = "media:$mediaId"
                val name = cursor.getString(display).orEmpty()
                val item = CatalogItem("local:$uri", CatalogKind.SONG,
                    cursor.getString(title)?.takeIf(String::isNotBlank) ?: filenameTitle(name),
                    cursor.getString(artist)?.takeUnless { it.isBlank() || it == "<unknown>" } ?: "Unknown artist",
                    uri.toString(), artworkUrl = artwork.cached(uri),
                    durationMs = cursor.getLong(duration).takeIf { it > 0 })
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
            val audioFiles = ArrayList<Pair<Uri, String>>()
            val images = HashMap<String, Uri>()
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
                    val uri = DocumentsContract.buildDocumentUriUsingTree(tree, documentId)
                    val filename = cursor.getString(name).orEmpty()
                    when {
                        mediaType.startsWith("audio/", ignoreCase = true) -> audioFiles += uri to filename
                        mediaType.startsWith("image/", ignoreCase = true) &&
                            filename.substringAfterLast('.', "").lowercase(Locale.ROOT) in LOCAL_IMAGE_EXTENSIONS ->
                            images[filename.lowercase(Locale.ROOT)] = uri
                    }
                }
            }
            audioFiles.forEach { (uri, filename) ->
                val documentId = DocumentsContract.getDocumentId(uri)
                val mediaUri = if (Build.VERSION.SDK_INT >= 29)
                    runCatching { MediaStore.getMediaUri(context, uri) }.getOrNull() else null
                val identity = mediaUri?.let { mapped -> runCatching {
                    "media:${ContentUris.parseId(mapped)}"
                }.getOrNull() } ?: legacyTreeIdentity(documentId)
                    ?: "${uri.authority}:$documentId"
                val sidecar = localCover(filename, images)
                if (identity in result) {
                    if (sidecar != null) result[identity]?.let { existing ->
                        result[identity] = existing.copy(sidecarArt = sidecar)
                    }
                    return@forEach
                }
                val metadata = readMetadata(uri)
                val item = CatalogItem("local:$uri", CatalogKind.SONG,
                    metadata.title ?: filenameTitle(filename), metadata.artist ?: "Unknown artist",
                    uri.toString(), artworkUrl = artwork.cached(uri), durationMs = metadata.durationMs)
                result[identity] = LocalSong(item, metadata.album, identity, sidecarArt = sidecar)
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

    /** API 26–28: document IDs can match DATA metadata for deduplication. */
    private fun legacyTreeIdentity(documentId: String): String? {
        if (Build.VERSION.SDK_INT >= 29 || !documentId.startsWith("primary:")) return null
        val relative = documentId.substringAfter(':').trimStart('/')
        if (relative.contains("..")) return null
        return "path:${Environment.getExternalStorageDirectory().resolve(relative).absolutePath}"
    }
}
