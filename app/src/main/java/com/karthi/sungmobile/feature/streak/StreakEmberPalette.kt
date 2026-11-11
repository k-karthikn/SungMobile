package com.karthi.sungmobile.feature.streak

import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

internal data class EmberPalette(val outer: Color, val body: Color, val core: Color)

/**
 * A seamless four-year OKLCH cycle. Day one begins as soft gold, reaches ember-magenta through
 * orange and red, then returns through the same warm arc. It never crosses green, cyan, or blue.
 */
internal fun emberPalette(streakDays: Int): EmberPalette {
    val index = (streakDays.coerceAtLeast(1) - 1) % EMBER_CYCLE_DAYS
    val phase = index.toDouble() / EMBER_CYCLE_DAYS
    val heat = (1.0 - cos(2.0 * PI * phase)) / 2.0
    val lightness = 0.84 - 0.20 * heat
    val chroma = 0.08 + 0.12 * heat
    // 55° is gold and 328° is red-magenta; decreasing hue traverses only warm fire colors.
    val hue = 55.0 - 87.0 * heat
    return EmberPalette(
        outer = oklchToSrgb(lightness - 0.16, chroma * 0.70, hue),
        body = oklchToSrgb(lightness, chroma, hue),
        core = oklchToSrgb((lightness + 0.13).coerceAtMost(0.97), chroma * 0.32, hue),
    )
}

internal fun emberColor(streakDays: Int): Color = emberPalette(streakDays).body

internal const val EMBER_CYCLE_DAYS = 1_461

private data class LinearRgb(val red: Double, val green: Double, val blue: Double) {
    val inGamut get() = red in 0.0..1.0 && green in 0.0..1.0 && blue in 0.0..1.0
}

/** Converts OKLCH through linear sRGB, reducing chroma instead of clipping an out-of-gamut hue. */
private fun oklchToSrgb(lightness: Double, initialChroma: Double, hueDegrees: Double): Color {
    var chroma = initialChroma
    repeat(18) {
        val linear = oklchToLinearSrgb(lightness, chroma, hueDegrees)
        if (linear.inGamut) return Color(
            linearToSrgb(linear.red).toFloat(),
            linearToSrgb(linear.green).toFloat(),
            linearToSrgb(linear.blue).toFloat(),
        )
        chroma *= 0.82
    }
    val neutral = oklchToLinearSrgb(lightness, 0.0, hueDegrees)
    return Color(
        linearToSrgb(neutral.red.coerceIn(0.0, 1.0)).toFloat(),
        linearToSrgb(neutral.green.coerceIn(0.0, 1.0)).toFloat(),
        linearToSrgb(neutral.blue.coerceIn(0.0, 1.0)).toFloat(),
    )
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

private fun linearToSrgb(value: Double): Double =
    if (value <= 0.0031308) value * 12.92 else 1.055 * value.pow(1.0 / 2.4) - 0.055
