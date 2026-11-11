package com.karthi.sungmobile.core.model.library

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/** Derived from qualified listening history; it never persists a second copy of user activity. */
data class StreakProgress(
    val privateMode: Boolean = false,
    val activeDays: Set<LocalDate> = emptySet(),
    val currentDays: Int = 0,
    val longestDays: Int = 0,
)

interface ConsistencyStreakRepository {
    val changes: Flow<Unit>
    suspend fun progress(): StreakProgress
}

/**
 * Calculates streaks in local calendar days. A user can still continue yesterday's streak today;
 * a gap before yesterday resets the current run to zero.
 */
fun calculateStreakProgress(activeDays: Set<LocalDate>, today: LocalDate): StreakProgress {
    val days = activeDays.filterTo(sortedSetOf()) { !it.isAfter(today) }
    val endpoint = when {
        today in days -> today
        today.minusDays(1) in days -> today.minusDays(1)
        else -> null
    }
    val current = endpoint?.let { streakLengthOn(it, days) } ?: 0
    var longest = 0
    var run = 0
    var previous: LocalDate? = null
    days.forEach { day ->
        run = if (previous?.plusDays(1) == day) run + 1 else 1
        longest = maxOf(longest, run)
        previous = day
    }
    return StreakProgress(activeDays = days, currentDays = current, longestDays = longest)
}

/** The consecutive run that ends on [day], used to color historical calendar cells. */
fun streakLengthOn(day: LocalDate, activeDays: Set<LocalDate>): Int {
    var length = 0
    var candidate = day
    while (candidate in activeDays) {
        length++
        candidate = candidate.minusDays(1)
    }
    return length
}
