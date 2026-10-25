package com.karthi.sungmobile.feature.lyrics

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.lyrics.LyricsDocument
import com.karthi.sungmobile.core.model.lyrics.LyricsOrigin
import com.karthi.sungmobile.core.model.lyrics.isLocalSong
import com.karthi.sungmobile.core.model.lyrics.parseLrc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.Charset

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
            if (file.isFile && file.length() <= MAX_TEXT_BYTES) runCatching {
                file.inputStream().use { localDocument(it, file.name) }
            }.getOrNull() else null
        }
    }.getOrNull()

    private fun readSidecar(uri: Uri, name: String): LyricsDocument? = runCatching {
        resolver.openInputStream(uri)?.use { localDocument(it, name) }
    }.getOrNull()

    private fun embedded(uri: Uri): LyricsDocument? {
        val text = runCatching { resolver.openInputStream(uri)?.use(::readEmbeddedTag) }.getOrNull()
            ?: runCatching { resolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                FileInputStream(descriptor.fileDescriptor).channel.use(::readMp4Lyrics)
            } }.getOrNull()
        return text?.let { localDocument(it, "Embedded lyrics") }
    }

    private fun localDocument(stream: InputStream, name: String): LyricsDocument? {
        val bytes = stream.readBounded(MAX_TEXT_BYTES) ?: return null
        val text = when {
            bytes.size >= 2 && bytes[0] == 0xff.toByte() && bytes[1] == 0xfe.toByte() ->
                bytes.copyOfRange(2, bytes.size).toString(Charsets.UTF_16LE)
            bytes.size >= 2 && bytes[0] == 0xfe.toByte() && bytes[1] == 0xff.toByte() ->
                bytes.copyOfRange(2, bytes.size).toString(Charsets.UTF_16BE)
            else -> bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF")
        }
        return localDocument(text, name)
    }
}

internal fun sidecarSuffix(stem: String, filename: String): String? =
    listOf("lrc", "txt").firstOrNull { filename.equals("$stem.$it", ignoreCase = true) }

internal fun localDocument(raw: String, source: String): LyricsDocument? {
    val text = raw.trim().takeIf { it.isNotEmpty() && it.length <= MAX_TEXT_BYTES } ?: return null
    val lines = parseLrc(text)
    if (lines.isNotEmpty()) return LyricsDocument(LyricsOrigin.LOCAL, source, lines = lines)
    val plain = text.lineSequence().filterNot { it.matches(Regex("^\\[[a-zA-Z]{1,12}:.*]$")) }
        .joinToString("\n").trim().takeIf(String::isNotBlank) ?: return null
    return LyricsDocument(LyricsOrigin.LOCAL, source, plainText = plain)
}

private const val MAX_TEXT_BYTES = 256_000
private const val MAX_TAG_BYTES = 1_048_576

private fun InputStream.readBounded(limit: Int): ByteArray? {
    val bytes = ByteArray(limit + 1)
    var count = 0
    while (count < bytes.size) {
        val n = read(bytes, count, bytes.size - count)
        if (n < 0) break
        count += n
    }
    return if (count <= limit) bytes.copyOf(count) else null
}

private fun InputStream.readExact(size: Int): ByteArray? {
    val bytes = ByteArray(size)
    var count = 0
    while (count < size) {
        val n = read(bytes, count, size - count)
        if (n < 0) return null
        count += n
    }
    return bytes
}

/** ID3 USLT (MP3) and Vorbis comments (FLAC); both are near the start of the file. */
internal fun readEmbeddedTag(stream: InputStream): String? {
    val prefix = stream.readExact(4) ?: return null
    if (prefix.copyOfRange(0, 3).toString(Charsets.US_ASCII) == "ID3") {
        val tail = stream.readExact(6) ?: return null
        val header = prefix + tail
        val version = header[3].toInt() and 0xff
        if (version !in 2..4) return null
        val size = synchsafe(header, 6)
        if (size !in 1..MAX_TAG_BYTES) return null
        return parseId3Lyrics(stream.readExact(size) ?: return null, version)
    }
    if (prefix.toString(Charsets.US_ASCII) == "fLaC") return parseFlacLyrics(stream)
    return null
}

private fun synchsafe(bytes: ByteArray, at: Int): Int =
    ((bytes[at].toInt() and 0x7f) shl 21) or ((bytes[at + 1].toInt() and 0x7f) shl 14) or
        ((bytes[at + 2].toInt() and 0x7f) shl 7) or (bytes[at + 3].toInt() and 0x7f)

private fun big32(bytes: ByteArray, at: Int): Int =
    ((bytes[at].toInt() and 0xff) shl 24) or ((bytes[at + 1].toInt() and 0xff) shl 16) or
        ((bytes[at + 2].toInt() and 0xff) shl 8) or (bytes[at + 3].toInt() and 0xff)

private fun little32(bytes: ByteArray, at: Int): Int =
    (bytes[at].toInt() and 0xff) or ((bytes[at + 1].toInt() and 0xff) shl 8) or
        ((bytes[at + 2].toInt() and 0xff) shl 16) or ((bytes[at + 3].toInt() and 0xff) shl 24)

internal fun parseId3Lyrics(tag: ByteArray, version: Int): String? {
    var at = 0
    val header = if (version == 2) 6 else 10
    while (at + header <= tag.size) {
        val idLength = if (version == 2) 3 else 4
        val id = tag.copyOfRange(at, at + idLength).toString(Charsets.US_ASCII)
        if (!id.all { it.isLetterOrDigit() }) break
        val size = when (version) {
            2 -> ((tag[at + 3].toInt() and 0xff) shl 16) or
                ((tag[at + 4].toInt() and 0xff) shl 8) or (tag[at + 5].toInt() and 0xff)
            4 -> synchsafe(tag, at + 4)
            else -> big32(tag, at + 4)
        }
        if (size < 0 || at + header + size > tag.size) break
        if (id == "USLT" || id == "ULT") {
            val text = decodeUslt(tag.copyOfRange(at + header, at + header + size))
            if (!text.isNullOrBlank()) return text
        }
        at += header + size
    }
    return null
}

private fun decodeUslt(frame: ByteArray): String? {
    if (frame.size < 5) return null
    val encoding = frame[0].toInt() and 0xff
    val wide = encoding == 1 || encoding == 2
    var end = 4
    while (end + (if (wide) 1 else 0) < frame.size) {
        if (frame[end] == 0.toByte() && (!wide || frame[end + 1] == 0.toByte())) break
        end += if (wide) 2 else 1
    }
    val start = end + if (wide) 2 else 1
    if (start >= frame.size || frame.size - start > MAX_TEXT_BYTES) return null
    val charset: Charset = when (encoding) {
        0 -> Charsets.ISO_8859_1
        1 -> Charsets.UTF_16
        2 -> Charsets.UTF_16BE
        3 -> Charsets.UTF_8
        else -> return null
    }
    return frame.copyOfRange(start, frame.size).toString(charset).trim('\u0000', '\uFEFF', ' ', '\n', '\r')
}

private fun parseFlacLyrics(stream: InputStream): String? {
    var read = 0
    while (read < MAX_TAG_BYTES) {
        val block = stream.readExact(4) ?: return null
        val last = block[0].toInt() and 0x80 != 0
        val type = block[0].toInt() and 0x7f
        val size = ((block[1].toInt() and 0xff) shl 16) or
            ((block[2].toInt() and 0xff) shl 8) or (block[3].toInt() and 0xff)
        if (size > MAX_TAG_BYTES - read) return null
        val payload = stream.readExact(size) ?: return null
        read += size + 4
        if (type == 4) return parseVorbisLyrics(payload)
        if (last) break
    }
    return null
}

internal fun parseVorbisLyrics(bytes: ByteArray): String? {
    if (bytes.size < 8) return null
    val vendor = little32(bytes, 0)
    if (vendor < 0 || vendor > bytes.size - 8) return null
    var at = 4 + vendor
    val count = little32(bytes, at)
    at += 4
    if (count !in 0..4_000) return null
    repeat(count) {
        if (at + 4 > bytes.size) return null
        val size = little32(bytes, at)
        at += 4
        if (size < 0 || size > bytes.size - at) return null
        val comment = bytes.copyOfRange(at, at + size).toString(Charsets.UTF_8)
        at += size
        if (comment.substringBefore('=').uppercase() in setOf("LYRICS", "UNSYNCEDLYRICS", "SYNCEDLYRICS"))
            return comment.substringAfter('=').takeIf { it.length <= MAX_TEXT_BYTES }
    }
    return null
}

/** Apple ©lyr atom, used by M4A. Channel seeking avoids reading the audio payload. */
internal fun readMp4Lyrics(channel: FileChannel): String? = runCatching {
    val size = channel.size()
    if (size < 16) return@runCatching null
    val visited = intArrayOf(0)
    scanAtoms(channel, 0, size, 0, visited)
}.getOrNull()

private fun scanAtoms(channel: FileChannel, start: Long, end: Long, depth: Int, visited: IntArray): String? {
    if (depth > 5) return null
    var at = start
    while (at + 8 <= end && visited[0]++ < 10_000) {
        val base = channel.readAt(at, 8) ?: return null
        val header = if (big32(base, 0) == 1) channel.readAt(at, 16) ?: return null else base
        val atomSize = big32(header, 0).toLong() and 0xffffffffL
        val extended = atomSize == 1L
        val headerSize = if (extended) 16L else 8L
        val length = when {
            atomSize == 0L -> end - at
            extended -> ByteBuffer.wrap(header, 8, 8).long
            else -> atomSize
        }
        if (length < headerSize || length > end - at) return null
        val type = header.copyOfRange(4, 8).toString(Charsets.ISO_8859_1)
        val body = at + headerSize
        val found = when (type) {
            "moov", "udta", "ilst" -> scanAtoms(channel, body, at + length, depth + 1, visited)
            "meta" -> scanAtoms(channel, body + 4, at + length, depth + 1, visited)
            "©lyr" -> readLyricAtom(channel, body, at + length)
            else -> null
        }
        if (found != null) return found
        at += length
    }
    return null
}

private fun readLyricAtom(channel: FileChannel, start: Long, end: Long): String? {
    val header = channel.readAt(start, 16) ?: return null
    if (header.copyOfRange(4, 8).toString(Charsets.ISO_8859_1) != "data") return null
    val size = big32(header, 0).toLong() and 0xffffffffL
    if (size !in 17..(MAX_TEXT_BYTES + 16).toLong() || start + size > end) return null
    return channel.readAt(start + 16, (size - 16).toInt())?.toString(Charsets.UTF_8)
        ?.trim('\u0000', '\uFEFF', ' ', '\n', '\r')
}

private fun FileChannel.readAt(position: Long, count: Int): ByteArray? {
    val buffer = ByteBuffer.allocate(count)
    var offset = position
    while (buffer.hasRemaining()) {
        val n = read(buffer, offset)
        if (n <= 0) return null
        offset += n
    }
    return buffer.array()
}
