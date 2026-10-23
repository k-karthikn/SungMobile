package com.karthi.sungmobile.app

import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.karthi.sungmobile.core.playback.PlaybackState
import kotlinx.coroutines.flow.*

/** Navigation/library chrome does not display a clock. Live screens opt into the original flow. */
internal fun Flow<PlaybackState>.chromeUpdates(): Flow<PlaybackState> = map { it.withoutClock() }.distinctUntilChanged()

internal fun PlaybackState.withoutClock(): PlaybackState = copy(
    engine = engine.copy(positionMs = 0), sleepTimer = sleepTimer?.copy(remainingMs = 0),
)

@Composable
internal fun LivePlaybackContent(fallback: PlaybackState, updates: StateFlow<PlaybackState>?,
    content: @Composable (PlaybackState) -> Unit) {
    if (updates == null) content(fallback)
    else {
        val live by updates.collectAsStateWithLifecycle()
        content(live)
    }
}
