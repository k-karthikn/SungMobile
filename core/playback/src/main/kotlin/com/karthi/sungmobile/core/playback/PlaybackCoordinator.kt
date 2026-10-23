package com.karthi.sungmobile.core.playback

import com.karthi.sungmobile.core.model.*
import com.karthi.sungmobile.core.model.lyrics.lyricsVideoId
import java.util.UUID
import kotlin.math.pow
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/** Service-owned playback state machine. All commands and engine events run on Main. */
class PlaybackCoordinator(
    private val resolver: StreamResolver,
    private val engine: PlayerEngine,
    private val scope: CoroutineScope,
    private val now: () -> Long = System::currentTimeMillis,
    private val relatedSource: RelatedSource? = null,
) : PlaybackControl {
    private val mutableState = MutableStateFlow(PlaybackState())
    override val state = mutableState.asStateFlow()
    private var resolution: Job? = null
    private var preparation: Job? = null
    private var prepared: Pair<String, ResolvedStream>? = null
    private var preparationAttemptedId: String? = null
    private var preparationGeneration = 0L
    private var relatedJob: Job? = null
    private var relatedGeneration = 0L
    private var relatedAutoplayEnabled = false
    private var generation = 0L
    private var refreshes = 0
    private var wantsPlayback = false
    private var loaded = false
    private var endedHandled = false
    private var resumePositionMs = 0L
    private var undoQueue: QueueState? = null
    private var smartShuffleEnabled = false
    private var trimDb = 0
    private var trackTrimsDb: Map<String, Int> = emptyMap()
    private var fadeVolume = 1f
    var onNaturalEnd: ((QueueState) -> Boolean)? = null
    var onCompletedTrack: ((CatalogItem) -> Unit)? = null
    var onStopped: (() -> Unit)? = null
    private val observation = scope.launch {
        engine.state.collect { snapshot ->
            if (!loaded) return@collect
            resumePositionMs = snapshot.positionMs
            mutableState.update { it.copy(engine = snapshot) }
            prepareNextIfNearEnd(snapshot)
            when {
                snapshot.error == EngineFailure.EXPIRED_STREAM && wantsPlayback && refreshes == 0 -> {
                    refreshes++
                    resolve(snapshot.positionMs)
                }
                snapshot.ended && !endedHandled && wantsPlayback -> {
                    endedHandled = true
                    val queue = state.value.queue
                    queue.current?.track?.let { onCompletedTrack?.invoke(it) }
                    if (onNaturalEnd?.invoke(queue) == true) pause()
                    else {
                        val nextId = queue.nextId(automatic = true)
                        if (nextId != null) activate(nextId, true, preparedStream = takePrepared(nextId))
                        else requestRelated(queue)
                    }
                }
                snapshot.error == null && !snapshot.ended -> wantsPlayback = snapshot.playRequested
            }
        }
    }

    fun select(track: CatalogItem) = select(listOf(track), 0)
    override fun select(tracks: List<CatalogItem>, index: Int) {
        if (tracks.isEmpty() || index !in tracks.indices) return
        replace(tracks.map { QueueEntry(track = it) }, index, 0, play = true)
    }

    fun replace(entries: List<QueueEntry>, index: Int, position: Long, play: Boolean, startShuffled: Boolean? = null) {
        resetUndo()
        val old = state.value.queue
        val queue = if (startShuffled == true && entries.isNotEmpty()) {
            val shuffled = entries.shuffled()
            val startingQueue = QueueState(shuffled, shuffled.first().id, old.repeat, true, entries.map { it.id })
            if (smartShuffleEnabled) startingQueue.smartShuffle() else startingQueue
        } else QueueState(entries, entries.getOrNull(index.coerceIn(0, entries.lastIndex.coerceAtLeast(0)))?.id,
            repeat = old.repeat).let { queue ->
            if (startShuffled ?: old.shuffled) {
                if (smartShuffleEnabled) queue.smartShuffle() else queue.shuffle(true)
            } else queue
        }
        mutableState.update { it.copy(queue = queue) }
        if (queue.currentId == null) stopEmpty() else activate(requireNotNull(queue.currentId), play, position)
    }

    override fun playAt(track: CatalogItem, positionMs: Long) {
        replace(listOf(QueueEntry(track = track)), 0, positionMs.coerceIn(0, track.durationMs ?: 86_400_000), true, false)
    }
    override fun playCollection(tracks: List<CatalogItem>, shuffle: Boolean) {
        if (tracks.isNotEmpty()) replace(tracks.map { QueueEntry(track = it) }, 0, 0, true, shuffle)
    }
    override fun enqueueAll(tracks: List<CatalogItem>, next: Boolean) {
        val queue = state.value.queue
        insert(if (next) queue.currentIndex + 1 else queue.entries.size, tracks.map { QueueEntry(track = it) })
    }

    fun restore(snapshot: QueueSnapshot) {
        cancelResolution()
        cancelPreparation()
        cancelRelated()
        loaded = false
        wantsPlayback = false
        engine.stop()
        resumePositionMs = snapshot.positionMs
        trimDb = trackTrimsDb[snapshot.queue.current?.track?.lyricsVideoId()] ?: 0
        applyVolume()
        mutableState.value = PlaybackState(track = snapshot.queue.current?.track, queue = snapshot.queue, passId = snapshot.passId,
            speed = state.value.speed,
            engine = EngineSnapshot(positionMs = resumePositionMs, durationMs = snapshot.queue.current?.track?.durationMs ?: 0))
    }

    fun setStorageError(failed: Boolean) { mutableState.update { it.copy(storageError = failed) } }
    fun setHistoryError(failed: Boolean) { mutableState.update { it.copy(historyError = failed) } }
    fun setSpeed(speed: PlaybackSpeed) {
        engine.setSpeed(speed)
        mutableState.update { it.copy(speed = speed) }
    }
    fun setSleepTimer(status: SleepTimerStatus?) { mutableState.update { it.copy(sleepTimer = status) } }
    fun setFadeVolume(volume: Float) { fadeVolume = volume.coerceIn(0f, 1f); applyVolume() }
    fun setTrimDb(db: Int) { trimDb = db.coerceIn(-12, 0); applyVolume() }
    fun setTrackTrims(trimsDb: Map<String, Int>) {
        trackTrimsDb = trimsDb
        setTrimDb(trimsDb[state.value.track?.lyricsVideoId()] ?: 0)
    }
    fun setSmartShuffleEnabled(enabled: Boolean) {
        if (smartShuffleEnabled == enabled) return
        smartShuffleEnabled = enabled
        if (enabled && state.value.queue.shuffled) {
            cancelPreparation()
            resetUndo()
            editQueue(state.value.queue.smartShuffle())
        }
    }
    fun setRelatedAutoplayEnabled(enabled: Boolean) {
        relatedAutoplayEnabled = enabled
        if (!enabled) cancelRelated()
    }
    private fun applyVolume() { engine.setVolume((10.0.pow(trimDb / 20.0) * fadeVolume).toFloat()) }
    fun setConnected(ready: Boolean) { mutableState.update { it.copy(connected = ready) } }

    private fun activate(id: String, play: Boolean, position: Long = 0, preparedStream: ResolvedStream? = null) {
        val queue = state.value.queue
        if (queue.entries.none { it.id == id }) return
        cancelPreparation()
        cancelRelated()
        trimDb = trackTrimsDb[queue.entries.first { it.id == id }.track.lyricsVideoId()] ?: 0
        applyVolume()
        refreshes = 0
        wantsPlayback = play
        loaded = false
        cancelResolution()
        engine.stop()
        resumePositionMs = position.coerceAtLeast(0)
        val updated = queue.copy(currentId = id)
        mutableState.update { it.copy(track = updated.current?.track, queue = updated, sourceError = null, passId = UUID.randomUUID().toString(),
            resolving = false, engine = EngineSnapshot(positionMs = resumePositionMs,
                durationMs = updated.current?.track?.durationMs ?: 0)) }
        if (play) {
            if (preparedStream != null && preparedStream.isFresh(now())) loadResolved(preparedStream, resumePositionMs)
            else resolve(resumePositionMs)
        }
    }

    private fun resolve(positionMs: Long) {
        val track = state.value.track ?: return
        cancelResolution()
        val request = generation
        loaded = false
        endedHandled = false
        resumePositionMs = positionMs
        engine.stop()
        mutableState.update { it.copy(resolving = true, sourceError = null,
            engine = EngineSnapshot(positionMs = positionMs, playRequested = wantsPlayback)) }
        resolution = scope.launch {
            try {
                val stream = resolver.resolve(track)
                if (request != generation) return@launch
                if (!stream.isFresh(now())) throw SourceException(SourceFailure.UNAVAILABLE)
                loadResolved(stream, positionMs)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (request == generation) mutableState.update { it.copy(resolving = false,
                    sourceError = (error as? SourceException)?.failure ?: SourceFailure.EXTRACTION,
                    engine = it.engine.copy(playRequested = false)) }
            }
        }
    }

    private fun loadResolved(stream: ResolvedStream, positionMs: Long) {
        endedHandled = false
        mutableState.update { current ->
            current.copy(track = stream.track, resolving = false, queue = current.queue.copy(
                entries = current.queue.entries.map { if (it.id == current.queue.currentId) it.copy(track = stream.track) else it }))
        }
        engine.load(stream, positionMs)
        loaded = true
        if (wantsPlayback) engine.play()
    }

    /** Keep only one transient signed URL; preparation downloads no audio bytes. */
    private fun prepareNextIfNearEnd(snapshot: EngineSnapshot) {
        if (!loaded || !wantsPlayback || !snapshot.playing || snapshot.ended || snapshot.error != null ||
            snapshot.durationMs <= 0 || snapshot.durationMs - snapshot.positionMs > 45_000) return
        val queue = state.value.queue
        val nextId = queue.nextId(automatic = true)?.takeUnless { it == queue.currentId } ?: return
        if (prepared?.first == nextId && prepared?.second?.isFresh(now()) == true) return
        if (preparationAttemptedId == nextId) return
        val track = queue.entries.firstOrNull { it.id == nextId }?.track ?: return
        cancelPreparation()
        preparationAttemptedId = nextId
        val request = preparationGeneration
        val fromId = queue.currentId
        preparation = scope.launch {
            try {
                val stream = resolver.resolve(track)
                if (request == preparationGeneration && wantsPlayback && state.value.queue.currentId == fromId &&
                    state.value.queue.nextId(automatic = true) == nextId && stream.isFresh(now())) {
                    prepared = nextId to stream
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Natural handover falls back to normal resolution.
            } finally {
                if (request == preparationGeneration) {
                    preparation = null
                }
            }
        }
    }

    private fun takePrepared(id: String): ResolvedStream? = prepared?.takeIf {
        it.first == id && it.second.isFresh(now())
    }?.second

    private fun cancelPreparation() {
        preparationGeneration++
        preparation?.cancel()
        preparation = null
        preparationAttemptedId = null
        prepared = null
    }

    private fun requestRelated(queue: QueueState) {
        val source = relatedSource ?: return
        val seed = queue.current?.track ?: return
        if (!relatedAutoplayEnabled || queue.repeat != RepeatMode.OFF || relatedJob != null) return
        val request = ++relatedGeneration
        relatedJob = scope.launch {
            try {
                val candidates = withTimeout(15_000) { source.related(seed, 5) }
                if (request != relatedGeneration || !wantsPlayback || !relatedAutoplayEnabled ||
                    state.value.queue != queue) return@launch
                val existing = queue.entries.map { it.track.id }.toSet()
                val usable = candidates.filter { it.kind == CatalogKind.SONG && it.id !in existing }
                    .distinctBy { it.id }.take(5)
                if (usable.isEmpty()) return@launch
                val added = usable.map { QueueEntry(track = it) }
                relatedJob = null
                resetUndo()
                editQueue(queue.insert(queue.entries.size, added))
                activate(added.first().id, true)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // An unavailable recommendation feed preserves normal end-of-queue behavior.
            } finally {
                if (request == relatedGeneration) relatedJob = null
            }
        }
    }

    private fun cancelRelated() {
        relatedGeneration++
        relatedJob?.cancel()
        relatedJob = null
    }

    fun play() {
        cancelRelated()
        val current = state.value
        wantsPlayback = true
        if (current.track != null && (current.passId == null || current.engine.ended))
            mutableState.update { it.copy(passId = UUID.randomUUID().toString()) }
        if (current.resolving) return
        if (!loaded || current.sourceError != null || current.engine.error != null) {
            refreshes = 0
            resolve(resumePositionMs)
        } else {
            if (current.engine.ended) { endedHandled = false; engine.seekTo(0) }
            engine.play()
        }
    }

    override fun toggle() {
        val current = state.value
        if ((current.resolving || current.engine.playRequested) && current.sourceError == null &&
            current.engine.error == null && !current.engine.ended) pause() else play()
    }

    fun pause() {
        wantsPlayback = false
        cancelPreparation()
        cancelRelated()
        if (state.value.resolving) cancelResolution()
        engine.pause()
        mutableState.update { it.copy(resolving = false, engine = it.engine.copy(playing = false, playRequested = false)) }
    }

    override fun seekTo(positionMs: Long) {
        val current = state.value.engine
        if (current.seekable && current.durationMs > 0) {
            cancelPreparation()
            engine.seekTo(positionMs.coerceIn(0, current.durationMs))
        }
    }

    override fun next() { state.value.queue.nextId(automatic = false)?.let { activate(it, wantsPlayback) } }
    override fun previous() {
        val queue = state.value.queue
        if (state.value.engine.positionMs > 3_000) {
            if (loaded) seekTo(0) else activate(queue.currentId ?: return, wantsPlayback)
        } else {
            val index = if (queue.currentIndex > 0) queue.currentIndex - 1
                else if (queue.repeat == RepeatMode.ALL) queue.entries.lastIndex else 0
            queue.entries.getOrNull(index)?.let { activate(it.id, wantsPlayback) }
        }
    }
    override fun jump(entryId: String) = activate(entryId, true)
    fun seekEntry(index: Int, position: Long) {
        val entry = state.value.queue.entries.getOrNull(index) ?: return
        if (entry.id == state.value.queue.currentId && loaded) seekTo(position)
        else activate(entry.id, wantsPlayback, position)
    }

    override fun enqueue(track: CatalogItem, next: Boolean) {
        val queue = state.value.queue
        insert(if (next) queue.currentIndex + 1 else queue.entries.size, listOf(QueueEntry(track = track)))
    }
    fun insert(index: Int, entries: List<QueueEntry>) {
        resetUndo()
        editQueue(state.value.queue.insert(index, entries))
    }
    override fun remove(entryId: String) = removeIds(setOf(entryId))
    fun removeIds(ids: Set<String>) {
        if (state.value.queue.entries.none { it.id in ids }) return
        undoQueue = state.value.queue
        editQueue(state.value.queue.remove(ids))
        mutableState.update { it.copy(canUndo = true) }
    }
    override fun clearUpcoming() {
        val queue = state.value.queue
        removeIds(queue.entries.drop(queue.currentIndex + 1).map { it.id }.toSet())
    }
    override fun move(entryId: String, offset: Int) {
        val queue = state.value.queue
        val from = queue.entries.indexOfFirst { it.id == entryId }
        moveRange(from, from + 1, (from + offset).coerceIn(0, queue.entries.lastIndex.coerceAtLeast(0)))
    }
    fun moveRange(from: Int, until: Int, destination: Int) {
        resetUndo()
        editQueue(state.value.queue.move(from, until, destination))
    }
    override fun undo() {
        val saved = undoQueue ?: return
        val currentId = state.value.queue.currentId
        resetUndo()
        // Restore deleted entries, never replace a currently playing successor with the removed song.
        editQueue(saved.copy(currentId = currentId?.takeIf { id -> saved.entries.any { it.id == id } } ?: saved.currentId))
    }
    override fun setRepeat(mode: RepeatMode) {
        cancelPreparation()
        cancelRelated()
        mutableState.update { it.copy(queue = it.queue.copy(repeat = mode)) }
    }
    override fun setShuffle(enabled: Boolean) {
        cancelPreparation()
        resetUndo()
        editQueue(if (enabled && smartShuffleEnabled && !state.value.queue.shuffled)
            state.value.queue.smartShuffle() else state.value.queue.shuffle(enabled))
    }

    private fun editQueue(queue: QueueState) {
        val previous = state.value.queue.currentId
        if (state.value.queue != queue) cancelRelated()
        if (state.value.queue.nextId(true) != queue.nextId(true) || previous != queue.currentId) cancelPreparation()
        val play = wantsPlayback
        mutableState.update { it.copy(queue = queue) }
        if (previous != queue.currentId) {
            if (queue.currentId == null) stopEmpty() else activate(requireNotNull(queue.currentId), play)
        }
    }
    private fun resetUndo() { undoQueue = null; mutableState.update { it.copy(canUndo = false) } }
    private fun cancelResolution() { generation++; resolution?.cancel(); resolution = null }
    private fun stopEmpty() {
        stop()
        mutableState.update { it.copy(track = null, queue = QueueState(repeat = it.queue.repeat)) }
    }
    override fun stop() {
        onStopped?.invoke()
        cancelResolution()
        cancelPreparation()
        cancelRelated()
        wantsPlayback = false
        loaded = false
        resumePositionMs = 0
        engine.stop()
        mutableState.update { it.copy(resolving = false, sourceError = null, engine = EngineSnapshot(), passId = null) }
    }
    fun release() { cancelResolution(); cancelPreparation(); cancelRelated(); observation.cancel(); engine.release() }
}
