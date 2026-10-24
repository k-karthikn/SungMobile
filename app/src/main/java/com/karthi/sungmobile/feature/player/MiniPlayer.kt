package com.karthi.sungmobile.feature.player

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.R
import com.karthi.sungmobile.app.PlayerActions
import com.karthi.sungmobile.core.designsystem.artwork.SungArtwork
import com.karthi.sungmobile.core.designsystem.icon.*
import com.karthi.sungmobile.core.designsystem.theme.LocalSungMotionEnabled
import com.karthi.sungmobile.core.playback.PlaybackState
import kotlinx.coroutines.flow.StateFlow

/** The same player chrome is hosted once by SungApp on Home, Search and Library. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MiniPlayer(state: PlaybackState, actions: PlayerActions, onOpen: () -> Unit,
    onLyrics: () -> Unit, onQueue: () -> Unit, onVolume: () -> Unit,
    modifier: Modifier = Modifier, updates: StateFlow<PlaybackState>? = null) {
    val track = state.track ?: return
    Surface(onClick = onOpen, modifier = modifier.fillMaxWidth().testTag("mini_player"),
        shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            BoxWithConstraints {
                val compact = maxWidth < 340.dp || LocalDensity.current.fontScale > 1.3f
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    SungArtwork(track.artworkUrl, Modifier.size(44.dp))
                    Column(Modifier.weight(1f).padding(start = 6.dp)) {
                        Text(track.title, style = MaterialTheme.typography.titleSmall,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(track.subtitle, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (!compact) MiniSkipButton(SungSymbol.SkipPrevious, R.string.previous_track,
                        actions.previous, state.connected && state.queue.current != null, "mini_previous")
                    MiniPlayButton(state, actions.toggle)
                    if (!compact) MiniSkipButton(SungSymbol.SkipNext, R.string.next_track,
                        actions.next, state.connected && state.queue.nextId(false) != null, "mini_next")
                    MiniPlayerMenu(state, actions, onLyrics, onQueue, onVolume)
                }
            }
            MiniPlayerProgress(state, updates, actions.seek)
        }
    }
}

@Composable
private fun MiniSkipButton(symbol: SungSymbol, label: Int, action: () -> Unit,
    enabled: Boolean, tag: String) {
    IconButton(action, modifier = Modifier.size(48.dp).testTag(tag), enabled = enabled) {
        SungIcon(symbol, stringResource(label), Modifier.size(22.dp))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MiniPlayButton(state: PlaybackState, toggle: () -> Unit) {
    val action = state.actionLabel()
    val busy = state.resolving || (state.engine.buffering && state.engine.playRequested)
    val playing = action == R.string.pause
    FilledIconToggleButton(checked = playing, onCheckedChange = { toggle() },
        enabled = state.connected, modifier = Modifier.size(52.dp).testTag("mini_play_pause"),
        shapes = IconButtonDefaults.toggleableShapes(
            shape = MaterialTheme.shapes.large,
            checkedShape = MaterialShapes.Cookie9Sided.toShape())) {
        if (busy && LocalSungMotionEnabled.current) {
            LoadingIndicator(Modifier.size(28.dp), color = LocalContentColor.current)
        } else {
            Crossfade(action, animationSpec = tween(if (LocalSungMotionEnabled.current) 180 else 0),
                label = "Mini player action") { label ->
                SungIcon(if (label == R.string.pause) SungSymbol.Pause else SungSymbol.Play,
                    stringResource(label), Modifier.size(26.dp))
            }
        }
    }
}
