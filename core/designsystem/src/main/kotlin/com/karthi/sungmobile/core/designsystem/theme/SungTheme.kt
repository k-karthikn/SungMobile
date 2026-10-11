package com.karthi.sungmobile.core.designsystem.theme

import android.animation.ValueAnimator
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
import androidx.lifecycle.compose.LifecycleResumeEffect

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class ColorMode { BLUE, WALLPAPER }

val LocalSungMotionEnabled = staticCompositionLocalOf { true }

/** All screens use the same roles; artwork never sits behind readable text. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SungTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    colorMode: ColorMode = ColorMode.BLUE,
    reduceMotion: Boolean = false,
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
    val colors = when {
        colorMode == ColorMode.WALLPAPER && Build.VERSION.SDK_INT >= 31 ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private object ImmediateMotion : MotionScheme {
    override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> = snap()
    override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> = snap()
    override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> = snap()
    override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> = snap()
    override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> = snap()
    override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> = snap()
}
