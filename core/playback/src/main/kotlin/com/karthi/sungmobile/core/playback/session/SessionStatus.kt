package com.karthi.sungmobile.core.playback.session

import android.os.Bundle
import com.karthi.sungmobile.core.model.SourceFailure
import com.karthi.sungmobile.core.playback.PlaybackState

/** Session extras carry app-only status; MediaMetadata equality deliberately ignores extras. */
data class SessionStatus(val canUndo: Boolean, val storageError: Boolean, val resolving: Boolean, val sourceError: SourceFailure?, val historyError: Boolean = false) {
    fun toBundle() = Bundle().apply {
        putBoolean(PlaybackCommands.HISTORY_ERROR, historyError)
        putBoolean(PlaybackCommands.CAN_UNDO, canUndo)
        putBoolean(PlaybackCommands.STORAGE_ERROR, storageError)
        putBoolean(PlaybackCommands.RESOLVING, resolving)
        putString(PlaybackCommands.SOURCE_ERROR, sourceError?.name)
    }
}
fun PlaybackState.sessionStatus() = SessionStatus(canUndo, storageError, resolving, sourceError, historyError)
