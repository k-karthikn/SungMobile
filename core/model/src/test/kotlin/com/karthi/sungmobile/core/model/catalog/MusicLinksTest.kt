package com.karthi.sungmobile.core.model.catalog

import com.karthi.sungmobile.core.model.*
import org.junit.Assert.*
import org.junit.Test

class MusicLinksTest {
    private val song = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
    @Test fun sharedTextPreservesSongPlaylistAndTimestampChoices() {
        val choices = MusicLinks.extract("Listen: $song&list=PL1234567890&t=1m42s")
        assertEquals(listOf(song, "https://www.youtube.com/playlist?list=PL1234567890"), choices.map { it.url })
        assertEquals(listOf(102_000L, 0L), choices.map { it.startMs })
    }
    @Test fun plainLinksAndMultipleLinksNormalizeAndDeduplicate() {
        val choices = MusicLinks.extract("Great song (youtu.be/dQw4w9WgXcQ). Also $song and youtube.com/watch?v=RB-RcX5DS5A")
        assertEquals(2, choices.size)
        assertEquals(song, choices.first().url)
    }
    @Test fun deceptiveHostsSchemesAndUnsupportedPathsAreRejected() {
        listOf("evilyoutube.com/watch?v=dQw4w9WgXcQ", "ftp://youtube.com/watch?v=dQw4w9WgXcQ",
            "https://youtube.com.evil.test/watch?v=dQw4w9WgXcQ", "https://user@youtube.com/watch?v=dQw4w9WgXcQ",
            "https://youtube.com/channel/", "https://youtube.com/@artist/redirect", "https://youtube.com/redirect?q=$song",
            "https://youtu.be/short", "http://youtu.be/dQw4w9WgXcQ").forEach {
            assertThrows(it, SourceException::class.java) { MusicLinks.extract(it) }
        }
    }
    @Test fun timestampsAreBoundedAndMalformedValuesIgnored() {
        assertEquals(86_400_000L, MusicLinks.parse("$song&t=999999999").single().startMs)
        assertEquals(0L, MusicLinks.parse("$song&t=oops").single().startMs)
        assertEquals(0L, MusicLinks.parse("$song&t=-3").single().startMs)
        assertEquals(5_000L, MusicLinks.parse("$song#t=5").single().startMs)
    }
    @Test fun excessiveInputAndTooManyLinksAreRejected() {
        assertThrows(SourceException::class.java) { MusicLinks.extract("x".repeat(16_385)) }
        assertThrows(SourceException::class.java) { MusicLinks.extract(List(21) { song }.joinToString(" ")) }
    }
    @Test fun browseIdentityIsPreservedForCapabilityHandling() {
        val url = "https://music.youtube.com/browse/MPREb_abcdefghijk"
        assertEquals(url, MusicLinks.extract(url).single().url)
        assertTrue(YouTubeLink.parse(url) is YouTubeLink.Browse)
    }
}
