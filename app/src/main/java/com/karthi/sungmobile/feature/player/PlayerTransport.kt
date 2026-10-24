package com.karthi.sungmobile.feature.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.R
import com.karthi.sungmobile.app.PlayerActions
import com.karthi.sungmobile.core.designsystem.icon.*
import com.karthi.sungmobile.core.designsystem.theme.LocalSungMotionEnabled
import com.karthi.sungmobile.core.model.RepeatMode
import com.karthi.sungmobile.core.playback.PlaybackState

@Composable
internal fun PlayerTransport(state: PlaybackState, actions: PlayerActions) {
    val queue = state.queue
    val repeatLabel = stringResource(when (queue.repeat) {
        RepeatMode.OFF -> R.string.repeat_off
        RepeatMode.ALL -> R.string.repeat_all
        RepeatMode.ONE -> R.string.repeat_one
    })
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val compact = maxWidth < 310.dp
        val sideWidth = if (compact) 52.dp else 64.dp
        val playSize = if (compact) 72.dp else 80.dp
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            TransportModeButton(SungSymbol.Repeat, repeatLabel, queue.repeat != RepeatMode.OFF,
                oneBadge = queue.repeat == RepeatMode.ONE, modifier = Modifier.testTag("repeat_toggle"), onClick = {
                    actions.repeat(when (queue.repeat) {
                        RepeatMode.OFF -> RepeatMode.ALL
                        RepeatMode.ALL -> RepeatMode.ONE
                        RepeatMode.ONE -> RepeatMode.OFF
                    })
                })
            FilledTonalIconButton(onClick = actions.previous, shapes = IconButtonDefaults.shapes(),
                modifier = Modifier.width(sideWidth).height(playSize).testTag("previous_track"),
                enabled = queue.current != null) {
                SungIcon(SungSymbol.SkipPrevious, stringResource(R.string.previous_track), Modifier.size(28.dp))
            }
            ExpressivePlayButton(state, actions.toggle, Modifier.testTag("play_pause"),
                size = playSize, iconSize = 34.dp)
            FilledTonalIconButton(onClick = actions.next, shapes = IconButtonDefaults.shapes(),
                modifier = Modifier.width(sideWidth).height(playSize).testTag("next_track"),
                enabled = queue.nextId(false) != null) {
                SungIcon(SungSymbol.SkipNext, stringResource(R.string.next_track), Modifier.size(28.dp))
            }
            TransportModeButton(SungSymbol.Shuffle, stringResource(R.string.shuffle), queue.shuffled,
                modifier = Modifier.testTag("shuffle_toggle"), onClick = { actions.shuffle(!queue.shuffled) })
        }
    }
}

@Composable
private fun TransportModeButton(symbol: SungSymbol, label: String, active: Boolean,
    onClick: () -> Unit, modifier: Modifier = Modifier, oneBadge: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    val duration = if (LocalSungMotionEnabled.current) 200 else 0
    val container = animateColorAsState(if (active) colors.primaryContainer else colors.surface,
        tween(duration), label = "$label container")
    val foreground = animateColorAsState(if (active) colors.onPrimaryContainer else colors.onSurfaceVariant,
        tween(duration), label = "$label icon")
    Surface(onClick = onClick, modifier = modifier.size(48.dp).semantics {
        contentDescription = label
        selected = active
    },
        shape = CircleShape, border = BorderStroke(1.dp, colors.outlineVariant),
        color = container.value, contentColor = foreground.value) {
        Box(Modifier.fillMaxSize().padding(11.dp), contentAlignment = Alignment.Center) {
            SungIcon(symbol, null, Modifier.size(24.dp))
            if (oneBadge) Text("1", style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.BottomEnd))
        }
    }
}
