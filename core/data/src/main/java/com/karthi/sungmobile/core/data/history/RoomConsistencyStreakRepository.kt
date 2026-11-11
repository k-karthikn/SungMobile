package com.karthi.sungmobile.core.data.history

import com.karthi.sungmobile.core.data.db.SungDatabase
import com.karthi.sungmobile.core.data.db.changes
import com.karthi.sungmobile.core.data.db.transaction
import com.karthi.sungmobile.core.model.library.ConsistencyStreakRepository
import com.karthi.sungmobile.core.model.library.StreakProgress
import com.karthi.sungmobile.core.model.library.calculateStreakProgress
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow

/** Reads the existing qualified-play history without creating a duplicate persistence format. */
class RoomConsistencyStreakRepository(
    private val db: SungDatabase,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
    private val today: () -> LocalDate = { LocalDate.now(zone()) },
) : ConsistencyStreakRepository {
    override val changes: Flow<Unit> = db.changes("listening_events", "history_policy")

    override suspend fun progress(): StreakProgress = db.transaction {
        if (db.historyDao().policy()?.privateMode == true) return@transaction StreakProgress(privateMode = true)
        val activeDays = db.historyDao().qualifiedListeningDays().mapTo(mutableSetOf(), LocalDate::parse)
        calculateStreakProgress(activeDays, today())
    }
}
