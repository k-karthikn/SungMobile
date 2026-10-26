package com.karthi.sungmobile.playback

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.*
import com.google.common.util.concurrent.ListenableFuture
import com.karthi.sungmobile.core.model.*
import com.karthi.sungmobile.core.playback.*
import com.karthi.sungmobile.core.playback.session.*
import com.karthi.sungmobile.feature.feedback.DiagnosticLog
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/** Activity-scoped remote control. Releasing it does not stop service-owned audio. */
@androidx.annotation.OptIn(UnstableApi::class)
class PlaybackConnection(private val context: Context, private val scope: CoroutineScope) : PlaybackControl {
    private val mutableState = MutableStateFlow(PlaybackState(connected = false))
    override val state = mutableState.asStateFlow()
    private var controller: MediaController? = null
    private var future: ListenableFuture<MediaController>? = null
    private var closed = false
    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) { publish() }
    }
    private val ticker = scope.launch {
        state.map { it.engine.playing }.distinctUntilChanged().collectLatest { playing ->
            while (playing && isActive) { delay(250); publishPosition() }
        }
    }
    init { connect() }

    fun connect() {
        if (closed || controller != null || future != null) return
        val pending = MediaController.Builder(context, SessionToken(context, ComponentName(context, SungPlaybackService::class.java)))
            .setListener(object : MediaController.Listener {
                override fun onExtrasChanged(controller: MediaController, extras: Bundle) { publish() }
                override fun onDisconnected(controller: MediaController) {
                    this@PlaybackConnection.controller = null
                    future = null
                    mutableState.update { it.copy(connected = false, engine = it.engine.copy(playing = false, playRequested = false)) }
                }
            }).buildAsync()
        future = pending
        pending.addListener({
            if (!closed) {
                try { controller = pending.get().also { it.addListener(listener) }; publish() }
                catch (failure: Exception) {
                    DiagnosticLog.recordError("playback_connection_failed", failure)
                    future = null
                    mutableState.update { it.copy(connected = false) }
                }
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun publish() {
        val player = controller ?: return
        player.playbackSnapshot()?.let { mutableState.value = it }
    }

    private fun publishPosition() {
        val player = controller ?: return
        // Timeline/metadata and session extras arrive through listeners. The clock tick must
        // not reconstruct every queue entry (up to 500 songs) on the main thread.
        val position = player.positionSnapshot(mutableState.value)
        if (position != null) mutableState.value = position else publish()
    }

    override fun select(tracks: List<CatalogItem>, index: Int) {
        if (!state.value.connected || index !in tracks.indices) return
        controller?.apply {
            setMediaItems(tracks.map { QueueEntry(track = it).toMediaItem() }, index, 0)
            prepare()
            play()
        }
    }
    override fun playAt(track: CatalogItem, positionMs: Long) {
        if (!state.value.connected) return
        controller?.apply {
            val item = QueueEntry(track = track).toMediaItem().buildUpon().setRequestMetadata(
                androidx.media3.common.MediaItem.RequestMetadata.Builder().setExtras(Bundle().apply {
                    putBoolean(PlaybackCommands.COLLECTION_SHUFFLE, false)
                }).build()).build()
            setMediaItems(listOf(item), 0, positionMs.coerceIn(0, track.durationMs ?: 86_400_000))
            prepare(); play()
        }
    }
    override fun playCollection(tracks: List<CatalogItem>, shuffle: Boolean) {
        if (!state.value.connected || tracks.isEmpty()) return
        val items = tracks.map { QueueEntry(track = it).toMediaItem() }.toMutableList()
        items[0] = items[0].buildUpon().setRequestMetadata(androidx.media3.common.MediaItem.RequestMetadata.Builder()
            .setExtras(Bundle().apply { putBoolean(PlaybackCommands.COLLECTION_SHUFFLE, shuffle) }).build()).build()
        controller?.apply { setMediaItems(items, 0, 0); prepare(); play() }
    }
    override fun enqueueAll(tracks: List<CatalogItem>, next: Boolean) {
        controller?.apply {
            val index = if (next) (currentMediaItemIndex + 1).coerceIn(0, mediaItemCount) else mediaItemCount
            addMediaItems(index, tracks.map { QueueEntry(track = it).toMediaItem() })
        }
    }
    override fun toggle() {
        controller?.apply { if (playWhenReady && playbackState != Player.STATE_ENDED && playerError == null) pause() else { prepare(); play() } }
    }
    override fun seekTo(positionMs: Long) { controller?.seekTo(positionMs.coerceAtLeast(0)) }
    override fun stop() { controller?.stop() }
    override fun next() { controller?.seekToNext() }
    override fun previous() { controller?.seekToPrevious() }
    override fun enqueue(track: CatalogItem, next: Boolean) {
        controller?.apply {
            val index = if (next) (currentMediaItemIndex + 1).coerceIn(0, mediaItemCount) else mediaItemCount
            addMediaItem(index, QueueEntry(track = track).toMediaItem())
        }
    }
    override fun jump(entryId: String) {
        indexOf(entryId)?.let { controller?.apply { seekToDefaultPosition(it); play() } }
    }
    override fun remove(entryId: String) { indexOf(entryId)?.let { controller?.removeMediaItem(it) } }
    override fun move(entryId: String, offset: Int) {
        indexOf(entryId)?.let { from -> controller?.apply { moveMediaItem(from, (from + offset).coerceIn(0, mediaItemCount - 1)) } }
    }
    private fun indexOf(id: String): Int? = controller?.let { player ->
        (0 until player.mediaItemCount).firstOrNull { player.getMediaItemAt(it).mediaId == id }
    }
    override fun clearUpcoming() { custom(PlaybackCommands.CLEAR_UPCOMING) }
    override fun undo() { custom(PlaybackCommands.UNDO) }
    private fun custom(action: String) { controller?.sendCustomCommand(SessionCommand(action, Bundle.EMPTY), Bundle.EMPTY) }
    fun setSpeed(speed: PlaybackSpeed) { custom(PlaybackCommands.SPEED, Bundle().apply {
        putInt(PlaybackCommands.SPEED_TENTHS, speed.tenths)
        putBoolean(PlaybackCommands.PRESERVE_PITCH, speed.preservePitch)
    }) }
    fun armTimer(mode: SleepMode, minutes: Int = 0, fade: Boolean = false) { custom(PlaybackCommands.TIMER, Bundle().apply {
        putString(PlaybackCommands.TIMER_MODE, mode.name)
        putInt(PlaybackCommands.TIMER_MINUTES, minutes)
        putBoolean(PlaybackCommands.TIMER_FADE, fade)
    }) }
    fun cancelTimer() { custom(PlaybackCommands.CANCEL_TIMER) }
    fun restoreSession(id: String, completed: (Boolean) -> Unit) {
        val player = controller
        if (player == null) { completed(false); return }
        val result = player.sendCustomCommand(SessionCommand(PlaybackCommands.RESTORE_SESSION, Bundle.EMPTY),
            Bundle().apply { putString(PlaybackCommands.SESSION_ID, id) })
        result.addListener({ completed(runCatching {
            result.get().resultCode == SessionResult.RESULT_SUCCESS
        }.getOrDefault(false)) }, ContextCompat.getMainExecutor(context))
    }
    private fun custom(action: String, args: Bundle) {
        controller?.sendCustomCommand(SessionCommand(action, Bundle.EMPTY), args)
    }
    override fun setRepeat(mode: RepeatMode) { controller?.repeatMode = when (mode) { RepeatMode.ONE -> Player.REPEAT_MODE_ONE; RepeatMode.ALL -> Player.REPEAT_MODE_ALL; else -> Player.REPEAT_MODE_OFF } }
    override fun setShuffle(enabled: Boolean) { controller?.shuffleModeEnabled = enabled }
    fun close() {
        closed = true
        ticker.cancel()
        controller?.removeListener(listener)
        future?.let(MediaController::releaseFuture)
        controller = null
        future = null
    }
}
