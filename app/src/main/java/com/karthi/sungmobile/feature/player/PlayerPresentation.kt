package com.karthi.sungmobile.feature.player

import com.karthi.sungmobile.R
import com.karthi.sungmobile.core.playback.PlaybackState
import com.karthi.sungmobile.feature.search.message
import java.util.Locale

internal fun PlaybackState.actionLabel() = when {
    sourceError != null || engine.error != null -> R.string.retry
    engine.ended -> R.string.replay
    resolving || engine.playRequested -> R.string.pause
    else -> R.string.play
}

internal fun PlaybackState.statusLabel() = when {
    sourceError != null -> sourceError!!.message()
    engine.error != null -> R.string.playback_error
    resolving -> R.string.resolving
    engine.buffering && engine.playRequested -> R.string.buffering
    engine.playing -> R.string.playing
    engine.ended -> R.string.ended
    else -> R.string.paused
}

internal fun formatTime(ms: Long): String = String.format(Locale.ROOT, "%d:%02d", ms.coerceAtLeast(0) / 60_000, ms.coerceAtLeast(0) / 1000 % 60)
