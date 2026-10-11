package com.karthi.sungmobile.core.playback

import com.karthi.sungmobile.core.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackPersistenceTest {
    private class Engine : PlayerEngine {
        override val state = MutableStateFlow(EngineSnapshot())
        override fun load(stream: ResolvedStream, positionMs: Long) = Unit
        override fun play() = Unit
        override fun pause() = Unit
        override fun seekTo(positionMs: Long) = Unit
        override fun stop() = Unit
        override fun release() = Unit
    }
    private val resolver = object : StreamResolver { override suspend fun resolve(track: CatalogItem): ResolvedStream = error("Restoration must not resolve") }
    @Test fun restoreDoesNotOverwriteSavedQueueWithInitialEmptyState() = runTest {
        val entry = QueueEntry(track = CatalogItem("a", CatalogKind.SONG, "Song", "Artist", "https://www.youtube.com/watch?v=a"))
        val saved = QueueSnapshot(QueueState(listOf(entry), entry.id), 32_000)
        val writes = mutableListOf<QueueSnapshot>()
        val store = object : QueueStore { override suspend fun read() = saved; override suspend fun write(snapshot: QueueSnapshot) { writes += snapshot } }
        val player = PlaybackCoordinator(resolver, Engine(), backgroundScope)
        val persistence = PlaybackPersistence(player, store, backgroundScope, backgroundScope)
        persistence.restore(); runCurrent()
        assertEquals(listOf(saved), writes)
        assertFalse(player.state.value.engine.playRequested)
        persistence.close(); runCurrent()
        assertEquals(saved, writes.last())
    }
    @Test fun writeFailureIsVisibleAndLaterSaveRecovers() = runTest {
        var fail = true
        val store = object : QueueStore {
            override suspend fun read() = QueueSnapshot()
            override suspend fun write(snapshot: QueueSnapshot) { if (fail) throw java.io.IOException("disk full") }
        }
        val player = PlaybackCoordinator(resolver, Engine(), backgroundScope)
        val persistence = PlaybackPersistence(player, store, backgroundScope, backgroundScope)
        persistence.restore(); runCurrent()
        assertTrue(player.state.value.storageError)
        fail = false
        player.setRepeat(RepeatMode.ALL); runCurrent()
        assertFalse(player.state.value.storageError)
        persistence.close(); runCurrent()
    }
    @Test fun failedReadDoesNotEraseSavedDataOnOpenOrClose() = runTest {
        val writes = mutableListOf<QueueSnapshot>()
        val store = object : QueueStore {
            override suspend fun read(): QueueSnapshot = throw java.io.IOException("unreadable")
            override suspend fun write(snapshot: QueueSnapshot) { writes += snapshot }
        }
        val player = PlaybackCoordinator(resolver, Engine(), backgroundScope)
        val persistence = PlaybackPersistence(player, store, backgroundScope, backgroundScope)
        persistence.restore(); runCurrent()
        assertTrue(player.state.value.storageError)
        persistence.close(); runCurrent()
        assertTrue(writes.isEmpty())
    }

}
