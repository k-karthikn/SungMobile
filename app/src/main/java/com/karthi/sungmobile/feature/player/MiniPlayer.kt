package com.karthi.sungmobile.feature.player

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.karthi.sungmobile.R
import com.karthi.sungmobile.core.designsystem.artwork.SungArtwork
import com.karthi.sungmobile.core.designsystem.icon.*
import com.karthi.sungmobile.core.playback.PlaybackState
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MiniPlayer(state: PlaybackState, onOpen: () -> Unit, onToggle: () -> Unit, modifier: Modifier = Modifier,
    updates: StateFlow<PlaybackState>? = null) {
    val track = state.track ?: return
    Surface(onClick = onOpen, modifier = modifier.fillMaxWidth().testTag("mini_player"),
        shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.secondaryContainer) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SungArtwork(track.artworkUrl, Modifier.size(48.dp))
                Column(Modifier.weight(1f)) {
                    Text(track.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                FilledIconButton(onClick = onToggle, shapes = IconButtonDefaults.shapes(), enabled = state.connected,
                    modifier = Modifier.size(52.dp).testTag("mini_play_pause")) {
                    SungIcon(if (state.actionLabel() == R.string.pause) SungSymbol.Pause else SungSymbol.Play,
                        stringResource(state.actionLabel()), Modifier.size(28.dp))
                }
            }
            MiniPlayerProgress(state, updates)
        }
    }
}

/** Read the ticking state inside the progress lambda; artwork/text/navigation need no clock updates. */
@Composable
private fun MiniPlayerProgress(fallback: PlaybackState, updates: StateFlow<PlaybackState>?) {
    val clock = updates?.collectAsStateWithLifecycle()
    LinearProgressIndicator(progress = {
        val engine = (clock?.value ?: fallback).engine
        if (engine.durationMs > 0) (engine.positionMs.toFloat() / engine.durationMs).coerceIn(0f, 1f) else 0f
    }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(3.dp), drawStopIndicator = {})
}
