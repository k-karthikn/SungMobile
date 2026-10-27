package com.karthi.sungmobile.feature.lyrics

import com.karthi.sungmobile.core.model.lyrics.LyricLine
import com.karthi.sungmobile.core.model.lyrics.LyricsDocument
import com.karthi.sungmobile.core.model.lyrics.LyricsOrigin
import org.junit.Assert.assertEquals
import org.junit.Test

class LyricsPreviewTextTest {
    @Test fun timedPreviewFollowsPlaybackAndSavedOffset() {
        val document = LyricsDocument(LyricsOrigin.YOUTUBE, "Test", lines = listOf(
            LyricLine(0, "First"), LyricLine(1_000, "Second"),
            LyricLine(2_000, "Third"), LyricLine(3_000, "Fourth")))
        assertEquals(listOf("Second", "Third", "Fourth"), lyricsPreviewText(document, 800, 250))
        assertEquals(listOf("Fourth"), lyricsPreviewText(document, 4_000, 0))
    }

    @Test fun plainPreviewSkipsEmptyLinesAndEmptyDocumentShowsNothing() {
        val document = LyricsDocument(LyricsOrigin.LOCAL, "Test",
            plainText = "\n  First line  \n\n Second line\nThird line\nFourth line")
        assertEquals(listOf("First line", "Second line", "Third line"), lyricsPreviewText(document, 0, 0))
        assertEquals(emptyList<String>(), lyricsPreviewText(document.copy(plainText = "  \n  "), 0, 0))
    }
}
