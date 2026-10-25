package com.karthi.sungmobile.feature.queue

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.R
import com.karthi.sungmobile.app.PlayerActions
import com.karthi.sungmobile.core.designsystem.artwork.ArtworkMask
import com.karthi.sungmobile.core.designsystem.artwork.SungArtwork
import com.karthi.sungmobile.core.designsystem.icon.*
import com.karthi.sungmobile.core.designsystem.component.SungConfirmDialog
import com.karthi.sungmobile.core.designsystem.theme.LocalSungMotionEnabled
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.playback.PlaybackState
import com.karthi.sungmobile.core.model.library.libraryKeyOrNull
import java.util.Locale

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
    LazyColumn(modifier.fillMaxSize().testTag("queue_list"),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)) {
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
            val current = entry.id == queue.currentId
            val duration = entry.track.durationMs?.takeIf { it > 0 }
                ?: state.engine.durationMs.takeIf { current && it > 0 }
            QueueTrackRow(entry.track, current, duration, { actions.jump(entry.id) },
                animation.testTag("queue_entry_$index")) {
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

/** A queue uses a continuous list; only the playing entry gets a quiet container. */
@Composable
private fun QueueTrackRow(track: CatalogItem, current: Boolean, durationMs: Long?, onClick: () -> Unit,
    modifier: Modifier = Modifier, actions: @Composable () -> Unit) {
    val currentLabel = stringResource(R.string.current_track)
    val artist = queueArtist(track.subtitle)
    Surface(onClick = onClick, modifier = modifier.fillMaxWidth().semantics {
        if (current) contentDescription = "$currentLabel, ${track.title}, $artist"
    }, shape = RoundedCornerShape(16.dp),
        color = if (current) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SungArtwork(track.artworkUrl, Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)), mask = ArtworkMask.SQUARE)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(track.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (current) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface)
                Text(artist, style = MaterialTheme.typography.bodySmall, maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (current) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (durationMs != null) Text(formatQueueDuration(durationMs),
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1)
            actions()
        }
    }
}

/** Repair punctuation from queues saved before Home's provider-run parser was corrected. */
internal fun queueArtist(subtitle: String): String = subtitle.substringBefore('•')
    .replace(Regex("\\s*·\\s*([,&])\\s*·\\s*")) { match ->
        if (match.groupValues[1] == ",") ", " else " & "
    }
    .replace(Regex("\\s*·\\s*[,;&]\\s*$"), "")
    .replace(Regex("\\s*·\\s*[0-9.,]+[KMB]?\\s+plays\\s*$", RegexOption.IGNORE_CASE), "")
    .replace(Regex("\\s+"), " ").trim()

private fun formatQueueDuration(ms: Long): String {
    val seconds = ms / 1000
    return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60)
}
