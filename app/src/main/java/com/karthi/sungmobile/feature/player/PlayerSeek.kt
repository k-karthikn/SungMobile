package com.karthi.sungmobile.feature.player

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    Column {
        Slider(state = slider,
            track = { PlayerSeekTrack({ slider.value / range.endInclusive }, engine.playing && scrub == null) }, onValueChange = { scrub = it },
            onValueChangeFinished = { scrub?.let { onSeek(it.toLong()) }; scrub = null },
            enabled = engine.seekable && !state.resolving && !engine.buffering,
            modifier = Modifier.testTag("seek_slider").semantics { contentDescription = label })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(scrub?.toLong() ?: engine.positionMs), Modifier.testTag("player_position"), style = MaterialTheme.typography.labelMedium)
            Text(formatTime(engine.durationMs), style = MaterialTheme.typography.labelMedium)
        }
    }
}
