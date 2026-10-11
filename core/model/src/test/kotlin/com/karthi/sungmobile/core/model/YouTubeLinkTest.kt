package com.karthi.sungmobile.core.model

import org.junit.Assert.*
import org.junit.Test

class YouTubeLinkTest {
    @Test fun canonicalizesSongsWithoutTrackingOrQueueParameters() {
        val id = "dQw4w9WgXcQ"
        listOf(
            "https://youtu.be/$id?si=tracking",
            "https://music.youtube.com/watch?v=$id&list=PL1234567890",
            "https://www.youtube.com/shorts/$id",
        ).forEach { assertEquals(YouTubeLink.Song(id), YouTubeLink.parse(it)) }
    }

    @Test fun rejectsForeignHostsCredentialsAndMalformedIds() {
        listOf(
            "https://youtube.com.evil.example/watch?v=dQw4w9WgXcQ",
            "https://youtube.com@evil.example/watch?v=dQw4w9WgXcQ",
            "https://user@youtube.com/watch?v=dQw4w9WgXcQ",
            "file:///tmp/music.mp3", "https://youtu.be/bad", "https://youtu.be/dQw4w9WgXcQ/extra",
            "http://youtube.com/watch?v=dQw4w9WgXcQ",
        ).forEach { link ->
            val failure = assertThrows(SourceException::class.java) { YouTubeLink.parse(link) }
            assertEquals(SourceFailure.INVALID_LINK, failure.failure)
        }
    }

    @Test fun preservesPlaylistIdentity() {
        assertEquals(YouTubeLink.Playlist("PL1234567890"),
            YouTubeLink.parse("https://music.youtube.com/playlist?list=PL1234567890"))
    }

    @Test fun streamExpiryIncludesSafetyMarginAndLogsAreRedacted() {
        val track = CatalogItem("id", CatalogKind.SONG, "Song", "Artist", "https://youtu.be/dQw4w9WgXcQ")
        val stream = ResolvedStream(track, "https://secret.example?token=secret", null, emptyMap(), 128, 200_000)
        assertTrue(stream.isFresh(79_999))
        assertFalse(stream.isFresh(80_000))
        assertFalse(stream.toString().contains("secret"))
    }
}
