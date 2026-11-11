package com.karthi.sungmobile.feature.streak

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StreakEmberPaletteTest {
    @Test fun fourYearCycleReturnsSeamlesslyToDayOneGold() {
        val first = emberColor(1)
        val nextCycle = emberColor(EMBER_CYCLE_DAYS + 1)
        assertEquals(first.value, nextCycle.value)
        assertTrue(first.red > first.blue)
        assertTrue(first.green > first.blue)
    }

    @Test fun hottestPointStaysInTheWarmRedMagentaRange() {
        val hottest = emberColor(EMBER_CYCLE_DAYS / 2 + 1)
        assertTrue(hottest.red > hottest.green)
        assertTrue(hottest.red > hottest.blue)
    }
}
