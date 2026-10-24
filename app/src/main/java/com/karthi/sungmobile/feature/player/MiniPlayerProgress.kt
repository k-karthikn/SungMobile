package com.karthi.sungmobile.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.karthi.sungmobile.core.playback.PlaybackState
import kotlinx.coroutines.flow.StateFlow

/** The ticking clock is collected here so artwork, title, menus and navigation stay still. */
@Composable
internal fun MiniPlayerProgress(fallback: PlaybackState, updates: StateFlow<PlaybackState>?,
    onSeek: (Long) -> Unit) {
    val live = updates?.collectAsStateWithLifecycle()?.value ?: fallback
    val engine = live.engine
    val duration = engine.durationMs.coerceAtLeast(0)
    var scrub by remember(live.queue.currentId, live.track?.id) { mutableStateOf<Float?>(null) }
    Row(Modifier.fillMaxWidth().testTag("mini_progress"), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(formatTime(scrub?.toLong() ?: engine.positionMs), style = MaterialTheme.typography.labelSmall,
            maxLines = 1, modifier = Modifier.widthIn(min = 36.dp))
        if (duration > 0) {
            val range = 0f..duration.toFloat()
            val slider = remember(live.queue.currentId, live.track?.id, duration) { SliderState(trackRange = range) }
            slider.value = (scrub ?: engine.positionMs.toFloat()).coerceIn(range)
            Slider(state = slider,
                track = { PlayerSeekTrack({ slider.value / range.endInclusive }, engine.playing && scrub == null) },
                onValueChange = { scrub = it },
                onValueChangeFinished = { scrub?.let { onSeek(it.toLong()) }; scrub = null },
                thumb = { Box(Modifier.size(width = 4.dp, height = 22.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))) },
                enabled = live.connected && engine.seekable && !live.resolving && !engine.buffering,
                modifier = Modifier.weight(1f).height(48.dp).testTag("mini_seek")
                    .semantics { contentDescription = "Seek in ${live.track?.title.orEmpty()}" })
        } else {
            // A zero-duration stream must not pretend it is a seekable, finished song.
            Box(Modifier.weight(1f).height(48.dp), contentAlignment = Alignment.Center) {
                PlayerSeekTrack({ 0f }, false)
            }
        }
        Text(if (duration > 0) formatTime(duration) else "—:—",
            style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End,
            maxLines = 1, modifier = Modifier.widthIn(min = 36.dp))
    }
}
