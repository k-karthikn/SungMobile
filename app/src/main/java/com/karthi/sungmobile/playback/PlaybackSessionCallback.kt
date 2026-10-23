package com.karthi.sungmobile.playback

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.*
import com.google.common.util.concurrent.*
import com.karthi.sungmobile.core.playback.*
import com.karthi.sungmobile.core.playback.session.*
import com.karthi.sungmobile.core.model.PlaybackSpeed
import com.karthi.sungmobile.core.model.SleepMode
import com.karthi.sungmobile.app.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@androidx.annotation.OptIn(UnstableApi::class)
class PlaybackSessionCallback(
    private val ownUid: Int,
    private val restored: ListenableFuture<Unit>,
    private val playback: PlaybackCoordinator,
    private val timer: SleepTimerController? = null,
    private val container: AppContainer? = null,
    private val scope: CoroutineScope? = null,
) : MediaSession.Callback {
    override fun onConnectAsync(session: MediaSession, controller: MediaSession.ControllerInfo): ListenableFuture<MediaSession.ConnectionResult> {
        val ownApp = controller.uid == ownUid
        if (!ownApp && !controller.isTrusted) return Futures.immediateFuture(MediaSession.ConnectionResult.reject())
        return Futures.transform(restored, {
            val commands = Player.Commands.Builder().addAllCommands()
            if (!ownApp) commands.removeAll(Player.COMMAND_SET_MEDIA_ITEM, Player.COMMAND_CHANGE_MEDIA_ITEMS)
            val custom = SessionCommands.Builder()
            if (ownApp) {
                custom.add(SessionCommand(PlaybackCommands.UNDO, Bundle.EMPTY))
                custom.add(SessionCommand(PlaybackCommands.CLEAR_UPCOMING, Bundle.EMPTY))
                custom.add(SessionCommand(PlaybackCommands.SPEED, Bundle.EMPTY))
                custom.add(SessionCommand(PlaybackCommands.TIMER, Bundle.EMPTY))
                custom.add(SessionCommand(PlaybackCommands.CANCEL_TIMER, Bundle.EMPTY))
                custom.add(SessionCommand(PlaybackCommands.RESTORE_SESSION, Bundle.EMPTY))
            }
            MediaSession.ConnectionResult.AcceptedResultBuilder(session, controller)
                .setAvailablePlayerCommands(commands.build()).setAvailableSessionCommands(custom.build()).build()
        }, MoreExecutors.directExecutor())
    }

    override fun onAddMediaItems(session: MediaSession, controller: MediaSession.ControllerInfo,
        mediaItems: List<MediaItem>): ListenableFuture<List<MediaItem>> =
        Futures.immediateFuture(if (controller.uid == ownUid) mediaItems else emptyList())

    override fun onCustomCommand(session: MediaSession, controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand, args: Bundle): ListenableFuture<SessionResult> {
        if (controller.uid != ownUid) return Futures.immediateFuture(SessionResult(SessionError.ERROR_PERMISSION_DENIED))
        when (customCommand.customAction) {
            PlaybackCommands.UNDO -> playback.undo()
            PlaybackCommands.CLEAR_UPCOMING -> playback.clearUpcoming()
            PlaybackCommands.SPEED -> {
                val speed = PlaybackSpeed(args.getInt(PlaybackCommands.SPEED_TENTHS, 10).coerceIn(5, 20),
                    args.getBoolean(PlaybackCommands.PRESERVE_PITCH, true))
                playback.setSpeed(speed)
                if (scope != null && container != null) scope.launch { container.settings.setPlaybackSpeed(speed) }
            }
            PlaybackCommands.TIMER -> {
                val mode = SleepMode.entries.firstOrNull { it.name == args.getString(PlaybackCommands.TIMER_MODE) }
                    ?: return Futures.immediateFuture(SessionResult(SessionError.ERROR_BAD_VALUE))
                val minutes = args.getInt(PlaybackCommands.TIMER_MINUTES)
                if (mode == SleepMode.DURATION && minutes !in 1..240)
                    return Futures.immediateFuture(SessionResult(SessionError.ERROR_BAD_VALUE))
                if (playback.state.value.queue.currentId == null)
                    return Futures.immediateFuture(SessionResult(SessionError.ERROR_BAD_VALUE))
                (timer ?: return Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED)))
                    .arm(mode, minutes, args.getBoolean(PlaybackCommands.TIMER_FADE))
            }
            PlaybackCommands.CANCEL_TIMER -> timer?.cancel()
            PlaybackCommands.RESTORE_SESSION -> {
                val id = args.getString(PlaybackCommands.SESSION_ID)
                    ?: return Futures.immediateFuture(SessionResult(SessionError.ERROR_BAD_VALUE))
                val owner = container ?: return Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
                val worker = scope ?: return Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
                val result = SettableFuture.create<SessionResult>()
                worker.launch {
                    try {
                        val saved = owner.convenience.session(id)
                        if (saved == null) result.set(SessionResult(SessionError.ERROR_BAD_VALUE))
                        else {
                            timer?.cancel()
                            playback.restore(saved.snapshot)
                            playback.setSpeed(saved.speed)
                            owner.settings.setPlaybackSpeed(saved.speed)
                            result.set(SessionResult(SessionResult.RESULT_SUCCESS))
                        }
                    } catch (_: Exception) {
                        result.set(SessionResult(SessionError.ERROR_BAD_VALUE))
                    }
                }
                return result
            }
            else -> return Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
        }
        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
    }
}
