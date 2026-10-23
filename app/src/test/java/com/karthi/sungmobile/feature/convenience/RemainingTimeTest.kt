package com.karthi.sungmobile.feature.convenience

import org.junit.Assert.assertEquals
import org.junit.Test

class RemainingTimeTest {
    @Test fun durationTimerShowsSecondsInsteadOfRoundedMinutes() {
        assertEquals("1:00", formatRemainingTime(60_000))
        assertEquals("0:59", formatRemainingTime(59_000))
        assertEquals("0:01", formatRemainingTime(1))
        assertEquals("0:00", formatRemainingTime(0))
        assertEquals("15:30", formatRemainingTime(930_000))
    }
}
