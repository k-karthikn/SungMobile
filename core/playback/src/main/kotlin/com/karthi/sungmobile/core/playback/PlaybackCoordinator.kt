package com.karthi.sungmobile.core.playback

import com.karthi.sungmobile.core.model.*
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/** Service-owned playback state machine. All commands and engine events run on Main. */
class PlaybackCoordinator(
    private val resolver: StreamResolver,
    private val engine: PlayerEngine,
    private val scope: CoroutineScope,
    private val now: () -> Long = System::currentTimeMillis,
) : PlaybackControl {
    private val mutableState = MutableStateFlow(PlaybackState())
    override val state = mutableState.asStateFlow()
    private var resolution: Job? = null
    private var generation = 0L
    private var refreshes = 0
    private var wantsPlayback = false
    private var loaded = false
    private var endedHandled = false
    private var resumePositionMs = 0L
    private var undoQueue: QueueState? = null
    private val observation = scope.launch {
        engine.state.collect { snapshot ->
            if (!loaded) return@collect
            resumePositionMs = snapshot.positionMs
            mutableState.update { it.copy(engine = snapshot) }
            when {
                snapshot.error == EngineFailure.EXPIRED_STREAM && wantsPlayback && refreshes == 0 -> {
                    refreshes++
                    resolve(snapshot.positionMs)
                }
                snapshot.ended && !endedHandled && wantsPlayback -> {
                    endedHandled = true
                    state.value.queue.nextId(automatic = true)?.let { activate(it, true) }
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
            QueueState(shuffled, shuffled.first().id, old.repeat, true, entries.map { it.id })
        } else QueueState(entries, entries.getOrNull(index.coerceIn(0, entries.lastIndex.coerceAtLeast(0)))?.id,
            repeat = old.repeat).shuffle(startShuffled ?: old.shuffled)
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
        loaded = false
        wantsPlayback = false
        engine.stop()
        resumePositionMs = snapshot.positionMs
        mutableState.value = PlaybackState(track = snapshot.queue.current?.track, queue = snapshot.queue, passId = snapshot.passId,
            engine = EngineSnapshot(positionMs = resumePositionMs, durationMs = snapshot.queue.current?.track?.durationMs ?: 0))
    }

    fun setStorageError(failed: Boolean) { mutableState.update { it.copy(storageError = failed) } }
    fun setHistoryError(failed: Boolean) { mutableState.update { it.copy(historyError = failed) } }
    fun setConnected(ready: Boolean) { mutableState.update { it.copy(connected = ready) } }

    private fun activate(id: String, play: Boolean, position: Long = 0) {
        val queue = state.value.queue
        if (queue.entries.none { it.id == id }) return
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
        if (play) resolve(resumePositionMs)
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
                mutableState.update { current ->
                    current.copy(track = stream.track, resolving = false, queue = current.queue.copy(
                        entries = current.queue.entries.map { if (it.id == current.queue.currentId) it.copy(track = stream.track) else it }))
                }
                engine.load(stream, positionMs)
                loaded = true
                if (wantsPlayback) engine.play()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (request == generation) mutableState.update { it.copy(resolving = false,
                    sourceError = (error as? SourceException)?.failure ?: SourceFailure.EXTRACTION,
                    engine = it.engine.copy(playRequested = false)) }
            }
        }
    }

    fun play() {
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
        if (state.value.resolving) cancelResolution()
        engine.pause()
        mutableState.update { it.copy(resolving = false, engine = it.engine.copy(playing = false, playRequested = false)) }
    }

    override fun seekTo(positionMs: Long) {
        val current = state.value.engine
        if (current.seekable && current.durationMs > 0) engine.seekTo(positionMs.coerceIn(0, current.durationMs))
    }

    override fun next() { state.value.queue.nextId(automatic = false)?.let { activate(it, wantsPlayback) } }
    override fun previous() {
        val queue = state.value.queue
        if (state.value.engine.positionMs > 3_000) {
            if (loaded) engine.seekTo(0) else activate(queue.currentId ?: return, wantsPlayback)
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
    override fun setRepeat(mode: RepeatMode) { mutableState.update { it.copy(queue = it.queue.copy(repeat = mode)) } }
    override fun setShuffle(enabled: Boolean) { resetUndo(); editQueue(state.value.queue.shuffle(enabled)) }

    private fun editQueue(queue: QueueState) {
        val previous = state.value.queue.currentId
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
        cancelResolution()
        wantsPlayback = false
        loaded = false
        resumePositionMs = 0
        engine.stop()
        mutableState.update { it.copy(resolving = false, sourceError = null, engine = EngineSnapshot(), passId = null) }
    }
    fun release() { cancelResolution(); observation.cancel(); engine.release() }
}
