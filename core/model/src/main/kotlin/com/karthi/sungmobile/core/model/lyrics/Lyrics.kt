package com.karthi.sungmobile.core.model.lyrics

import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.YouTubeLink
import java.security.MessageDigest
import java.util.Base64

enum class LyricsOrigin { YOUTUBE, LRCLIB, LOCAL }

data class LyricLine(val startMs: Long, val text: String)

data class LyricsDocument(
    val origin: LyricsOrigin,
    val attribution: String,
    val plainText: String? = null,
    val lines: List<LyricLine> = emptyList(),
) {
    val synchronized get() = lines.isNotEmpty()
    init { require(plainText != null || lines.isNotEmpty()) }
}

interface LyricsGateway {
    suspend fun youtube(track: CatalogItem): LyricsDocument?
    suspend fun lrclib(track: CatalogItem): LyricsDocument?
}

/** Keep YouTube's text unless LRCLIB supplies timing that YouTube lacks. */
fun selectLyrics(youtube: LyricsDocument?, lrclib: LyricsDocument?): LyricsDocument? = when {
    youtube?.synchronized == true -> youtube
    lrclib?.synchronized == true -> lrclib
    else -> youtube ?: lrclib
}

fun CatalogItem.lyricsVideoId(): String? =
    (runCatching { YouTubeLink.parse(url) }.getOrNull() as? YouTubeLink.Song)?.videoId

/** Local content URIs need a stable, filename-safe key for lyric text and timing offsets. */
fun CatalogItem.lyricsStorageKey(): String? = lyricsVideoId() ?: id.takeIf { it.startsWith("local:content://") }
    ?.let { "l" + Base64.getUrlEncoder().withoutPadding().encodeToString(
        MessageDigest.getInstance("SHA-256").digest(it.toByteArray(Charsets.UTF_8))).take(10) }

fun CatalogItem.isLocalSong(): Boolean = id.startsWith("local:content://")

/** Shared by LRCLIB and local sidecar/embedded lyric files. */
fun parseLrc(text: String): List<LyricLine> {
    if (text.length > 256_000) return emptyList()
    val stamp = Regex("\\[(\\d{1,3}):(\\d{2})(?:\\.(\\d{1,3}))?]")
    return text.lineSequence().flatMap { raw ->
        val lyric = raw.replace(stamp, "").trim()
        if (lyric.isBlank()) emptySequence() else stamp.findAll(raw).mapNotNull { match ->
            val minutes = match.groupValues[1].toLongOrNull() ?: return@mapNotNull null
            val seconds = match.groupValues[2].toLongOrNull() ?: return@mapNotNull null
            if (seconds > 59) return@mapNotNull null
            val fraction = match.groupValues[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0
            LyricLine((minutes * 60 + seconds) * 1_000 + fraction, lyric)
        }
    }.take(4_000).sortedBy { it.startMs }.toList()
}

/** The display clock includes the saved offset; tapping a line reverses it. */
fun currentLyricLine(lines: List<LyricLine>, positionMs: Long, offsetMs: Int): Int {
    val time = (positionMs + offsetMs).coerceAtLeast(0)
    var low = 0
    var high = lines.size
    while (low < high) {
        val mid = (low + high) ushr 1
        if (lines[mid].startMs <= time) low = mid + 1 else high = mid
    }
    return low - 1
}

fun seekForLine(line: LyricLine, offsetMs: Int, durationMs: Long): Long =
    (line.startMs - offsetMs).coerceAtLeast(0).let { if (durationMs > 0) it.coerceAtMost(durationMs) else it }
