package com.karthi.sungmobile.feature.lyrics

import com.karthi.sungmobile.core.model.lyrics.LyricsDocument
import com.karthi.sungmobile.core.model.lyrics.currentLyricLine

/** A short excerpt for Now Playing; the full document remains owned by the lyrics page. */
internal fun lyricsPreviewText(document: LyricsDocument, positionMs: Long, offsetMs: Int): List<String> {
    if (document.lines.isNotEmpty()) {
        val current = currentLyricLine(document.lines, positionMs, offsetMs).coerceAtLeast(0)
        return document.lines.asSequence().drop(current).map { it.text.trim() }
            .filter(String::isNotEmpty).take(3).toList()
    }
    return document.plainText.orEmpty().lineSequence().map(String::trim)
        .filter(String::isNotEmpty).take(3).toList()
}
