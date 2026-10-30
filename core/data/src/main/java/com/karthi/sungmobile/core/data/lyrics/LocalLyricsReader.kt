package com.karthi.sungmobile.core.data.lyrics

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.lyrics.LyricsDocument
import com.karthi.sungmobile.core.model.lyrics.isLocalSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

fun interface LocalLyricsSource {
    suspend fun read(track: CatalogItem): LyricsDocument?
}

/** Reads only the playing file and its matching sidecar; no library-wide file walk. */
class LocalLyricsReader(private val context: Context, private val grantedTrees: () -> List<Uri>) : LocalLyricsSource {
    private val resolver get() = context.contentResolver

    override suspend fun read(track: CatalogItem): LyricsDocument? = withContext(Dispatchers.IO) {
        if (!track.isLocalSong()) return@withContext null
        val uri = Uri.parse(track.id.removePrefix("local:"))
        val name = displayName(uri) ?: return@withContext embedded(uri)
        val sidecar = runCatching { sidecar(uri, name) }.getOrNull()
        if (sidecar?.synchronized == true) return@withContext sidecar
        val embedded = embedded(uri)
        when {
            embedded?.synchronized == true -> embedded
            else -> sidecar ?: embedded
        }
    }

    private fun displayName(uri: Uri): String? = runCatching {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0)?.takeIf(String::isNotBlank) else null
        }
    }.getOrNull()

    private fun sidecar(uri: Uri, name: String): LyricsDocument? {
        val stem = name.substringBeforeLast('.', name)
        if (stem.isBlank()) return null
        if (DocumentsContract.isDocumentUri(context, uri)) {
            findDocumentSidecar(uri, stem)?.let { return it }
        }
        return findMediaSidecar(uri, stem)
    }

    private fun findDocumentSidecar(uri: Uri, stem: String): LyricsDocument? = runCatching {
        val path = DocumentsContract.findDocumentPath(resolver, uri)?.path ?: return@runCatching null
        val parentId = path.getOrNull(path.lastIndex - 1) ?: return@runCatching null
        findSidecarInFolder(uri, parentId, stem)
    }.getOrNull()

    private fun findSidecarInFolder(tree: Uri, parentId: String, stem: String): LyricsDocument? {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentId)
        val matches = mutableMapOf<String, Uri>()
        resolver.query(children, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { cursor ->
            var seen = 0
            while (cursor.moveToNext() && seen++ < 5_000) {
                val filename = cursor.getString(1) ?: continue
                val suffix = sidecarSuffix(stem, filename) ?: continue
                val id = cursor.getString(0) ?: continue
                matches[suffix] = DocumentsContract.buildDocumentUriUsingTree(tree, id)
            }
        }
        return listOf("lrc", "txt").firstNotNullOfOrNull { suffix ->
            matches[suffix]?.let { readSidecar(it, "$stem.$suffix") }
        }
    }

    private fun findMediaSidecar(uri: Uri, stem: String): LyricsDocument? {
        if (Build.VERSION.SDK_INT < 29) return legacyMediaSidecar(uri, stem)
        val relative = runCatching {
            resolver.query(uri, arrayOf(MediaStore.MediaColumns.RELATIVE_PATH), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()?.trimStart('/') ?: return null
        if (relative.contains("..") || !relative.endsWith('/')) return null
        // READ_MEDIA_AUDIO does not grant access to text files. Try only SAF trees the user picked.
        return grantedTrees().firstNotNullOfOrNull { tree ->
            runCatching {
                if (tree.authority != "com.android.externalstorage.documents") return@runCatching null
                val rootId = DocumentsContract.getTreeDocumentId(tree)
                val volume = rootId.substringBefore(':', "")
                val rootPath = rootId.substringAfter(':', "").trim('/')
                if (volume != "primary" || (rootPath.isNotEmpty() &&
                    !relative.startsWith("$rootPath/", ignoreCase = true))) return@runCatching null
                findSidecarInFolder(tree, "$volume:${relative.trimEnd('/')}", stem)
            }.getOrNull()
        }
    }

    private fun legacyMediaSidecar(uri: Uri, stem: String): LyricsDocument? = runCatching {
        val path = resolver.query(uri, arrayOf(MediaStore.Audio.Media.DATA), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } ?: return@runCatching null
        val parent = File(path).parentFile ?: return@runCatching null
        val music = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).canonicalFile
        if (!parent.canonicalFile.toPath().startsWith(music.toPath())) return@runCatching null
        listOf("lrc", "txt").firstNotNullOfOrNull { suffix ->
            val file = File(parent, "$stem.$suffix")
            if (file.isFile && file.length() <= MAX_LOCAL_LYRICS_BYTES) runCatching {
                file.inputStream().use { parseLocalLyricsDocument(it, file.name) }
            }.getOrNull() else null
        }
    }.getOrNull()

    private fun readSidecar(uri: Uri, name: String): LyricsDocument? = runCatching {
        resolver.openInputStream(uri)?.use { parseLocalLyricsDocument(it, name) }
    }.getOrNull()

    private fun embedded(uri: Uri): LyricsDocument? {
        val text = runCatching { resolver.openInputStream(uri)?.use(::readEmbeddedTag) }.getOrNull()
            ?: runCatching { resolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                FileInputStream(descriptor.fileDescriptor).channel.use(::readMp4Lyrics)
            } }.getOrNull()
        return text?.let { localDocument(it, "Embedded lyrics") }
    }
}
