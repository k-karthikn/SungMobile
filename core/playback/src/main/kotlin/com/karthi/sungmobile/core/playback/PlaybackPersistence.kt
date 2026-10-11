package com.karthi.sungmobile.core.playback

import com.karthi.sungmobile.core.model.QueueSnapshot
import com.karthi.sungmobile.core.model.QueueStore
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

/** Serial, conflated disk writes; position checkpoints at most once per five-second bucket. */
class PlaybackPersistence(
    private val player: PlaybackCoordinator,
    private val store: QueueStore,
    private val serviceScope: CoroutineScope,
    private val writerScope: CoroutineScope,
) {
    private val writes = Channel<QueueSnapshot>(Channel.CONFLATED)
    private var observer: Job? = null
    private var restored = false
    private var writable = false
    private val writer = writerScope.launch {
        for (snapshot in writes) {
            try { store.write(snapshot); player.setStorageError(false) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { player.setStorageError(true) }
        }
    }

    suspend fun restore() {
        player.setConnected(false)
        var readFailed = false
        try { player.restore(store.read()) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { readFailed = true; player.setStorageError(true) }
        restored = true
        writable = !readFailed
        player.setConnected(true)
        observer = serviceScope.launch {
            player.state.distinctUntilChangedBy {
                listOf(it.queue, it.engine.positionMs / 5_000, it.engine.playRequested, it.engine.ended)
            }.drop(if (readFailed) 1 else 0).collect {
                writable = true
                writes.send(QueueSnapshot(it.queue, it.engine.positionMs))
            }
        }
    }

    /** The application writer drains after the service scope is cancelled. Never blocks Main. */
    fun close(): Job {
        observer?.cancel()
        if (restored && writable) writes.trySend(player.state.value.let { QueueSnapshot(it.queue, it.engine.positionMs) })
        writes.close()
        return writer
    }
}
