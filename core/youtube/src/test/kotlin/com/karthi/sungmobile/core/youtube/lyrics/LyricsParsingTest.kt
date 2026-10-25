package com.karthi.sungmobile.core.youtube.lyrics

import com.karthi.sungmobile.core.model.lyrics.parseLrc
import org.junit.Assert.*
import org.junit.Test

class LyricsParsingTest {
    @Test fun parsesYouTubeTabAndPlainLyricsWithoutInventedTiming() {
        val next = """{"contents":{"singleColumnMusicWatchNextResultsRenderer":{"tabbedRenderer":{"watchNextTabbedResultsRenderer":{"tabs":[{"tabRenderer":{"title":"Lyrics","endpoint":{"browseEndpoint":{"browseId":"MPLYt_123","browseEndpointContextSupportedConfigs":{"browseEndpointContextMusicConfig":{"pageType":"MUSIC_PAGE_TYPE_TRACK_LYRICS"}}}}}}]}}}}}"""
        assertEquals("MPLYt_123", parseYouTubeNext(next))
        val body = """{"contents":{"sectionListRenderer":{"contents":[{"musicDescriptionShelfRenderer":{"description":{"runs":[{"text":"line one\\nline two"}]},"footer":{"runs":[{"text":"Source: Artist"}]}}}]}}}"""
        val document = parseYouTubeLyrics(body)!!
        assertFalse(document.synchronized)
        assertEquals("Source: Artist", document.attribution)
    }

    @Test fun parsesTimedLinesAndRejectsMismatchedFallback() {
        val body = """{
          "contents": {"elementRenderer": {"newElement": {"type": {"componentType": {"model": {
            "timedLyricsModel": {"lyricsData": {
              "timedLyricsData": [{"lyricLine":"hello","cueRange":{"startTimeMilliseconds":"1000"}}],
              "sourceMessage":"Source: YouTube"
            }}
          }}}}}}
        }"""
        assertEquals(1_000, parseYouTubeLyrics(body)!!.lines.single().startMs)
        val fallback = """{"trackName":"Song","artistName":"Artist","duration":180,"syncedLyrics":"[00:01.00]first complete lyric line here\n[00:03.00]second complete lyric line here\n[00:05.00]third complete lyric line here"}"""
        assertNotNull(parseLrclib(fallback, "Song", "Artist", 180_000))
        assertNull(parseLrclib(fallback, "Song (Live)", "Artist", 180_000))
        assertNull(parseLrclib(fallback, "Song", "Artist", 190_000))
        assertNull(parseLrclib("""{"trackName":"Song","artistName":"Artist","duration":180,"syncedLyrics":"[00:01.00]probe"}""", "Song", "Artist", 180_000))
    }

    @Test fun lrcDoesNotTreatUntimedTextAsSynced() {
        assertTrue(parseLrc("plain words").isEmpty())
        assertEquals(2, parseLrc("[00:01.0][00:02.00]hello").size)
    }
    @Test fun mobileLyricsWithoutCompleteCueRangesRemainPlain() {
        val body = """{"contents":{"elementRenderer":{"newElement":{"type":{"componentType":{"model":{"timedLyricsModel":{"lyricsData":{"timedLyricsData":[{"lyricLine":"first line","cueRange":{"startTimeMilliseconds":"1000"}},{"lyricLine":"second line"}],"sourceMessage":"Source: LyricFind"}}}}}}}}}"""
        val lyrics = parseYouTubeLyrics(body)!!
        assertFalse(lyrics.synchronized)
        assertEquals("first line\nsecond line", lyrics.plainText)
        assertEquals("Source: LyricFind", lyrics.attribution)
    }
}
