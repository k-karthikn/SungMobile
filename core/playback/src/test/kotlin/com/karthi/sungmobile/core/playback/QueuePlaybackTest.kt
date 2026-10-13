package com.karthi.sungmobile.core.playback

import com.karthi.sungmobile.core.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QueuePlaybackTest {
    private val songs = listOf("one", "two", "three").map { CatalogItem(it, CatalogKind.SONG, it, "Artist", "https://www.youtube.com/watch?v=$it") }
    private class Engine : PlayerEngine {
        override val state = MutableStateFlow(EngineSnapshot())
        var loads = 0
        override fun load(stream: ResolvedStream, positionMs: Long) { loads++; state.value = EngineSnapshot(ready = true, positionMs = positionMs, durationMs = 100_000, seekable = true) }
        override fun play() { state.value = state.value.copy(playing = true, playRequested = true) }
        override fun pause() { state.value = state.value.copy(playing = false, playRequested = false) }
        override fun seekTo(positionMs: Long) { state.value = state.value.copy(positionMs = positionMs, ended = false) }
        override fun stop() { state.value = EngineSnapshot() }
        override fun release() = stop()
        fun end() { state.value = state.value.copy(playing = false, ended = true, ready = false, positionMs = 100_000) }
    }
    private val resolver = object : StreamResolver {
        override suspend fun resolve(track: CatalogItem) = ResolvedStream(track, "https://example.test/audio", null, emptyMap(), 128, null)
    }

    @Test fun collectionPlaybackAndBulkEnqueueKeepQueueOperationsAtomic() = runTest {
        val engine = Engine(); val player = PlaybackCoordinator(resolver, engine, backgroundScope)
        player.playCollection(songs, shuffle = true); runCurrent()
        assertTrue(player.state.value.queue.shuffled)
        assertEquals(songs.map { it.id }.toSet(), player.state.value.queue.entries.map { it.track.id }.toSet())
        val current = player.state.value.queue.currentId
        player.enqueueAll(listOf(songs[1], songs[2]), next = true)
        assertEquals(current, player.state.value.queue.currentId)
        assertEquals(listOf("two", "three"), player.state.value.queue.entries
            .drop(player.state.value.queue.currentIndex + 1).take(2).map { it.track.id })
        assertEquals(1, engine.loads)
    }

    @Test fun completionAdvancesOnceAndQueueEndStops() = runTest {
        val engine = Engine(); val player = PlaybackCoordinator(resolver, engine, backgroundScope)
        player.select(songs, 0); runCurrent()
        engine.end(); runCurrent()
        assertEquals("two", player.state.value.track?.id)
        assertTrue(player.state.value.engine.playing)
        assertEquals(2, engine.loads)
        engine.end(); runCurrent(); engine.end(); runCurrent()
        assertEquals("three", player.state.value.track?.id)
        assertTrue(player.state.value.engine.ended)
        assertEquals(3, engine.loads)
    }
    @Test fun repeatOneReplaysCurrentWhileManualNextAdvances() = runTest {
        val engine = Engine(); val player = PlaybackCoordinator(resolver, engine, backgroundScope)
        player.select(songs, 0); player.setRepeat(RepeatMode.ONE); runCurrent()
        engine.end(); runCurrent()
        assertEquals("one", player.state.value.track?.id)
        assertEquals(2, engine.loads)
        player.next(); runCurrent()
        assertEquals("two", player.state.value.track?.id)
    }
    @Test fun repeatAllWrapsAtTheEnd() = runTest {
        val engine = Engine(); val player = PlaybackCoordinator(resolver, engine, backgroundScope)
        player.select(songs, 2); player.setRepeat(RepeatMode.ALL); runCurrent()
        engine.end(); runCurrent()
        assertEquals("one", player.state.value.track?.id)
    }
    @Test fun restorationRetainsPositionButDoesNotResolveUntilPlay() = runTest {
        val engine = Engine(); val player = PlaybackCoordinator(resolver, engine, backgroundScope)
        val entries = songs.map { QueueEntry(track = it) }
        player.restore(QueueSnapshot(QueueState(entries, entries[1].id, RepeatMode.ALL), 23_000)); runCurrent()
        assertEquals(0, engine.loads)
        assertFalse(player.state.value.engine.playRequested)
        assertEquals(23_000, player.state.value.engine.positionMs)
        player.play(); runCurrent()
        assertEquals(23_000, engine.state.value.positionMs)
        assertEquals("two", player.state.value.track?.id)
    }
    @Test fun removingUpcomingDoesNotReloadAndUndoDoesNotRestartRemovedCurrent() = runTest {
        val engine = Engine(); val player = PlaybackCoordinator(resolver, engine, backgroundScope)
        player.select(songs, 0); runCurrent()
        val original = player.state.value.queue
        player.remove(original.entries[2].id); runCurrent()
        assertEquals(1, engine.loads)
        player.undo(); runCurrent()
        assertEquals(original.entries, player.state.value.queue.entries)
        player.remove(original.currentId!!); runCurrent()
        assertEquals("two", player.state.value.track?.id)
        player.undo(); runCurrent()
        assertEquals("two", player.state.value.track?.id)
        assertEquals(2, engine.loads)
        assertEquals(original.entries, player.state.value.queue.entries)
    }
    @Test fun removeCurrentWhilePausedStaysPaused() = runTest {
        val engine = Engine(); val player = PlaybackCoordinator(resolver, engine, backgroundScope)
        player.select(songs, 0); runCurrent(); player.pause(); runCurrent()
        player.remove(player.state.value.queue.currentId!!); runCurrent()
        assertEquals("two", player.state.value.track?.id)
        assertFalse(engine.state.value.playing)
        assertEquals(1, engine.loads)
    }
    @Test fun removeAllAndUndoRestoresPaused() = runTest {
        val engine = Engine(); val player = PlaybackCoordinator(resolver, engine, backgroundScope)
        player.select(songs, 0); runCurrent()
        player.removeIds(player.state.value.queue.entries.map { it.id }.toSet()); runCurrent()
        assertNull(player.state.value.track)
        player.undo(); runCurrent()
        assertEquals(3, player.state.value.queue.entries.size)
        assertFalse(player.state.value.engine.playRequested)
        assertEquals(1, engine.loads)
    }
    @Test fun focusLossOrUnplugDoesNotRestartOnQueueEdit() = runTest {
        val engine = Engine(); val player = PlaybackCoordinator(resolver, engine, backgroundScope)
        player.select(songs, 0); runCurrent()
        engine.pause(); runCurrent()
        player.next(); runCurrent()
        assertFalse(engine.state.value.playing)
        assertEquals(1, engine.loads)
    }
    @Test fun networkFailureRetainsQueueAndManualRetryPosition() = runTest {
        val engine = Engine(); val player = PlaybackCoordinator(resolver, engine, backgroundScope)
        player.select(songs, 0); runCurrent()
        engine.state.value = EngineSnapshot(positionMs = 15_000, error = EngineFailure.NETWORK); runCurrent()
        assertEquals(1, engine.loads)
        assertEquals(3, player.state.value.queue.entries.size)
        player.play(); runCurrent()
        assertEquals(15_000, engine.state.value.positionMs)
        assertEquals(2, engine.loads)
    }
    @Test fun resolverRuntimeFailureBecomesAnActionableError() = runTest {
        val engine = Engine()
        val broken = object : StreamResolver { override suspend fun resolve(track: CatalogItem): ResolvedStream = throw IllegalStateException("provider bug") }
        val player = PlaybackCoordinator(broken, engine, backgroundScope)
        player.select(songs, 0); runCurrent()
        assertEquals(SourceFailure.EXTRACTION, player.state.value.sourceError)
        assertFalse(player.state.value.resolving)
        assertEquals(3, player.state.value.queue.entries.size)
    }
    @Test fun staleUncancellableResolutionCannotPlayAfterSkip() = runTest {
        val first = CompletableDeferred<ResolvedStream>()
        val source = object : StreamResolver {
            override suspend fun resolve(track: CatalogItem) = if (track.id == "one") withContext(NonCancellable) { first.await() } else resolver.resolve(track)
        }
        val engine = Engine(); val player = PlaybackCoordinator(source, engine, backgroundScope)
        player.select(songs, 0); runCurrent(); player.next(); runCurrent()
        first.complete(resolver.resolve(songs[0])); runCurrent()
        assertEquals("two", player.state.value.track?.id)
        assertEquals(1, engine.loads)
    }
}
