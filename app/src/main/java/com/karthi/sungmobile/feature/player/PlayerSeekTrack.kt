package com.karthi.sungmobile.feature.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.karthi.sungmobile.core.designsystem.theme.LocalSungMotionEnabled
import kotlin.math.PI
import kotlin.math.sin

/** Draw-only animation: the Material slider retains scrubbing, focus and accessibility. */
@Composable
internal fun PlayerSeekTrack(fraction: () -> Float, playing: Boolean) {
    var resumed by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        resumed = true
        onPauseOrDispose { resumed = false }
    }
    val moving = playing && resumed && LocalSungMotionEnabled.current
    val phase = if (moving) {
        rememberInfiniteTransition(label = "seek wave").animateFloat(0f, 1f,
            infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "wave phase")
    } else remember { mutableFloatStateOf(0f) }
    val active = MaterialTheme.colorScheme.primary
    val inactive = MaterialTheme.colorScheme.surfaceContainerHighest
    Canvas(Modifier.fillMaxWidth().height(12.dp)) {
        val y = size.height / 2
        val end = size.width * fraction().coerceIn(0f, 1f)
        val stroke = 3.dp.toPx()
        drawLine(inactive, Offset(end, y), Offset(size.width, y), stroke, StrokeCap.Round)
        if (end > 0) {
            if (!moving) drawLine(active, Offset(0f, y), Offset(end, y), stroke, StrokeCap.Round)
            else {
                val wave = Path()
                val wavelength = 36.dp.toPx()
                var x = 0f
                while (x <= end) {
                    val py = y + sin((x / wavelength - phase.value) * 2 * PI).toFloat() * 2.dp.toPx()
                    if (x == 0f) wave.moveTo(x, py) else wave.lineTo(x, py)
                    x += 3.dp.toPx()
                }
                wave.lineTo(end, y + sin((end / wavelength - phase.value) * 2 * PI).toFloat() * 2.dp.toPx())
                drawPath(wave, active, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
    }
}
