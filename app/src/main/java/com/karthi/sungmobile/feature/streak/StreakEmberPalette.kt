package com.karthi.sungmobile.feature.streak

import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

internal data class EmberPalette(val outer: Color, val body: Color, val core: Color)

/**
 * A continuous four-year OKLCH hue cycle. Day one starts at yellow, then travels through orange,
 * red, pink, purple, blue, cyan, and green before returning to yellow. Chroma follows the sRGB
 * gamut boundary smoothly so every hue stays vivid without clipping or hard-coded color stops.
 */
internal fun emberPalette(streakDays: Int): EmberPalette {
    val hue = emberHueDegrees(streakDays)
    return EmberPalette(
        outer = oklchColor(OUTER_LIGHTNESS, hue),
        body = oklchColor(BODY_LIGHTNESS, hue),
        core = oklchColor(CORE_LIGHTNESS, hue),
    )
}

internal fun emberColor(streakDays: Int): Color = oklchColor(BODY_LIGHTNESS, emberHueDegrees(streakDays))

/** Day one is phase zero; the day after the cycle's final day seamlessly starts the next lap. */
internal fun emberHueDegrees(streakDays: Int): Double {
    val dayInCycle = (streakDays.coerceAtLeast(1) - 1) % EMBER_CYCLE_DAYS
    val phase = dayInCycle.toDouble() / EMBER_CYCLE_DAYS
    return positiveModulo(START_HUE_DEGREES - FULL_TURN_DEGREES * phase, FULL_TURN_DEGREES)
}

internal const val EMBER_CYCLE_DAYS = 1_461

private const val START_HUE_DEGREES = 100.0 // yellow
private const val FULL_TURN_DEGREES = 360.0
private const val OUTER_LIGHTNESS = 0.63
private const val BODY_LIGHTNESS = 0.72
private const val CORE_LIGHTNESS = 0.91
private const val GAMUT_HEADROOM = 0.82
private const val MAX_CHROMA = 0.4

private data class LinearRgb(val red: Double, val green: Double, val blue: Double) {
    val inGamut get() = red in 0.0..1.0 && green in 0.0..1.0 && blue in 0.0..1.0
}

/**
 * Chooses a hue-specific chroma just inside the sRGB gamut. Binary search avoids hue-dependent
 * clipping and its color jumps while preserving the same perceptual lightness for each layer.
 */
private fun oklchColor(lightness: Double, hueDegrees: Double): Color {
    val chroma = maxSrgbChroma(lightness, hueDegrees) * GAMUT_HEADROOM
    val linear = oklchToLinearSrgb(lightness, chroma, hueDegrees)
    return Color(
        linearToSrgb(linear.red.coerceIn(0.0, 1.0)).toFloat(),
        linearToSrgb(linear.green.coerceIn(0.0, 1.0)).toFloat(),
        linearToSrgb(linear.blue.coerceIn(0.0, 1.0)).toFloat(),
    )
}

private fun maxSrgbChroma(lightness: Double, hueDegrees: Double): Double {
    var inGamut = 0.0
    var outOfGamut = MAX_CHROMA
    repeat(20) {
        val candidate = (inGamut + outOfGamut) / 2.0
        if (oklchToLinearSrgb(lightness, candidate, hueDegrees).inGamut) inGamut = candidate
        else outOfGamut = candidate
    }
    return inGamut
}

private fun oklchToLinearSrgb(lightness: Double, chroma: Double, hueDegrees: Double): LinearRgb {
    val hue = hueDegrees * PI / 180.0
    val a = chroma * cos(hue)
    val b = chroma * sin(hue)
    val l = (lightness + 0.3963377774 * a + 0.2158037573 * b).pow(3)
    val m = (lightness - 0.1055613458 * a - 0.0638541728 * b).pow(3)
    val s = (lightness - 0.0894841775 * a - 1.2914855480 * b).pow(3)
    return LinearRgb(
        red = 4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
        green = -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
        blue = -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s,
    )
}

private fun positiveModulo(value: Double, modulus: Double): Double =
    ((value % modulus) + modulus) % modulus

private fun linearToSrgb(value: Double): Double =
    if (value <= 0.0031308) value * 12.92 else 1.055 * value.pow(1.0 / 2.4) - 0.055
