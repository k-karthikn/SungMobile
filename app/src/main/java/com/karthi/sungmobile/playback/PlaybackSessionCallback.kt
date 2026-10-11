package com.karthi.sungmobile.playback

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.*
import com.google.common.util.concurrent.*
import com.karthi.sungmobile.core.playback.*
import com.karthi.sungmobile.core.playback.session.*

@androidx.annotation.OptIn(UnstableApi::class)
class PlaybackSessionCallback(
    private val ownUid: Int,
    private val restored: ListenableFuture<Unit>,
    private val playback: PlaybackCoordinator,
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
            else -> return Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
        }
        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
    }
}
