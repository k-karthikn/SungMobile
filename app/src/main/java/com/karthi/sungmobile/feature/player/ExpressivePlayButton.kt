package com.karthi.sungmobile.feature.player

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import com.karthi.sungmobile.R
import com.karthi.sungmobile.core.designsystem.icon.SungIcon
import com.karthi.sungmobile.core.designsystem.icon.SungSymbol
import com.karthi.sungmobile.core.designsystem.theme.LocalSungMotionEnabled
import com.karthi.sungmobile.core.playback.PlaybackState

/** The mini, queue and full player share the same square-to-flower playback transition. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ExpressivePlayButton(state: PlaybackState, onClick: () -> Unit,
    modifier: Modifier = Modifier, size: Dp = 52.dp, iconSize: Dp = 26.dp) {
    val action = state.actionLabel()
    val busy = state.resolving || (state.engine.buffering && state.engine.playRequested)
    val motion = LocalSungMotionEnabled.current
    val colors = MaterialTheme.colorScheme
    val description = stringResource(if (busy) state.statusLabel() else action)
    val pauseShape = action == R.string.pause
    val progress = animateFloatAsState(if (pauseShape) 1f else 0f,
        animationSpec = if (motion) MaterialTheme.motionScheme.defaultSpatialSpec() else snap(),
        label = "Playback button shape").value
    val morph = remember { Morph(MaterialShapes.Square, MaterialShapes.Cookie9Sided) }
    val shape = remember(morph, progress) { playbackMorphShape(morph, progress) }
    Surface(onClick = onClick, enabled = state.connected,
        modifier = modifier.size(size).semantics { contentDescription = description },
        shape = shape,
        color = if (state.connected) colors.primary else colors.surfaceContainerHighest,
        contentColor = if (state.connected) colors.onPrimary else colors.onSurfaceVariant) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (busy) {
                if (motion) LoadingIndicator(Modifier.size(iconSize), color = LocalContentColor.current)
                else SungIcon(SungSymbol.Refresh, null, Modifier.size(iconSize))
            } else Crossfade(action, animationSpec = tween(if (motion) 180 else 0),
                label = "Playback action") { label ->
                SungIcon(if (label == R.string.pause) SungSymbol.Pause else SungSymbol.Play,
                    null, Modifier.size(iconSize))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun playbackMorphShape(morph: Morph, progress: Float): Shape = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = morph.toPath(progress, Path()).apply {
            transform(Matrix().apply { scale(size.width, size.height) })
            translate(size.center - getBounds().center)
        }
        return Outline.Generic(path)
    }
}
