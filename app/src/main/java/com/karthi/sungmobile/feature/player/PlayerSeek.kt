package com.karthi.sungmobile.feature.player

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.R
import com.karthi.sungmobile.core.playback.PlaybackState

@Composable
internal fun PlayerSeek(state: PlaybackState, onSeek: (Long) -> Unit) {
    val engine = state.engine
    var scrub by remember(state.queue.currentId, state.track?.id) { mutableStateOf<Float?>(null) }
    val range = 0f..engine.durationMs.coerceAtLeast(1).toFloat()
    val slider = remember(state.queue.currentId, state.track?.id, range) { SliderState(trackRange = range) }
    slider.value = (scrub ?: engine.positionMs.toFloat()).coerceIn(range)
    val label = stringResource(R.string.seek)
    val canSeek = engine.seekable && !state.resolving && !engine.buffering
    Column {
        Slider(state = slider,
            track = { PlayerSeekTrack({ slider.value / range.endInclusive }, engine.playing && scrub == null) }, onValueChange = { scrub = it },
            onValueChangeFinished = { scrub?.let { onSeek(it.toLong()) }; scrub = null },
            thumb = {
                Box(Modifier.size(width = 5.dp, height = 22.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50)))
            },
            enabled = canSeek,
            modifier = Modifier.testTag("seek_slider").semantics { contentDescription = label })
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(formatTime(scrub?.toLong() ?: engine.positionMs), Modifier.weight(1f).testTag("player_position"),
                style = MaterialTheme.typography.labelMedium)
            SeekStep(stringResource(R.string.back_ten_short), stringResource(R.string.back_ten),
                enabled = canSeek, onClick = { onSeek(engine.positionMs - 10_000) }, modifier = Modifier.testTag("seek_back"))
            SeekStep(stringResource(R.string.forward_ten_short), stringResource(R.string.forward_ten),
                enabled = canSeek, onClick = { onSeek(engine.positionMs + 10_000) }, modifier = Modifier.testTag("seek_forward"))
            Text(formatTime(engine.durationMs), Modifier.weight(1f), style = MaterialTheme.typography.labelMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.End)
        }
    }
}

@Composable
private fun SeekStep(text: String, description: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilledTonalIconButton(onClick = onClick, shapes = IconButtonDefaults.shapes(),
        modifier = modifier.size(48.dp).semantics { contentDescription = description }, enabled = enabled) {
        Text(text, style = MaterialTheme.typography.labelSmall)
    }
}
