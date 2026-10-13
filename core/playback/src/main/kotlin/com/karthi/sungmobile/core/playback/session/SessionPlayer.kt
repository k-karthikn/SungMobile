package com.karthi.sungmobile.core.playback.session

import android.os.Looper
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.karthi.sungmobile.core.model.RepeatMode
import com.karthi.sungmobile.core.playback.PlaybackCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Adapts the tested queue state machine to Media3's standard controller/notification protocol. */
@androidx.annotation.OptIn(UnstableApi::class)
class SessionPlayer(private val playback: PlaybackCoordinator, scope: CoroutineScope) : SimpleBasePlayer(Looper.getMainLooper()) {
    private val observation = scope.launch { playback.state.collect { invalidateState() } }

    override fun getState(): State {
        val state = playback.state.value
        val queue = state.queue
        val engine = state.engine
        val error = if (state.sourceError != null || engine.error != null)
            PlaybackException("Playback unavailable; retry or choose another track", null, PlaybackException.ERROR_CODE_IO_UNSPECIFIED) else null
        val commands = Player.Commands.Builder().addAll(
            Player.COMMAND_GET_CURRENT_MEDIA_ITEM, Player.COMMAND_GET_TIMELINE,
            Player.COMMAND_GET_METADATA, Player.COMMAND_RELEASE)
        if (state.connected) {
            commands.addAll(Player.COMMAND_SET_MEDIA_ITEM, Player.COMMAND_CHANGE_MEDIA_ITEMS,
                Player.COMMAND_SET_REPEAT_MODE, Player.COMMAND_SET_SHUFFLE_MODE,
                Player.COMMAND_PLAY_PAUSE, Player.COMMAND_PREPARE, Player.COMMAND_STOP)
            if (queue.current != null) commands.addAll(Player.COMMAND_SEEK_TO_MEDIA_ITEM, Player.COMMAND_SEEK_TO_PREVIOUS,
                Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
            if (queue.nextId(false) != null) commands.addAll(Player.COMMAND_SEEK_TO_NEXT, Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
            if (engine.seekable) commands.addAll(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
                Player.COMMAND_SEEK_BACK, Player.COMMAND_SEEK_FORWARD, Player.COMMAND_SEEK_TO_DEFAULT_POSITION)
        }
        return State.Builder().setAvailableCommands(commands.build())
            .setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build())
            .setPlaylist(queue.entries.map { entry ->
                val current = entry.id == queue.currentId
                val duration = if (current && engine.durationMs > 0) engine.durationMs else entry.track.durationMs
                MediaItemData.Builder(entry.id).setMediaItem(entry.toMediaItem())
                    .setDurationUs(duration?.takeIf { it > 0 }?.times(1000) ?: C.TIME_UNSET)
                    .setIsSeekable(current && engine.seekable).build()
            })
            .setCurrentMediaItemIndex(queue.currentIndex)
            .setContentPositionMs(engine.positionMs)
            .setPlayWhenReady(engine.playRequested, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            .setPlaybackSuppressionReason(if (engine.suppressed) Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS else Player.PLAYBACK_SUPPRESSION_REASON_NONE)
            .setPlaybackState(when {
                error != null || queue.entries.isEmpty() -> Player.STATE_IDLE
                state.resolving || engine.buffering -> Player.STATE_BUFFERING
                engine.ended -> Player.STATE_ENDED
                engine.ready -> Player.STATE_READY
                else -> Player.STATE_IDLE
            })
            .setIsLoading(state.resolving || engine.buffering)
            .setPlayerError(error)
            .setRepeatMode(when (queue.repeat) { RepeatMode.OFF -> Player.REPEAT_MODE_OFF; RepeatMode.ONE -> Player.REPEAT_MODE_ONE; RepeatMode.ALL -> Player.REPEAT_MODE_ALL })
            .setShuffleModeEnabled(queue.shuffled)
            .setSeekBackIncrementMs(10_000).setSeekForwardIncrementMs(10_000)
            .build()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean) = done { if (playWhenReady) playback.play() else playback.pause() }
    override fun handlePrepare() = done { if (playback.state.value.engine.playRequested) playback.play() }
    override fun handleStop() = done { playback.stop() }
    override fun handleRelease() = done { observation.cancel() }
    override fun handleSetRepeatMode(repeatMode: Int) = done {
        playback.setRepeat(when (repeatMode) { Player.REPEAT_MODE_ONE -> RepeatMode.ONE; Player.REPEAT_MODE_ALL -> RepeatMode.ALL; else -> RepeatMode.OFF })
    }
    override fun handleSetShuffleModeEnabled(shuffleModeEnabled: Boolean) = done { playback.setShuffle(shuffleModeEnabled) }
    override fun handleSetMediaItems(mediaItems: List<MediaItem>, startIndex: Int, startPositionMs: Long) = done {
        val entries = mediaItems.mapNotNull { it.toQueueEntry() }
        if (entries.size == mediaItems.size && entries.distinctBy { it.id }.size == entries.size)
            playback.replace(entries, startIndex.coerceAtLeast(0), startPositionMs.coerceAtLeast(0), play = playback.state.value.engine.playRequested,
                startShuffled = mediaItems.firstOrNull()?.requestMetadata?.extras?.takeIf { it.containsKey(PlaybackCommands.COLLECTION_SHUFFLE) }?.getBoolean(PlaybackCommands.COLLECTION_SHUFFLE))
    }
    override fun handleAddMediaItems(index: Int, mediaItems: List<MediaItem>) = done {
        val entries = mediaItems.mapNotNull { it.toQueueEntry() }
        val existing = playback.state.value.queue.entries.map { it.id }.toSet()
        if (entries.size == mediaItems.size && entries.none { it.id in existing } && entries.distinctBy { it.id }.size == entries.size)
            playback.insert(index, entries)
    }
    override fun handleMoveMediaItems(fromIndex: Int, toIndex: Int, newIndex: Int) = done { playback.moveRange(fromIndex, toIndex, newIndex) }
    override fun handleRemoveMediaItems(fromIndex: Int, toIndex: Int) = done {
        playback.removeIds(playback.state.value.queue.entries.subList(fromIndex, toIndex).map { it.id }.toSet())
    }
    override fun handleSeek(mediaItemIndex: Int, positionMs: Long, seekCommand: Int) = done {
        when (seekCommand) {
            Player.COMMAND_SEEK_TO_NEXT, Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> playback.next()
            Player.COMMAND_SEEK_TO_PREVIOUS -> playback.previous()
            Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> playback.seekEntry((playback.state.value.queue.currentIndex - 1).coerceAtLeast(0), 0)
            else -> playback.seekEntry(mediaItemIndex, positionMs.coerceAtLeast(0))
        }
    }
    private inline fun done(action: () -> Unit): ListenableFuture<Void?> { action(); return Futures.immediateVoidFuture() }
}
