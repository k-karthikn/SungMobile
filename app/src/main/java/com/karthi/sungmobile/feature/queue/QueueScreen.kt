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
import com.karthi.sungmobile.core.designsystem.component.SungConfirmDialog
import com.karthi.sungmobile.core.designsystem.theme.LocalSungMotionEnabled
import com.karthi.sungmobile.core.playback.PlaybackState
import com.karthi.sungmobile.core.model.library.libraryKeyOrNull

/** Deliberately simple: explicit accessible actions instead of a custom drag interaction. */
@Composable
fun QueueScreen(state: PlaybackState, actions: PlayerActions, onBack: () -> Unit, modifier: Modifier = Modifier,
    embedded: Boolean = false) {
    val library = com.karthi.sungmobile.feature.library.LocalLibraryUi.current
    val queue = state.queue
    val onlineCount = queue.entries.count { it.track.libraryKeyOrNull() != null }
    val motion = LocalSungMotionEnabled.current
    var confirmClear by remember { mutableStateOf(false) }
    if (confirmClear) SungConfirmDialog(stringResource(R.string.clear_upcoming),
        stringResource(R.string.clear_upcoming_body), stringResource(R.string.clear),
        { actions.clearUpcoming(); confirmClear = false }, { confirmClear = false })
    LazyColumn(modifier.fillMaxSize().testTag("queue_list"), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            if (!embedded) Row {
                IconButton(onBack) { SungIcon(SungSymbol.Back, stringResource(R.string.back)) }
                Text(stringResource(R.string.queue_count, queue.entries.size), Modifier.padding(12.dp), style = MaterialTheme.typography.titleLarge)
            }
            Row {
                TextButton({ confirmClear = true }, enabled = queue.currentIndex < queue.entries.lastIndex) { Text(stringResource(R.string.clear_upcoming)) }
                TextButton(actions.undo, enabled = state.canUndo, modifier = Modifier.testTag("queue_undo")) { Text(stringResource(R.string.undo)) }
            }
            TextButton(library.saveQueue, enabled = onlineCount > 0) {
                Text(if (onlineCount == queue.entries.size) "Save queue as playlist"
                    else "Save online songs as playlist")
            }
            if (queue.entries.isEmpty()) Text(stringResource(R.string.queue_empty))
        }
        itemsIndexed(queue.entries, key = { _, entry -> entry.id }) { index, entry ->
            var menu by remember { mutableStateOf(false) }
            val animation = if (motion) Modifier.animateItem() else Modifier
            com.karthi.sungmobile.core.designsystem.component.SungSongListItem(
                entry.track.title,
                if (entry.id == queue.currentId) "${stringResource(R.string.current_track)} · ${entry.track.subtitle}" else entry.track.subtitle,
                entry.track.artworkUrl, { actions.jump(entry.id) },
                modifier = animation.testTag("queue_entry_$index"), selected = entry.id == queue.currentId) {
                    Box {
                        IconButton({ menu = true }, Modifier.testTag("queue_options_$index")) { SungIcon(SungSymbol.More, stringResource(R.string.options)) }
                        DropdownMenu(menu, { menu = false }) {
                            com.karthi.sungmobile.feature.library.LibraryMenuItems(entry.track) { menu = false }
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
