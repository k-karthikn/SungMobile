package com.karthi.sungmobile.core.model

import com.karthi.sungmobile.core.model.lyrics.*
import org.junit.Assert.assertEquals
import org.junit.Test

class LyricsTest {
    @Test fun usesCanonicalVideoIdFromRealCatalogUrl() {
        val track = CatalogItem("https://www.youtube.com/watch?v=dQw4w9WgXcQ", CatalogKind.SONG,
            "Song", "Artist", "https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        assertEquals("dQw4w9WgXcQ", track.lyricsVideoId())
    }
    @Test fun timingOffsetAffectsHighlightAndSeekInOppositeDirections() {
        val lines = listOf(LyricLine(1_000, "one"), LyricLine(2_000, "two"))
        assertEquals(1, currentLyricLine(lines, 1_750, 250))
        assertEquals(1_750, seekForLine(lines[1], 250, 5_000))
        assertEquals(-1, currentLyricLine(lines, 0, 0))
        assertEquals(500, seekForLine(lines[1], 1_500, 500))
    }
    @Test fun timedFallbackUpgradesPlainYouTubeButNeverReplacesTimedYouTube() {
        val youtubePlain = LyricsDocument(LyricsOrigin.YOUTUBE, "Source: LyricFind", plainText = "plain lyrics")
        val youtubeTimed = LyricsDocument(LyricsOrigin.YOUTUBE, "Source: Musixmatch", lines = listOf(LyricLine(1000, "timed")))
        val lrclibPlain = LyricsDocument(LyricsOrigin.LRCLIB, "LRCLIB", plainText = "other plain lyrics")
        val lrclibTimed = LyricsDocument(LyricsOrigin.LRCLIB, "LRCLIB", lines = listOf(LyricLine(1200, "timed")))
        assertEquals(lrclibTimed, selectLyrics(youtubePlain, lrclibTimed))
        assertEquals(youtubePlain, selectLyrics(youtubePlain, lrclibPlain))
        assertEquals(youtubeTimed, selectLyrics(youtubeTimed, lrclibTimed))
        assertEquals(lrclibPlain, selectLyrics(null, lrclibPlain))
    }
}
