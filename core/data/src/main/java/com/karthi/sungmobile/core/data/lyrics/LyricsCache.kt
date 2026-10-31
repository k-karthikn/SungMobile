package com.karthi.sungmobile.core.data.lyrics

import android.content.Context
import com.karthi.sungmobile.core.model.lyrics.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.*

/** A private, disposable text cache. Room library data and saved offsets are separate. */
class LyricsCache(context: Context) {
    private val directory = File(context.applicationContext.cacheDir, "lyrics")
    private val lock = Mutex()

    suspend fun get(videoId: String, origin: LyricsOrigin): LyricsDocument? = withContext(Dispatchers.IO) { lock.withLock {
        val file = file(videoId, origin) ?: return@withContext null
        if (!file.isFile || file.length() > ENTRY_LIMIT || System.currentTimeMillis() - file.lastModified() > MAX_AGE) return@withContext null
        runCatching {
            DataInputStream(BufferedInputStream(FileInputStream(file))).use { input ->
                val version = input.readInt()
                if (version !in 1..3 || origin == LyricsOrigin.BETTER_LYRICS && version < 3)
                    return@runCatching null
                val attribution = input.readUTF()
                val plain = readText(input)
                val count = input.readInt().coerceIn(0, 4_000)
                val lines = List(count) {
                    val start = input.readLong()
                    val text = readText(input).orEmpty()
                    val syllables = if (version >= 2) {
                        val syllableCount = input.readInt().also { require(it in 0..MAX_SYLLABLES_PER_LINE) }
                        List(syllableCount) {
                            LyricSyllable(input.readLong(), input.readLong(), readText(input).orEmpty(),
                                input.readBoolean(), input.readBoolean())
                        }
                    } else emptyList()
                    LyricLine(start, text, syllables)
                }
                LyricsDocument(origin, attribution, plain, lines)
            }
        }.getOrNull()
    } }

    suspend fun put(videoId: String, document: LyricsDocument) = withContext(Dispatchers.IO) { lock.withLock {
        val target = file(videoId, document.origin) ?: return@withContext
        if (document.plainText.orEmpty().length > 256_000 || document.lines.size > 4_000) return@withContext
        directory.mkdirs()
        val temporary = File(directory, "${target.name}.tmp")
        runCatching {
            DataOutputStream(BufferedOutputStream(FileOutputStream(temporary))).use { output ->
                output.writeInt(3)
                output.writeUTF(document.attribution.take(1_000))
                writeText(output, document.plainText)
                output.writeInt(document.lines.size)
                document.lines.forEach { line ->
                    output.writeLong(line.startMs)
                    writeText(output, line.text)
                    val syllables = line.syllables.take(MAX_SYLLABLES_PER_LINE)
                    output.writeInt(syllables.size)
                    syllables.forEach { syllable ->
                        output.writeLong(syllable.startMs)
                        output.writeLong(syllable.durationMs)
                        writeText(output, syllable.text)
                        output.writeBoolean(syllable.isBackground)
                        output.writeBoolean(syllable.appendToPrevious)
                    }
                }
            }
            if (temporary.length() <= ENTRY_LIMIT && temporary.renameTo(target)) trim() else temporary.delete()
        }.onFailure { temporary.delete() }
    } }

    suspend fun bytes(): Long = withContext(Dispatchers.IO) { lock.withLock { directory.listFiles()?.sumOf { it.length() } ?: 0 } }
    suspend fun clear() = withContext(Dispatchers.IO) { lock.withLock { directory.listFiles()?.forEach { it.delete() }; Unit } }

    private fun trim() {
        val files = directory.listFiles()?.sortedByDescending { it.lastModified() } ?: return
        var used = 0L
        files.forEach { file -> used += file.length(); if (used > TOTAL_LIMIT) file.delete() }
    }

    private fun file(videoId: String, origin: LyricsOrigin): File? =
        if (VIDEO_ID.matches(videoId)) File(directory, "$videoId-${origin.name}.bin") else null

    private fun readText(input: DataInputStream): String? {
        val length = input.readInt()
        if (length < 0) return null
        require(length <= 256_000)
        return ByteArray(length).also { input.readFully(it) }.toString(Charsets.UTF_8)
    }
    private fun writeText(output: DataOutputStream, value: String?) {
        val bytes = value?.toByteArray(Charsets.UTF_8)
        output.writeInt(bytes?.size ?: -1)
        if (bytes != null) output.write(bytes)
    }
    private companion object {
        val VIDEO_ID = Regex("[A-Za-z0-9_-]{11}")
        const val ENTRY_LIMIT = 300_000L
        const val TOTAL_LIMIT = 8L * 1024 * 1024
        const val MAX_AGE = 30L * 24 * 60 * 60 * 1_000
        const val MAX_SYLLABLES_PER_LINE = 200
    }
}
