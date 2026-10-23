package com.karthi.sungmobile.core.playback

import com.karthi.sungmobile.core.model.QueueState
import com.karthi.sungmobile.core.model.SleepMode
import com.karthi.sungmobile.core.model.SleepTimerStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Service-owned timer; monotonic deadlines are deliberately canceled after process death. */
class SleepTimerController(
    private val playback: PlaybackCoordinator,
    private val scope: CoroutineScope,
    private val elapsedMs: () -> Long,
) {
    private var mode: SleepMode? = null
    private var deadline = 0L
    private var anchorId: String? = null
    private var anchorPass: String? = null
    private var queueIds: List<String> = emptyList()
    private var fade = false
    private var ticker: Job? = null

    init {
        playback.onNaturalEnd = ::onNaturalEnd
        playback.onStopped = ::cancel
    }

    fun arm(mode: SleepMode, minutes: Int = 0, fade: Boolean = false) {
        require(mode != SleepMode.DURATION || minutes in 1..240)
        cancel()
        val state = playback.state.value
        if (state.queue.currentId == null) return
        this.mode = mode
        this.fade = mode == SleepMode.DURATION && fade
        deadline = if (mode == SleepMode.DURATION) elapsedMs() + minutes * 60_000L else 0
        anchorId = state.queue.currentId
        anchorPass = state.passId
        queueIds = state.queue.entries.map { it.id }
        publish()
        if (mode == SleepMode.DURATION) ticker = scope.launch {
            while (true) {
                delay(250)
                val remaining = deadline - elapsedMs()
                if (remaining <= 0) {
                    cancel()
                    playback.pause()
                    break
                }
                playback.setFadeVolume(if (this@SleepTimerController.fade && remaining < 10_000)
                    (remaining / 10_000f).coerceIn(0f, 1f) else 1f)
                publish()
            }
        }
    }

    fun onPlaybackChanged() {
        val currentMode = mode ?: return
        val state = playback.state.value
        if (currentMode == SleepMode.END_TRACK &&
            (state.queue.currentId != anchorId || (anchorPass != null && state.passId != anchorPass))) cancel()
        if (currentMode == SleepMode.END_QUEUE && state.queue.entries.map { it.id } != queueIds) cancel()
        if (currentMode == SleepMode.DURATION && state.queue.currentId != anchorId) {
            anchorId = state.queue.currentId
            if (fade) {
                fade = false
                playback.setFadeVolume(1f)
                publish()
            }
        }
        if (state.queue.currentId == null) cancel()
    }

    private fun onNaturalEnd(queue: QueueState): Boolean {
        val shouldPause = when (mode) {
            SleepMode.END_TRACK -> queue.currentId == anchorId &&
                (anchorPass == null || playback.state.value.passId == anchorPass)
            SleepMode.END_QUEUE -> queue.nextId(automatic = true) == null && queue.entries.map { it.id } == queueIds
            else -> false
        }
        if (shouldPause) cancel()
        return shouldPause
    }

    fun cancel() {
        ticker?.cancel()
        ticker = null
        mode = null
        fade = false
        playback.setFadeVolume(1f)
        playback.setSleepTimer(null)
    }

    private fun publish() {
        val current = mode ?: return
        playback.setSleepTimer(SleepTimerStatus(current,
            if (current == SleepMode.DURATION) (deadline - elapsedMs()).coerceAtLeast(0) else 0,
            fade))
    }
}
