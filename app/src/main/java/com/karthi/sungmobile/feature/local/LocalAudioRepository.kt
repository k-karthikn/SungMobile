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
import android.os.SystemClock
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Media as AudioMedia
import androidx.core.content.ContextCompat
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.CatalogKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Locale

internal const val SUNG_MUSIC_PATH = "Music/SungMobile/"

data class LocalSong(val track: CatalogItem, val album: String?, val identity: String,
    val sidecarArt: Uri? = null)
data class LocalScan(val songs: List<LocalSong>, val needsAudioPermission: Boolean,
    val unreadableFolders: Int = 0)

/** Keeps the last scan in memory so switching Library tabs does not walk SAF trees again. */
class LocalAudioRepository(private val context: Context) {
    private val resolver get() = context.contentResolver
    private val preferences = context.getSharedPreferences("local_audio_folders", Context.MODE_PRIVATE)
    private val artwork = LocalArtworkRepository(context)
    private val artworkScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var artworkJob: Job? = null
    private val scanMutex = Mutex()
    @Volatile private var cachedScan: LocalScan? = null
    @Volatile private var scannedAtMs = 0L
    @Volatile private var revision = 0L
    @Volatile private var sidecarsDirty = false
    private val mutableUpdates = MutableStateFlow<LocalScan?>(null)
    val updates = mutableUpdates.asStateFlow()

    fun snapshot(): LocalScan? = cachedScan
    fun invalidate() { revision++; artworkJob?.cancel(); cachedScan = null }

    fun hasAudioPermission(): Boolean = ContextCompat.checkSelfPermission(context, audioPermission()) ==
        PackageManager.PERMISSION_GRANTED

    fun audioPermission(): String = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE

    fun addedFolders(): List<Uri> = preferences.getStringSet("trees", emptySet()).orEmpty()
        .mapNotNull { runCatching { Uri.parse(it) }.getOrNull() }.sortedBy(Uri::toString)

    fun addFolder(uri: Uri) {
        resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        preferences.edit().putStringSet("trees", addedFolders().map(Uri::toString).toSet() + uri.toString()).apply()
        sidecarsDirty = true
        invalidate()
    }

    fun removeFolder(uri: Uri) {
        preferences.edit().putStringSet("trees", addedFolders().map(Uri::toString).toSet() - uri.toString()).apply()
        runCatching { resolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        sidecarsDirty = true
        invalidate()
    }

    suspend fun scan(force: Boolean = false): LocalScan = withContext(Dispatchers.IO) { scanMutex.withLock {
        val now = SystemClock.elapsedRealtime()
        cachedScan?.takeIf { !force && now - scannedAtMs < SCAN_FRESHNESS_MS }?.let { return@withLock it }
        val songs = LinkedHashMap<String, LocalSong>()
        var denied = false
        var unreadable = 0
        try { scanDefault(songs) } catch (_: SecurityException) { denied = true }
        addedFolders().forEach { tree ->
            try { scanTree(tree, songs) } catch (_: Exception) { unreadable++ }
        }
        val recheckSidecars = force || sidecarsDirty
        sidecarsDirty = false
        LocalScan(songs.values.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.track.title }),
            !hasAudioPermission() || denied, unreadable)
            .also { result ->
                val generation = ++revision
                artworkJob?.cancel()
                cachedScan = result
                mutableUpdates.value = result
                scannedAtMs = SystemClock.elapsedRealtime()
                artworkJob = artworkScope.launch { enrichArtwork(result, generation, recheckSidecars) }
            }
    } }

    private suspend fun enrichArtwork(initial: LocalScan, generation: Long, recheckSidecars: Boolean) {
        val songs = initial.songs.toMutableList()
        var changed = 0
        songs.forEachIndexed { index, song ->
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            if (revision != generation) return
            val recheck = recheckSidecars && song.sidecarArt != null
            if (song.track.artworkUrl != null && !recheck) return@forEachIndexed
            val audio = Uri.parse(song.track.url)
            val art = runCatching { artwork.resolve(audio, song.sidecarArt, recheck) }.getOrNull() ?: return@forEachIndexed
            if (revision != generation) return
            songs[index] = song.copy(track = song.track.copy(artworkUrl = art))
            changed++
            if (changed >= 8) {
                publishArtwork(initial, songs)
                changed = 0
            }
        }
        if (changed > 0 && revision == generation) publishArtwork(initial, songs)
    }

    private fun publishArtwork(initial: LocalScan, songs: List<LocalSong>) {
        val updated = initial.copy(songs = songs.toList())
        cachedScan = updated
        mutableUpdates.value = updated
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
                            filename.substringAfterLast('.', "").lowercase(Locale.ROOT) in IMAGE_EXTENSIONS ->
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

    private companion object {
        const val SCAN_FRESHNESS_MS = 60_000L
        val IMAGE_EXTENSIONS = listOf("jpg", "jpeg", "png", "webp", "gif")
    }
}

/** Match Sung's track-specific cover first, followed by common album-cover names. */
internal fun localCover(filename: String, images: Map<String, Uri>): Uri? {
    val stem = filename.substringBeforeLast('.', filename).lowercase(Locale.ROOT)
    return (listOf(stem, "cover", "folder", "front", "artwork")).firstNotNullOfOrNull { base ->
        listOf("jpg", "jpeg", "png", "webp", "gif").firstNotNullOfOrNull { extension ->
            images["$base.$extension"]
        }
    }
}

internal fun filenameTitle(name: String): String = name.substringBeforeLast('.', name).ifBlank { "Untitled" }

/** API 26–28: document IDs from the primary storage provider can match DATA metadata for deduplication. */
private fun legacyTreeIdentity(documentId: String): String? {
    if (Build.VERSION.SDK_INT >= 29 || !documentId.startsWith("primary:")) return null
    val relative = documentId.substringAfter(':').trimStart('/')
    if (relative.contains("..")) return null
    return "path:${Environment.getExternalStorageDirectory().resolve(relative).absolutePath}"
}
