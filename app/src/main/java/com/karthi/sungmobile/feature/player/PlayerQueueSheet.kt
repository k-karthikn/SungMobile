package com.karthi.sungmobile.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.R
import com.karthi.sungmobile.app.PlayerActions
import com.karthi.sungmobile.core.designsystem.artwork.SungArtwork
import com.karthi.sungmobile.core.designsystem.icon.*
import com.karthi.sungmobile.core.playback.PlaybackState
import com.karthi.sungmobile.feature.queue.QueueScreen

/** The peek is always visible; sheet motion and nested list scrolling belong to Material 3. */
@Composable
internal fun PlayerQueueSheet(state: PlaybackState, actions: PlayerActions,
    expanded: Boolean, onToggle: () -> Unit, onCollapse: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize()) {
        val count = stringResource(R.string.queue_count, state.queue.entries.size)
        val action = stringResource(if (expanded) R.string.collapse_queue else R.string.expand_queue)
        Surface(onClick = onToggle, modifier = Modifier.fillMaxWidth().height(88.dp).testTag("open_queue")
            .semantics { contentDescription = "$action, $count" },
            color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)) {
                Box(Modifier.size(44.dp, 5.dp).background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape))
                Text(count, style = MaterialTheme.typography.titleMedium)
            }
        }
        state.track?.let { track ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).testTag("queue_now_playing"),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SungArtwork(track.artworkUrl, Modifier.size(52.dp))
                Column(Modifier.weight(1f)) {
                    Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text(track.subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                ExpressivePlayButton(state, actions.toggle, Modifier.testTag("queue_play_pause"),
                    size = 48.dp, iconSize = 24.dp)
                IconButton(actions.next, Modifier.size(48.dp), enabled = state.queue.nextId(false) != null) {
                    SungIcon(SungSymbol.SkipNext, stringResource(R.string.next_track))
                }
            }
        }
        HorizontalDivider()
        QueueScreen(state, actions, onCollapse, Modifier.weight(1f), embedded = true)
    }
}
