package com.karthi.sungmobile.core.playback.history

import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.CatalogKind
import org.junit.Assert.*
import org.junit.Test

class ListeningAccumulatorTest {
    private val song = CatalogItem("RB-RcX5DS5A", CatalogKind.SONG, "Song", "Artist",
        "https://www.youtube.com/watch?v=RB-RcX5DS5A", durationMs = 120_000)

    @Test fun countsOnlyElapsedPlayingTimeAndIgnoresPositionJumps() {
        val history = ListeningAccumulator("pass", song, 4, 100, 0)
        history.sample(playing = true, durationMs = 120_000, monotonicMs = 0, wallMs = 100, track = song)
        assertEquals(15_000L, history.sample(true, 120_000, 15_000, 15_100, song).listenedMs)
        assertEquals(15_000L, history.sample(false, 120_000, 15_000, 15_100, song).listenedMs)
        assertEquals(15_000L, history.sample(false, 120_000, 60_000, 60_100, song).listenedMs)
        assertEquals(15_000L, history.sample(true, 120_000, 60_000, 60_100, song).listenedMs)
        val qualified = history.sample(true, 120_000, 75_000, 75_100, song)
        assertEquals(30_000L, qualified.listenedMs)
        assertTrue(qualified.qualified)
        assertEquals("pass:4", qualified.id)
    }

    @Test fun shortTrackUsesHalfDurationAndRestoresCheckpoint() {
        val original = ListeningAccumulator("pass", song, 2, 500, 0)
        original.sample(true, 8_000, 0, 500, song)
        val saved = original.sample(true, 8_000, 4_000, 4_500, song)
        assertTrue(saved.qualified)
        val restored = ListeningAccumulator("pass", song, 2, 9_000, 20_000, saved)
        val paused = restored.sample(false, 8_000, 30_000, 20_000, song)
        assertEquals(4_000L, paused.listenedMs)
        restored.sample(true, 8_000, 30_000, 20_000, song)
        val next = restored.sample(true, 8_000, 40_000, 30_000, song)
        assertEquals(14_000L, next.listenedMs)
        assertTrue(next.qualified)
        assertEquals(500L, next.startedAt)
    }
}
