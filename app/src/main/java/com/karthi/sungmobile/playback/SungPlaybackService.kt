package com.karthi.sungmobile.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.*
import com.google.common.util.concurrent.*
import com.karthi.sungmobile.MainActivity
import com.karthi.sungmobile.SungApplication
import com.karthi.sungmobile.core.playback.*
import com.karthi.sungmobile.core.playback.session.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@androidx.annotation.OptIn(UnstableApi::class)
class SungPlaybackService : MediaSessionService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val restored = SettableFuture.create<Unit>()
    private lateinit var playback: PlaybackCoordinator
    private var historyRecording: com.karthi.sungmobile.core.playback.history.HistoryRecording? = null
    private lateinit var persistence: PlaybackPersistence
    private lateinit var sessionPlayer: SessionPlayer
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val container = (application as SungApplication).container
        playback = PlaybackCoordinator(container.catalog, ExoPlayerEngine(this, serviceScope), serviceScope)
        playback.setConnected(false)
        sessionPlayer = SessionPlayer(playback, serviceScope)
        persistence = PlaybackPersistence(playback, container.queueStore, serviceScope, container.applicationScope)
        session = MediaSession.Builder(this, sessionPlayer).setCallback(PlaybackSessionCallback(applicationInfo.uid, restored, playback))
            .setSessionActivity(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)).build()
        serviceScope.launch {
            playback.state.map { it.sessionStatus() }.distinctUntilChanged().collect { session?.setSessionExtras(it.toBundle()) }
        }
        serviceScope.launch {
            container.playbackShutdown?.join()
            persistence.restore()
            historyRecording = com.karthi.sungmobile.core.playback.history.HistoryRecording(
                playback.state, container.history, serviceScope, android.os.SystemClock::elapsedRealtime)
            serviceScope.launch { historyRecording!!.error.collect { playback.setHistoryError(it) } }
            restored.set(Unit)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = session

    override fun onDestroy() {
        val container = (application as SungApplication).container
        val queueFlush = persistence.close()
        val historyFlush = historyRecording?.close(container.applicationScope)
        container.playbackShutdown = container.applicationScope.launch { queueFlush.join(); historyFlush?.join() }
        restored.cancel(false)
        session?.release()
        sessionPlayer.release()
        playback.release()
        serviceScope.cancel()
        session = null
        super.onDestroy()
    }

}
