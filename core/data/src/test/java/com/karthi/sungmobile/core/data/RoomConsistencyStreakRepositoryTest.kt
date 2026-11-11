package com.karthi.sungmobile.core.data

import androidx.room.Room
import com.karthi.sungmobile.core.data.db.SungDatabase
import com.karthi.sungmobile.core.data.history.RoomConsistencyStreakRepository
import com.karthi.sungmobile.core.data.history.RoomHistoryRepository
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.CatalogKind
import com.karthi.sungmobile.core.model.library.HistorySetting
import com.karthi.sungmobile.core.model.library.ListeningRecord
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RoomConsistencyStreakRepositoryTest {
    private lateinit var db: SungDatabase
    private lateinit var history: RoomHistoryRepository
    private val zone = ZoneId.systemDefault()
    private val today = LocalDate.of(2026, 11, 11)
    private val song = CatalogItem("RB-RcX5DS5A", CatalogKind.SONG, "Yellow", "Coldplay",
        "https://www.youtube.com/watch?v=RB-RcX5DS5A")

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), SungDatabase::class.java).build()
        history = RoomHistoryRepository(db)
    }

    @After fun cleanup() { db.close() }

    @Test fun derivesOneActiveDayFromAnyNumberOfQualifiedEvents() = runBlocking {
        record(today.minusDays(1), "first")
        record(today, "second")
        record(today, "third")
        val streaks = RoomConsistencyStreakRepository(db, { zone }, { today })
        val progress = streaks.progress()
        assertEquals(2, progress.currentDays)
        assertEquals(2, progress.longestDays)
        assertEquals(2, progress.activeDays.size)
    }

    @Test fun privateModeHidesStreakAndDoesNotExposeHistory() = runBlocking {
        record(today, "first")
        history.setPolicy(HistorySetting.PRIVATE_MODE, true)
        val progress = RoomConsistencyStreakRepository(db, { zone }, { today }).progress()
        assertTrue(progress.privateMode)
        assertTrue(progress.activeDays.isEmpty())
        assertEquals(0, progress.currentDays)
    }

    private suspend fun record(day: LocalDate, id: String) {
        val time = day.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        history.recordListening(ListeningRecord(id, id, song, 0, 60_000, time - 60_000, time, true))
    }
}
