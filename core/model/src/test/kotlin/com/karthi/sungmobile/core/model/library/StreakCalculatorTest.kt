package com.karthi.sungmobile.core.model.library

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StreakCalculatorTest {
    private val today = LocalDate.of(2026, 11, 11)

    @Test fun qualifiedDaysBuildCurrentAndLongestRuns() {
        val days = setOf(today.minusDays(6), today.minusDays(5), today.minusDays(3),
            today.minusDays(2), today.minusDays(1), today)
        val progress = calculateStreakProgress(days, today)
        assertEquals(4, progress.currentDays)
        assertEquals(4, progress.longestDays)
        assertEquals(4, streakLengthOn(today, progress.activeDays))
    }

    @Test fun yesterdayCanStillBeContinuedButAnOlderGapResetsCurrentRun() {
        val continuing = calculateStreakProgress(setOf(today.minusDays(2), today.minusDays(1)), today)
        assertEquals(2, continuing.currentDays)

        val reset = calculateStreakProgress(setOf(today.minusDays(3), today.minusDays(2)), today)
        assertEquals(0, reset.currentDays)
        assertEquals(2, reset.longestDays)
    }

    @Test fun ignoresFutureDaysAndKeepsOnlyLocalHistory() {
        val progress = calculateStreakProgress(setOf(today.minusDays(1), today, today.plusDays(1)), today)
        assertEquals(setOf(today.minusDays(1), today), progress.activeDays)
        assertTrue(today.plusDays(1) !in progress.activeDays)
    }
}
