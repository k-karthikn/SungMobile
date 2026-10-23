package com.karthi.sungmobile.feature.player

import androidx.compose.foundation.layout.*
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
import com.karthi.sungmobile.app.PlayerActions
import com.karthi.sungmobile.core.designsystem.component.SungButton
import com.karthi.sungmobile.core.designsystem.icon.*
import com.karthi.sungmobile.core.playback.PlaybackState

@Composable
internal fun PlayerTransport(state: PlaybackState, actions: PlayerActions) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)) {
        val backLabel = stringResource(R.string.back_ten)
        val forwardLabel = stringResource(R.string.forward_ten)
        FilledTonalButton({ actions.seek(state.engine.positionMs - 10_000) }, Modifier.weight(1f).heightIn(min = 56.dp)
            .semantics { contentDescription = backLabel }, contentPadding = PaddingValues(8.dp),
            enabled = state.engine.seekable && !state.resolving && !state.engine.buffering) {
            Text(stringResource(R.string.back_ten_short))
        }
        IconButton(actions.previous, Modifier.size(48.dp).testTag("previous_track"), enabled = state.queue.current != null) {
            SungIcon(SungSymbol.SkipPrevious, stringResource(R.string.previous_track))
        }
        SungButton(actions.toggle, Modifier.width(72.dp).heightIn(min = 80.dp).testTag("play_pause")) {
            SungIcon(if (state.actionLabel() == R.string.pause) SungSymbol.Pause else SungSymbol.Play,
                stringResource(state.actionLabel()), Modifier.size(36.dp))
        }
        IconButton(actions.next, Modifier.size(48.dp).testTag("next_track"), enabled = state.queue.nextId(false) != null) {
            SungIcon(SungSymbol.SkipNext, stringResource(R.string.next_track))
        }
        FilledTonalButton({ actions.seek(state.engine.positionMs + 10_000) }, Modifier.weight(1f).heightIn(min = 56.dp).testTag("seek_forward")
            .semantics { contentDescription = forwardLabel }, contentPadding = PaddingValues(8.dp),
            enabled = state.engine.seekable && !state.resolving && !state.engine.buffering) {
            Text(stringResource(R.string.forward_ten_short))
        }
    }
}
