package com.karthi.sungmobile.playback

import android.os.SystemClock
import com.karthi.sungmobile.core.data.convenience.ConvenienceRepository
import com.karthi.sungmobile.core.data.history.RoomHistoryRepository
import com.karthi.sungmobile.core.model.lyrics.lyricsVideoId
import com.karthi.sungmobile.core.playback.PlaybackState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Counts actual playing time; Private mode never produces a resume checkpoint. */
class LongResumeRecorder(
    state: StateFlow<PlaybackState>,
    private val repository: ConvenienceRepository,
    private val history: RoomHistoryRepository,
    scope: CoroutineScope,
) {
    private var privateMode = false
    private val policy = scope.launch { history.policy().collect { privateMode = it.privateMode } }
    private val ticker: Job = scope.launch {
        var pass: String? = null
        var playedMs = 0L
        var lastWrite = 0L
        var previous = SystemClock.elapsedRealtime()
        while (isActive) {
            delay(1_000)
            val now = SystemClock.elapsedRealtime()
            val delta = (now - previous).coerceIn(0, 2_000)
            previous = now
            val current = state.value
            if (current.passId != pass) { pass = current.passId; playedMs = 0; lastWrite = 0 }
            val track = current.track
            val duration = current.engine.durationMs.takeIf { it > 0 } ?: track?.durationMs ?: 0
            val videoId = track?.lyricsVideoId()
            if (privateMode) { playedMs = 0; continue }
            if (videoId == null || duration < 20 * 60_000 || !current.engine.playing ||
                current.engine.suppressed || current.engine.buffering) continue
            playedMs += delta
            val position = current.engine.positionMs
            if (playedMs >= 30_000 && now - lastWrite >= 5_000 && position in 30_000 until (duration - 30_000)) {
                if (runCatching { !history.policy().first().privateMode }.getOrDefault(false))
                    runCatching { repository.recordResume(videoId, position) }
                        .onSuccess { lastWrite = now }
            }
        }
    }
    fun close() { policy.cancel(); ticker.cancel() }
}
