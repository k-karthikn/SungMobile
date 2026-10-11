package com.karthi.sungmobile.feature.queue

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.R
import com.karthi.sungmobile.app.PlayerActions
import com.karthi.sungmobile.core.designsystem.icon.*
import com.karthi.sungmobile.core.designsystem.theme.LocalSungMotionEnabled
import com.karthi.sungmobile.core.playback.PlaybackState

/** Deliberately simple: explicit accessible actions instead of a custom drag interaction. */
@Composable
fun QueueScreen(state: PlaybackState, actions: PlayerActions, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val queue = state.queue
    val motion = LocalSungMotionEnabled.current
    var confirmClear by remember { mutableStateOf(false) }
    if (confirmClear) AlertDialog(onDismissRequest = { confirmClear = false },
        title = { Text(stringResource(R.string.clear_upcoming)) }, text = { Text(stringResource(R.string.clear_upcoming_body)) },
        confirmButton = { TextButton({ actions.clearUpcoming(); confirmClear = false }) { Text(stringResource(R.string.clear)) } },
        dismissButton = { TextButton({ confirmClear = false }) { Text(stringResource(R.string.cancel)) } })
    LazyColumn(modifier.fillMaxSize().testTag("queue_list"), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row {
                IconButton(onBack) { SungIcon(SungSymbol.Back, stringResource(R.string.back)) }
                Text(stringResource(R.string.queue_count, queue.entries.size), Modifier.padding(12.dp), style = MaterialTheme.typography.titleLarge)
            }
            Row {
                TextButton({ confirmClear = true }, enabled = queue.currentIndex < queue.entries.lastIndex) { Text(stringResource(R.string.clear_upcoming)) }
                TextButton(actions.undo, enabled = state.canUndo, modifier = Modifier.testTag("queue_undo")) { Text(stringResource(R.string.undo)) }
            }
            if (queue.entries.isEmpty()) Text(stringResource(R.string.queue_empty))
        }
        itemsIndexed(queue.entries, key = { _, entry -> entry.id }) { index, entry ->
            var menu by remember { mutableStateOf(false) }
            val animation = if (motion) Modifier.animateItem() else Modifier
            Surface(onClick = { actions.jump(entry.id) }, modifier = animation.fillMaxWidth().testTag("queue_entry_$index"),
                color = if (entry.id == queue.currentId) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.medium) {
                Row(Modifier.padding(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(entry.track.title, style = MaterialTheme.typography.titleMedium)
                        Text(entry.track.subtitle, style = MaterialTheme.typography.bodyMedium)
                        if (entry.id == queue.currentId) Text(stringResource(R.string.current_track), style = MaterialTheme.typography.labelMedium)
                    }
                    Box {
                        TextButton({ menu = true }, Modifier.testTag("queue_options_$index")) { Text(stringResource(R.string.options)) }
                        DropdownMenu(menu, { menu = false }) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.move_up)) }, enabled = index > 0,
                                onClick = { actions.move(entry.id, -1); menu = false })
                            DropdownMenuItem(text = { Text(stringResource(R.string.move_down)) }, enabled = index < queue.entries.lastIndex,
                                onClick = { actions.move(entry.id, 1); menu = false })
                            DropdownMenuItem(text = { Text(stringResource(R.string.remove)) },
                                onClick = { actions.remove(entry.id); menu = false })
                        }
                    }
                }
            }
        }
    }
}
