package com.karthi.sungmobile.feature.player

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.R
import com.karthi.sungmobile.app.PlayerActions
import com.karthi.sungmobile.core.model.RepeatMode
import com.karthi.sungmobile.core.playback.PlaybackState

@Composable
internal fun QueueControls(state: PlaybackState, actions: PlayerActions, openQueue: () -> Unit) {
    val queue = state.queue
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(actions.previous, Modifier.weight(1f).testTag("previous_track"), enabled = queue.current != null) { Text(stringResource(R.string.previous_track)) }
            OutlinedButton(actions.next, Modifier.weight(1f).testTag("next_track"), enabled = queue.nextId(false) != null) { Text(stringResource(R.string.next_track)) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(queue.shuffled, { actions.shuffle(!queue.shuffled) }, label = { Text(stringResource(R.string.shuffle)) }, modifier = Modifier.heightIn(min = 48.dp))
            TextButton({ actions.repeat(when (queue.repeat) { RepeatMode.OFF -> RepeatMode.ALL; RepeatMode.ALL -> RepeatMode.ONE; RepeatMode.ONE -> RepeatMode.OFF }) }) {
                Text(stringResource(when (queue.repeat) { RepeatMode.OFF -> R.string.repeat_off; RepeatMode.ALL -> R.string.repeat_all; RepeatMode.ONE -> R.string.repeat_one }))
            }
        }
        FilledTonalButton(openQueue, Modifier.fillMaxWidth().testTag("open_queue")) { Text(stringResource(R.string.queue_count, queue.entries.size)) }
    }
}
