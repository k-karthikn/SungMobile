package com.karthi.sungmobile.feature.streak

import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StreakEmberPaletteTest {
    @Test fun cyclePassesThroughTheFullSpectrumInOrder() {
        val landmarks = listOf(
            0.0 to 100.0,   // yellow
            45.0 to 55.0,   // orange
            75.0 to 25.0,   // red
            110.0 to 350.0, // pink
            160.0 to 300.0, // purple
            200.0 to 260.0, // blue
            265.0 to 195.0, // cyan
            315.0 to 145.0, // green
        )

        landmarks.forEach { (turnDegrees, expectedHue) ->
            val day = (turnDegrees / 360.0 * EMBER_CYCLE_DAYS).toInt() + 1
            assertHueNear(expectedHue, oklchHue(emberColor(day)), tolerance = 1.0)
        }
    }

    @Test fun colorRepeatsAtTheCycleBoundaryAndForStreaksBeyondFourYears() {
        val first = emberColor(1)
        assertEquals(first.value, emberColor(EMBER_CYCLE_DAYS + 1).value)
        assertEquals(first.value, emberColor(EMBER_CYCLE_DAYS * 5 + 1).value)
        assertEquals(first.value, emberColor(Int.MAX_VALUE - ((Int.MAX_VALUE - 1) % EMBER_CYCLE_DAYS)).value)
    }

    @Test fun everyDailyTransitionIncludingTheWrapIsSmooth() {
        for (day in 1..EMBER_CYCLE_DAYS) {
            val current = emberColor(day)
            val next = emberColor(day + 1)
            val distance = colorDistance(current, next)
            assertTrue("day $day to ${day + 1} jumped by $distance", distance < 0.015)
        }
    }

    @Test fun flameLayersShareHueAndKeepOuterBodyCoreDepth() {
        val palette = emberPalette(EMBER_CYCLE_DAYS / 3)
        val outerLightness = oklchLightness(palette.outer)
        val bodyLightness = oklchLightness(palette.body)
        val coreLightness = oklchLightness(palette.core)
        val hue = oklchHue(palette.body)

        assertTrue(outerLightness < bodyLightness)
        assertTrue(bodyLightness < coreLightness)
        assertHueNear(hue, oklchHue(palette.outer), tolerance = 0.5)
        assertHueNear(hue, oklchHue(palette.core), tolerance = 0.5)
        listOf(palette.outer, palette.body, palette.core).forEach { color ->
            assertTrue(color.red in 0f..1f && color.green in 0f..1f && color.blue in 0f..1f)
        }
    }

    private fun assertHueNear(expected: Double, actual: Double, tolerance: Double) {
        val difference = abs(((actual - expected + 540.0) % 360.0) - 180.0)
        assertTrue("expected hue $expected°, got $actual°", difference <= tolerance)
    }

    private fun colorDistance(first: Color, second: Color): Double {
        val red = first.red - second.red
        val green = first.green - second.green
        val blue = first.blue - second.blue
        return kotlin.math.sqrt((red * red + green * green + blue * blue).toDouble())
    }

    private fun oklchHue(color: Color): Double {
        val red = srgbToLinear(color.red.toDouble())
        val green = srgbToLinear(color.green.toDouble())
        val blue = srgbToLinear(color.blue.toDouble())
        val l = (0.4122214708 * red + 0.5363325363 * green + 0.0514459929 * blue).pow(1.0 / 3.0)
        val m = (0.2119034982 * red + 0.6806995451 * green + 0.1073969566 * blue).pow(1.0 / 3.0)
        val s = (0.0883024619 * red + 0.2817188376 * green + 0.6299787005 * blue).pow(1.0 / 3.0)
        val a = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s
        val b = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
        return ((atan2(b, a) * 180.0 / PI) % 360.0 + 360.0) % 360.0
    }

    private fun oklchLightness(color: Color): Double {
        val red = srgbToLinear(color.red.toDouble())
        val green = srgbToLinear(color.green.toDouble())
        val blue = srgbToLinear(color.blue.toDouble())
        val l = (0.4122214708 * red + 0.5363325363 * green + 0.0514459929 * blue).pow(1.0 / 3.0)
        val m = (0.2119034982 * red + 0.6806995451 * green + 0.1073969566 * blue).pow(1.0 / 3.0)
        val s = (0.0883024619 * red + 0.2817188376 * green + 0.6299787005 * blue).pow(1.0 / 3.0)
        return 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s
    }

    private fun srgbToLinear(value: Double): Double =
        if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
}
