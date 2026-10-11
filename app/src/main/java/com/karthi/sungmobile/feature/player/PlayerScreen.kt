package com.karthi.sungmobile.feature.player

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.R
import com.karthi.sungmobile.app.PlayerActions
import com.karthi.sungmobile.core.designsystem.artwork.ArtworkComposition
import com.karthi.sungmobile.core.designsystem.component.SungButton
import com.karthi.sungmobile.core.designsystem.icon.*
import com.karthi.sungmobile.core.playback.PlaybackState

@Composable
fun PlayerScreen(state: PlaybackState, actions: PlayerActions, onBack: () -> Unit, modifier: Modifier = Modifier, openQueue: () -> Unit = {}) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack, Modifier.size(48.dp)) { SungIcon(SungSymbol.Back, stringResource(R.string.close_player)) }
            Text(stringResource(R.string.now_playing), style = MaterialTheme.typography.titleMedium)
        }
        ArtworkComposition(state.track?.artworkUrl, Modifier.widthIn(max = 420.dp).fillMaxWidth().align(Alignment.CenterHorizontally))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(state.track?.title.orEmpty(), Modifier.testTag("player_title").semantics { heading() }, style = MaterialTheme.typography.headlineLarge)
            Text(state.track?.subtitle.orEmpty(), style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(state.statusLabel()), Modifier.testTag("player_status"), style = MaterialTheme.typography.bodyMedium,
                color = if (state.sourceError != null || state.engine.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        PlayerSeek(state, actions.seek)
        val largeText = LocalDensity.current.fontScale > 1.3f
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)) {
            val backLabel = stringResource(R.string.back_ten)
            val forwardLabel = stringResource(R.string.forward_ten)
            FilledTonalButton({ actions.seek(state.engine.positionMs - 10_000) }, Modifier.weight(1f).heightIn(min = 56.dp)
                .semantics { contentDescription = backLabel }, contentPadding = PaddingValues(8.dp),
                enabled = state.engine.seekable && !state.resolving && !state.engine.buffering) {
                Text(stringResource(if (largeText) R.string.back_ten_short else R.string.back_ten))
            }
            SungButton(actions.toggle, Modifier.width(96.dp).heightIn(min = 80.dp).testTag("play_pause")) {
                SungIcon(if (state.actionLabel() == R.string.pause) SungSymbol.Pause else SungSymbol.Play,
                    stringResource(state.actionLabel()), Modifier.size(36.dp))
            }
            FilledTonalButton({ actions.seek(state.engine.positionMs + 10_000) }, Modifier.weight(1f).heightIn(min = 56.dp).testTag("seek_forward")
                .semantics { contentDescription = forwardLabel }, contentPadding = PaddingValues(8.dp),
                enabled = state.engine.seekable && !state.resolving && !state.engine.buffering) {
                Text(stringResource(if (largeText) R.string.forward_ten_short else R.string.forward_ten))
            }
        }
        QueueControls(state, actions, openQueue)
        TextButton(actions.stop, Modifier.align(Alignment.CenterHorizontally).testTag("stop")) { Text(stringResource(R.string.stop)) }
        Text(stringResource(R.string.background_notice), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 24.dp))
    }
}

@Composable
private fun PlayerSeek(state: PlaybackState, onSeek: (Long) -> Unit) {
    val engine = state.engine
    var scrub by remember(state.queue.currentId, state.track?.id) { mutableStateOf<Float?>(null) }
    val range = 0f..engine.durationMs.coerceAtLeast(1).toFloat()
    val slider = remember(state.queue.currentId, state.track?.id, range) { SliderState(trackRange = range) }
    slider.value = (scrub ?: engine.positionMs.toFloat()).coerceIn(range)
    val label = stringResource(R.string.seek)
    Column {
        Slider(state = slider, onValueChange = { scrub = it },
            onValueChangeFinished = { scrub?.let { onSeek(it.toLong()) }; scrub = null },
            enabled = engine.seekable && !state.resolving && !engine.buffering,
            modifier = Modifier.testTag("seek_slider").semantics { contentDescription = label })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(scrub?.toLong() ?: engine.positionMs), Modifier.testTag("player_position"), style = MaterialTheme.typography.labelMedium)
            Text(formatTime(engine.durationMs), style = MaterialTheme.typography.labelMedium)
        }
    }
}
