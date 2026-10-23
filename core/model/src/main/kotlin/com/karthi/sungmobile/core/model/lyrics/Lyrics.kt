package com.karthi.sungmobile.core.model.lyrics

import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.YouTubeLink

enum class LyricsOrigin { YOUTUBE, LRCLIB }

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
