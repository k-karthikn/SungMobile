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
import com.karthi.sungmobile.core.model.lyrics.lyricsVideoId
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
    private lateinit var timer: SleepTimerController
    private var resumeRecorder: LongResumeRecorder? = null
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val container = (application as SungApplication).container
        playback = PlaybackCoordinator(LocalAwareStreamResolver(container.catalog), ExoPlayerEngine(this, serviceScope), serviceScope,
            relatedSource = container.catalog)
        playback.setConnected(false)
        timer = SleepTimerController(playback, serviceScope, android.os.SystemClock::elapsedRealtime)
        playback.onCompletedTrack = { track ->
            track.lyricsVideoId()?.let { id -> serviceScope.launch {
                runCatching { container.convenience.clearResume(id) }
            } }
        }
        sessionPlayer = SessionPlayer(playback, serviceScope)
        persistence = PlaybackPersistence(playback, container.queueStore, serviceScope, container.applicationScope)
        session = MediaSession.Builder(this, sessionPlayer).setCallback(PlaybackSessionCallback(applicationInfo.uid, restored, playback, timer, container, serviceScope))
            .setSessionActivity(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)).build()
        serviceScope.launch {
            playback.state.map { it.sessionStatus() }.distinctUntilChanged().collect { session?.setSessionExtras(it.toBundle()) }
        }
        serviceScope.launch { playback.state.collect { timer.onPlaybackChanged() } }
        serviceScope.launch {
            container.settings.playbackSpeed.collectLatest { playback.setSpeed(it) }
        }
        serviceScope.launch {
            container.settings.advancedPlayback.collect { settings ->
                playback.setSmartShuffleEnabled(settings.smartShuffle)
                playback.setTrackTrims(settings.trimsDb)
            }
        }
        serviceScope.launch {
            combine(container.settings.advancedPlayback, container.history.policy()) { settings, policy ->
                settings.relatedAutoplay && !policy.privateMode
            }.distinctUntilChanged().collect { playback.setRelatedAutoplayEnabled(it) }
        }
        serviceScope.launch {
            container.playbackShutdown?.join()
            persistence.restore()
            historyRecording = com.karthi.sungmobile.core.playback.history.HistoryRecording(
                playback.state, container.history, serviceScope, android.os.SystemClock::elapsedRealtime)
            resumeRecorder = LongResumeRecorder(playback.state, container.convenience, container.history, serviceScope)
            serviceScope.launch { historyRecording!!.error.collect { playback.setHistoryError(it) } }
            restored.set(Unit)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = session

    override fun onDestroy() {
        val container = (application as SungApplication).container
        val queueFlush = persistence.close()
        val historyFlush = historyRecording?.close(container.applicationScope)
        resumeRecorder?.close()
        timer.cancel()
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
