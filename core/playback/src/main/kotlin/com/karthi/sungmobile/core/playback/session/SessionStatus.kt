package com.karthi.sungmobile.core.playback.session

import android.os.Bundle
import com.karthi.sungmobile.core.model.SourceFailure
import com.karthi.sungmobile.core.playback.PlaybackState
import com.karthi.sungmobile.core.model.PlaybackSpeed
import com.karthi.sungmobile.core.model.SleepTimerStatus

/** Session extras carry app-only status; MediaMetadata equality deliberately ignores extras. */
data class SessionStatus(val canUndo: Boolean, val storageError: Boolean, val resolving: Boolean,
    val sourceError: SourceFailure?, val historyError: Boolean = false,
    val speed: PlaybackSpeed = PlaybackSpeed(), val timer: SleepTimerStatus? = null,
    val originalOrder: List<String> = emptyList()) {
    fun toBundle() = Bundle().apply {
        putBoolean(PlaybackCommands.HISTORY_ERROR, historyError)
        putBoolean(PlaybackCommands.CAN_UNDO, canUndo)
        putBoolean(PlaybackCommands.STORAGE_ERROR, storageError)
        putBoolean(PlaybackCommands.RESOLVING, resolving)
        putString(PlaybackCommands.SOURCE_ERROR, sourceError?.name)
        putInt(PlaybackCommands.SPEED_TENTHS, speed.tenths)
        putBoolean(PlaybackCommands.PRESERVE_PITCH, speed.preservePitch)
        putString(PlaybackCommands.TIMER_MODE, timer?.mode?.name)
        putLong(PlaybackCommands.TIMER_REMAINING, timer?.remainingMs ?: 0)
        putBoolean(PlaybackCommands.TIMER_FADE, timer?.fade == true)
        putStringArrayList(PlaybackCommands.ORIGINAL_ORDER, ArrayList(originalOrder))
    }
}
fun PlaybackState.sessionStatus() = SessionStatus(canUndo, storageError, resolving, sourceError,
    historyError, speed, sleepTimer, queue.originalOrder)
