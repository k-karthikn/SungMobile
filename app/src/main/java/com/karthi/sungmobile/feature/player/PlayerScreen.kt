package com.karthi.sungmobile.feature.player

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.R
import com.karthi.sungmobile.app.PlayerActions
import com.karthi.sungmobile.core.designsystem.icon.*
import com.karthi.sungmobile.core.playback.PlaybackState

@Composable
fun PlayerScreen(state: PlaybackState, actions: PlayerActions, onBack: () -> Unit, modifier: Modifier = Modifier,
    openQueue: () -> Unit = {}, openLyrics: () -> Unit = {},
    moreContent: @Composable () -> Unit = {},
    convenienceContent: @Composable () -> Unit = {}) {
    var more by remember { mutableStateOf(false) }
    if (more) PlayerMoreSheet(actions, { more = false }, moreContent)
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack, Modifier.size(48.dp)) { SungIcon(SungSymbol.Back, stringResource(R.string.close_player)) }
            Text(stringResource(R.string.now_playing), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            IconButton({ more = true }, Modifier.testTag("player_more")) { SungIcon(SungSymbol.More, "More playback options") }
        }
        PlayerArtwork(state, actions.jump, Modifier.widthIn(max = 420.dp).fillMaxWidth().align(Alignment.CenterHorizontally))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(state.track?.title.orEmpty(), Modifier.testTag("player_title").semantics { heading() }, style = MaterialTheme.typography.headlineLarge)
                Text(state.track?.subtitle.orEmpty(), style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(state.statusLabel()), Modifier.testTag("player_status"), style = MaterialTheme.typography.bodyMedium,
                    color = if (state.sourceError != null || state.engine.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            state.track?.let { PlayerLibraryActions(it) }
        }
        PlayerSeek(state, actions.seek)
        PlayerTransport(state, actions)
        QueueControls(state, actions, openQueue)
        convenienceContent()
        FilledTonalButton(openLyrics, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.lyrics)) }
        Text(stringResource(R.string.background_notice), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 24.dp))
    }
}
