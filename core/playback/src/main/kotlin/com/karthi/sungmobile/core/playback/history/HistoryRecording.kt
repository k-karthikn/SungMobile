package com.karthi.sungmobile.core.playback.history

import com.karthi.sungmobile.core.model.library.*
import com.karthi.sungmobile.core.playback.PlaybackState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/** One serialized recorder per service; repositories enforce privacy again at commit time. */
class HistoryRecording(
    private val playback: StateFlow<PlaybackState>,
    private val repository: HistoryRepository,
    scope: CoroutineScope,
    private val monotonic: () -> Long,
    private val wall: () -> Long = System::currentTimeMillis,
) {
    private data class Sample(val state: PlaybackState, val policy: HistoryPolicy, val monotonic: Long, val wall: Long)
    private val mutableError = MutableStateFlow(false)
    val error = mutableError.asStateFlow()
    private var accumulator: ListeningAccumulator? = null
    private var lastSaved: ListeningRecord? = null
    private val observer = scope.launch {
        combine(playback, repository.policy()) { state, policy -> Sample(state, policy, monotonic(), wall()) }
            .buffer().catch { mutableError.value = true }.collect { sample ->
                try { consume(sample); mutableError.value = false }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { mutableError.value = true }
            }
    }

    private suspend fun consume(sample: Sample) {
        val state = sample.state
        val track = state.track
        val pass = state.passId
        val id = if (pass != null && track != null && sample.policy.recordsListening) "$pass:${sample.policy.listeningEpoch}" else null
        if (accumulator?.record?.id != id) {
            accumulator?.let { old ->
                val record = old.sample(false, old.record.track.durationMs ?: 0, sample.monotonic, sample.wall, old.record.track)
                save(record, force = true)
            }
            accumulator = null
            lastSaved = null
            if (id != null && pass != null && track != null) {
                val checkpoint = repository.checkpoint(id)
                accumulator = ListeningAccumulator(pass, track, sample.policy.listeningEpoch, sample.wall, sample.monotonic, checkpoint)
                lastSaved = checkpoint
            }
        }
        val active = accumulator ?: return
        val playing = state.engine.playing && !state.engine.suppressed && !state.resolving
        val record = active.sample(playing, state.engine.durationMs.takeIf { it > 0 } ?: track?.durationMs ?: 0,
            sample.monotonic, sample.wall, track ?: active.record.track)
        save(record, force = !playing || record.qualified != lastSaved?.qualified)
    }

    private suspend fun save(record: ListeningRecord, force: Boolean) {
        if (record.listenedMs <= 0 || record == lastSaved) return
        if (!force && record.listenedMs - (lastSaved?.listenedMs ?: 0) < 5_000) return
        repository.recordListening(record)
        lastSaved = record
    }

    /** Drain the last segment after observers stop; the service waits before starting another recorder. */
    fun close(writerScope: CoroutineScope): Job {
        observer.cancel()
        return writerScope.launch {
            observer.join()
            accumulator?.let {
                try { save(it.sample(false, it.record.track.durationMs ?: 0, monotonic(), wall(), it.record.track), true) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { mutableError.value = true }
            }
        }
    }
}
