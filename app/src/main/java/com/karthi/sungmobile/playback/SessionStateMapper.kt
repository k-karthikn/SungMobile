package com.karthi.sungmobile.playback

import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import com.karthi.sungmobile.core.model.*
import com.karthi.sungmobile.core.playback.*
import com.karthi.sungmobile.core.playback.session.*

@androidx.annotation.OptIn(UnstableApi::class)
internal fun MediaController.playbackSnapshot(): PlaybackState? {
    val player = this
    val entries = (0 until player.mediaItemCount).mapNotNull { player.getMediaItemAt(it).toQueueEntry() }
    val currentId = player.currentMediaItem?.mediaId?.takeIf { id -> entries.any { it.id == id } }
    if (entries.isNotEmpty() && currentId == null) return null
    val extras = player.sessionExtras
    val repeat = when (player.repeatMode) { Player.REPEAT_MODE_ONE -> RepeatMode.ONE; Player.REPEAT_MODE_ALL -> RepeatMode.ALL; else -> RepeatMode.OFF }
    val queue = QueueState(entries, currentId, repeat, player.shuffleModeEnabled)
    return PlaybackState(track = queue.current?.track, queue = queue,
        resolving = extras?.getBoolean(PlaybackCommands.RESOLVING) == true,
        sourceError = extras?.getString(PlaybackCommands.SOURCE_ERROR)?.let { name -> SourceFailure.entries.find { it.name == name } },
        canUndo = extras?.getBoolean(PlaybackCommands.CAN_UNDO) == true,
        historyError = extras.getBoolean(PlaybackCommands.HISTORY_ERROR),
        storageError = extras?.getBoolean(PlaybackCommands.STORAGE_ERROR) == true,
        connected = player.isCommandAvailable(Player.COMMAND_SET_MEDIA_ITEM),
        engine = EngineSnapshot(playing = player.isPlaying, ready = player.playbackState == Player.STATE_READY,
            suppressed = player.playbackSuppressionReason != Player.PLAYBACK_SUPPRESSION_REASON_NONE,
            playRequested = player.playWhenReady, buffering = player.playbackState == Player.STATE_BUFFERING,
            positionMs = player.currentPosition.coerceAtLeast(0), durationMs = player.duration.coerceAtLeast(0),
            seekable = player.isCurrentMediaItemSeekable, ended = player.playbackState == Player.STATE_ENDED,
            error = if (player.playerError != null) EngineFailure.OTHER else null))
}
