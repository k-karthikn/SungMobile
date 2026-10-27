package com.karthi.sungmobile.core.data

import androidx.room.Room
import com.karthi.sungmobile.core.data.db.*
import com.karthi.sungmobile.core.data.recommendation.RoomRecommendationProfileSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RoomRecommendationProfileSourceTest {
    @Test fun disabledHistoryKeepsLikesButPrivateModeHidesTheProfile() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), SungDatabase::class.java)
            .allowMainThreadQueries().build()
        try {
            val key = "youtube:RB-RcX5DS5A"
            db.libraryDao().saveTrack(TrackEntity().apply {
                trackKey = key; title = "Yellow"; artist = "Coldplay"
                canonicalUrl = "https://www.youtube.com/watch?v=RB-RcX5DS5A"
            })
            db.libraryDao().like(LikeEntity().apply { trackKey = key; likedAt = 1 })
            db.historyDao().saveEvent(ListeningEntity().apply {
                id = "event"; passId = "pass"; trackKey = key
                listenedMs = 60_000; startedAt = 1; lastPlayedAt = 60_001; qualified = true
            })
            val source = RoomRecommendationProfileSource(db)
            assertEquals(1, source.observe().first().topSongs.size)
            db.historyDao().setPolicy(HistoryPolicyEntity().apply { listening = false })
            val disabled = source.observe().first()
            assertEquals(1, disabled.liked.size)
            assertTrue(disabled.topSongs.isEmpty())
            assertTrue(disabled.recentEvents.isEmpty())
            db.historyDao().setPolicy(HistoryPolicyEntity().apply { privateMode = true })
            val private = source.observe().first()
            assertTrue(private.policy.privateMode)
            assertTrue(private.liked.isEmpty())
            assertTrue(private.saved.isEmpty())
        } finally { db.close() }
    }
}
