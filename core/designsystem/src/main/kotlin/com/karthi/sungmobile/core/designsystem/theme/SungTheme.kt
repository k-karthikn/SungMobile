package com.karthi.sungmobile.core.designsystem.theme

import android.animation.ValueAnimator
import android.app.WallpaperManager
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class ColorMode { BLUE, WALLPAPER, ACCENT }
enum class ColorSchemeStyle { NEUTRAL, BALANCED, VIBRANT, EXPRESSIVE, FAITHFUL }

val LocalSungMotionEnabled = staticCompositionLocalOf { true }

/** All screens use the same roles; artwork never sits behind readable text. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SungTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    colorMode: ColorMode = ColorMode.BLUE,
    reduceMotion: Boolean = false,
    colorSchemeStyle: ColorSchemeStyle = ColorSchemeStyle.BALANCED,
    accentColor: Int = 0xFF006C91.toInt(),
    useArtworkAccent: Boolean = false,
    artworkUrl: String? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    var systemMotion by remember { mutableStateOf(ValueAnimator.areAnimatorsEnabled()) }
    LifecycleResumeEffect(context) {
        systemMotion = ValueAnimator.areAnimatorsEnabled()
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                systemMotion = ValueAnimator.areAnimatorsEnabled()
            }
        }
        context.contentResolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        onPauseOrDispose { context.contentResolver.unregisterContentObserver(observer) }
    }
    val motionEnabled = systemMotion && !reduceMotion
    var wallpaperSeed by remember(context) { mutableStateOf<Color?>(null) }
    var artworkSeed by remember(context) { mutableStateOf<Color?>(null) }
    LaunchedEffect(context, useArtworkAccent, artworkUrl) {
        artworkSeed = null
        if (useArtworkAccent && !artworkUrl.isNullOrBlank()) {
            artworkSeed = withContext(Dispatchers.IO) {
                runCatching {
                    val request = ImageRequest.Builder(context).data(artworkUrl).size(64, 64).build()
                    val result = SingletonImageLoader.get(context).execute(request) as? SuccessResult
                    result?.image?.toBitmap(64, 64, android.graphics.Bitmap.Config.ARGB_8888)
                        ?.let(::artworkAccentSeed)
                }.getOrNull()?.let(::Color)
            }
        }
    }
    LaunchedEffect(context, colorMode) {
        if (colorMode == ColorMode.WALLPAPER && Build.VERSION.SDK_INT >= 31) {
            wallpaperSeed = withContext(Dispatchers.IO) {
                runCatching { WallpaperManager.getInstance(context)
                    .getWallpaperColors(WallpaperManager.FLAG_SYSTEM)?.primaryColor?.toArgb() }
                    .getOrNull()?.let(::Color)
            }
        }
    }
    DisposableEffect(context, colorMode) {
        if (colorMode != ColorMode.WALLPAPER || Build.VERSION.SDK_INT < 31) {
            onDispose { }
        } else {
            val manager = WallpaperManager.getInstance(context)
            val listener = WallpaperManager.OnColorsChangedListener { colors, which ->
                if (which and WallpaperManager.FLAG_SYSTEM != 0) {
                    wallpaperSeed = colors?.primaryColor?.toArgb()?.let(::Color)
                }
            }
            val registered = runCatching {
                manager.addOnColorsChangedListener(listener, Handler(Looper.getMainLooper()))
            }.isSuccess
            onDispose {
                if (registered) runCatching { manager.removeOnColorsChangedListener(listener) }
            }
        }
    }
    val style = when (colorSchemeStyle) {
        ColorSchemeStyle.NEUTRAL -> PaletteStyle.Neutral
        ColorSchemeStyle.BALANCED -> PaletteStyle.TonalSpot
        ColorSchemeStyle.VIBRANT -> PaletteStyle.Vibrant
        ColorSchemeStyle.EXPRESSIVE -> PaletteStyle.Expressive
        ColorSchemeStyle.FAITHFUL -> PaletteStyle.Fidelity
    }
    val colors = when {
        useArtworkAccent && artworkSeed != null ->
            dynamicColorScheme(seedColor = artworkSeed!!, isDark = dark, style = style)
        colorMode == ColorMode.ACCENT ->
            dynamicColorScheme(seedColor = Color(accentColor), isDark = dark, style = style)
        colorMode == ColorMode.WALLPAPER && Build.VERSION.SDK_INT >= 31 && wallpaperSeed != null ->
            dynamicColorScheme(seedColor = wallpaperSeed!!, isDark = dark, style = style)
        colorMode == ColorMode.WALLPAPER && Build.VERSION.SDK_INT >= 31 ->
            if (colorSchemeStyle == ColorSchemeStyle.BALANCED) {
                if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                val systemPrimary = if (dark) dynamicDarkColorScheme(context).primary
                    else dynamicLightColorScheme(context).primary
                dynamicColorScheme(seedColor = systemPrimary, isDark = dark, style = style)
            }
        colorSchemeStyle != ColorSchemeStyle.BALANCED ->
            dynamicColorScheme(seedColor = Color(0xFF377893), isDark = dark, style = style)
        dark -> SungDarkColors
        else -> SungLightColors
    }
    CompositionLocalProvider(LocalSungMotionEnabled provides motionEnabled) {
        MaterialExpressiveTheme(
            colorScheme = colors,
            typography = SungTypography,
            motionScheme = if (motionEnabled) MotionScheme.expressive() else ImmediateMotion,
            content = content,
        )
    }
}

/** Select a visible, saturated colour from a small cached artwork thumbnail. */
private fun artworkAccentSeed(bitmap: android.graphics.Bitmap): Int? {
    val totals = Array(24) { FloatArray(5) }
    val hsv = FloatArray(3)
    for (y in 0 until bitmap.height step 3) for (x in 0 until bitmap.width step 3) {
        val pixel = bitmap.getPixel(x, y)
        if (android.graphics.Color.alpha(pixel) < 192) continue
        android.graphics.Color.colorToHSV(pixel, hsv)
        if (hsv[1] < .24f || hsv[2] < .16f || hsv[2] > .94f) continue
        val bucket = (hsv[0] / 15f).toInt().coerceIn(0, 23)
        val weight = hsv[1] * (1f - kotlin.math.abs(hsv[2] - .58f) * .7f)
        totals[bucket][0] += weight
        totals[bucket][1] += android.graphics.Color.red(pixel) * weight
        totals[bucket][2] += android.graphics.Color.green(pixel) * weight
        totals[bucket][3] += android.graphics.Color.blue(pixel) * weight
        totals[bucket][4] += 1f
    }
    val best = totals.maxByOrNull { it[0] } ?: return null
    if (best[4] < 3f || best[0] <= 0f) return null
    return android.graphics.Color.rgb((best[1] / best[0]).toInt().coerceIn(0, 255),
        (best[2] / best[0]).toInt().coerceIn(0, 255), (best[3] / best[0]).toInt().coerceIn(0, 255))
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private object ImmediateMotion : MotionScheme {
    override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> = snap()
    override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> = snap()
    override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> = snap()
    override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> = snap()
    override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> = snap()
    override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> = snap()
}
