package com.karthi.sungmobile.feature.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
            infiniteRepeatable(tween(1000, easing = LinearEasing)), label = "wave phase")
    } else remember { mutableFloatStateOf(0f) }
    val active = MaterialTheme.colorScheme.primary
    val inactive = MaterialTheme.colorScheme.surfaceContainerHighest
    Canvas(Modifier.fillMaxWidth().height(16.dp)) {
        val y = size.height / 2
        val thumbCenter = size.width * fraction().coerceIn(0f, 1f)
        // Sung's expressive slider leaves 6 dp beside its 4 dp handle.
        val handleClearance = 8.dp.toPx()
        val playedEnd = (thumbCenter - handleClearance).coerceAtLeast(0f)
        val unplayedStart = (thumbCenter + handleClearance).coerceAtMost(size.width)
        val stroke = 3.dp.toPx()
        if (unplayedStart < size.width) {
            drawRoundRect(inactive, topLeft = Offset(unplayedStart, 0f),
                size = Size(size.width - unplayedStart, size.height),
                cornerRadius = CornerRadius(size.height / 2))
            val stopX = size.width - 8.dp.toPx()
            if (unplayedStart < stopX - 4.dp.toPx()) {
                drawCircle(active, radius = 2.dp.toPx(), center = Offset(stopX, y))
            }
        }
        if (playedEnd > 0f) {
            val wave = Path()
            val wavelength = 28.dp.toPx()
            val amplitude = (if (playing) 3.dp else 1.dp).toPx()
            var x = 0f
            while (x <= playedEnd) {
                val py = y + sin((x / wavelength - phase.value) * 2 * PI).toFloat() * amplitude
                if (x == 0f) wave.moveTo(x, py) else wave.lineTo(x, py)
                x += 3.dp.toPx()
            }
            wave.lineTo(playedEnd, y + sin((playedEnd / wavelength - phase.value) * 2 * PI).toFloat() * amplitude)
            drawPath(wave, active, style = Stroke(stroke, cap = StrokeCap.Round))
        }
    }
}
