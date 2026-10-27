package com.karthi.sungmobile.feature.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.playback.PlaybackState

/** Reserves the same text space for every track so the seek and transport controls stay put. */
@Composable
internal fun PlayerTrackDetails(state: PlaybackState) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(state.track?.title.orEmpty(), Modifier.testTag("player_title").semantics { heading() },
                style = MaterialTheme.typography.headlineLarge, minLines = 2, maxLines = 2,
                overflow = TextOverflow.Ellipsis)
            Text(state.track?.subtitle.orEmpty(), style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            Text(stringResource(state.statusLabel()), Modifier.testTag("player_status"),
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.sourceError != null || state.engine.error != null)
                    MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        state.track?.let { PlayerLibraryActions(it) }
    }
}
