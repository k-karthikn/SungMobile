package com.karthi.sungmobile.core.playback

import com.karthi.sungmobile.core.model.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceTimeBy
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackCoordinatorTest {
    @Test fun replacingTrackCancelsOldResolution() = runTest {
        val resolver = DeferredResolver()
        val engine = FakeEngine()
        val player = PlaybackCoordinator(resolver, engine, backgroundScope)
        player.select(track("old")); runCurrent()
        player.select(track("new")); runCurrent()
        resolver.requests[0].complete(stream("old"))
        resolver.requests[1].complete(stream("new")); runCurrent()
        assertEquals(listOf("new"), engine.loaded)
        assertTrue(engine.state.value.playing)
    }

    @Test fun pauseDuringResolutionPreventsLateAutoplay() = runTest {
        val resolver = DeferredResolver()
        val engine = FakeEngine()
        val player = PlaybackCoordinator(resolver, engine, backgroundScope)
        player.select(track("one")); runCurrent()
        player.pause()
        resolver.requests.single().complete(stream("one")); runCurrent()
        assertTrue(engine.loaded.isEmpty())
        assertFalse(player.state.value.resolving)
        player.toggle(); runCurrent()
        resolver.requests.last().complete(stream("one")); runCurrent()
        assertTrue(engine.state.value.playing)
    }

    @Test fun expiredHttpResponseRefreshesOnlyOnceAndPreservesPosition() = runTest {
        val resolver = DeferredResolver()
        val engine = FakeEngine()
        val player = PlaybackCoordinator(resolver, engine, backgroundScope)
        player.select(track("one")); runCurrent()
        resolver.requests[0].complete(stream("one")); runCurrent()
        engine.state.value = EngineSnapshot(positionMs = 22_000, error = EngineFailure.EXPIRED_STREAM)
        runCurrent()
        assertEquals(2, resolver.requests.size)
        resolver.requests[1].complete(stream("one")); runCurrent()
        assertEquals(22_000, engine.state.value.positionMs)
        engine.state.value = EngineSnapshot(positionMs = 22_000, error = EngineFailure.EXPIRED_STREAM)
        runCurrent()
        assertEquals(2, resolver.requests.size)
        assertEquals(EngineFailure.EXPIRED_STREAM, player.state.value.engine.error)
    }

    @Test fun stopDuringRecoveryCannotRestartPlayback() = runTest {
        val resolver = DeferredResolver()
        val engine = FakeEngine()
        val player = PlaybackCoordinator(resolver, engine, backgroundScope)
        player.select(track("one")); runCurrent()
        resolver.requests[0].complete(stream("one")); runCurrent()
        engine.state.value = EngineSnapshot(error = EngineFailure.EXPIRED_STREAM); runCurrent()
        player.stop()
        resolver.requests[1].complete(stream("one")); runCurrent()
        assertEquals("one", player.state.value.track?.id)
        assertFalse(player.state.value.engine.playRequested)
        assertFalse(engine.state.value.playing)
        assertEquals(1, engine.loaded.size)
    }

    @Test fun networkErrorRetriesTwiceFromSamePositionThenShowsError() = runTest {
        val resolver = DeferredResolver()
        val engine = FakeEngine()
        val player = PlaybackCoordinator(resolver, engine, backgroundScope)
        player.select(track("one")); runCurrent()
        resolver.requests[0].complete(stream("one")); runCurrent()
        player.seekTo(500_000); runCurrent()
        assertEquals(100_000, engine.state.value.positionMs)
        player.seekTo(-1); runCurrent()
        assertEquals(0, engine.state.value.positionMs)
        engine.state.value = EngineSnapshot(positionMs = 22_000, error = EngineFailure.NETWORK); runCurrent()
        assertTrue(player.state.value.resolving)
        assertEquals(1, resolver.requests.size)
        advanceTimeBy(2_000); runCurrent()
        assertEquals(2, resolver.requests.size)
        resolver.requests[1].complete(stream("one")); runCurrent()
        assertEquals(22_000, engine.state.value.positionMs)
        engine.state.value = EngineSnapshot(positionMs = 23_000, error = EngineFailure.NETWORK); runCurrent()
        advanceTimeBy(6_000); runCurrent()
        assertEquals(3, resolver.requests.size)
        resolver.requests[2].complete(stream("one")); runCurrent()
        engine.state.value = EngineSnapshot(positionMs = 24_000, error = EngineFailure.NETWORK); runCurrent()
        advanceTimeBy(10_000); runCurrent()
        assertEquals(3, resolver.requests.size)
        assertEquals(EngineFailure.NETWORK, player.state.value.engine.error)
    }

    @Test fun longBufferStallRetriesButPauseCancelsPendingRecovery() = runTest {
        val resolver = DeferredResolver()
        val engine = FakeEngine()
        val player = PlaybackCoordinator(resolver, engine, backgroundScope)
        player.select(track("one")); runCurrent()
        resolver.requests[0].complete(stream("one")); runCurrent()
        engine.state.value = EngineSnapshot(positionMs = 31_000, buffering = true, playRequested = true)
        runCurrent()
        advanceTimeBy(45_000); runCurrent()
        assertTrue(player.state.value.resolving)
        player.pause()
        advanceTimeBy(10_000); runCurrent()
        assertEquals(1, resolver.requests.size)
        assertFalse(player.state.value.engine.playRequested)
    }

    @Test fun networkRecoveryResumesAfterBufferStallAndIgnoresLocalFiles() = runTest {
        val resolver = DeferredResolver()
        val engine = FakeEngine()
        val player = PlaybackCoordinator(resolver, engine, backgroundScope)
        player.select(track("one")); runCurrent()
        resolver.requests[0].complete(stream("one")); runCurrent()
        engine.state.value = EngineSnapshot(positionMs = 31_000, buffering = true, playRequested = true)
        runCurrent()
        advanceTimeBy(45_000); runCurrent()
        advanceTimeBy(2_000); runCurrent()
        assertEquals(2, resolver.requests.size)
        resolver.requests[1].complete(stream("one")); runCurrent()
        assertEquals(31_000, engine.state.value.positionMs)

        player.select(track("local:one")); runCurrent()
        resolver.requests[2].complete(stream("local:one")); runCurrent()
        engine.state.value = EngineSnapshot(positionMs = 2_000, buffering = true, playRequested = true)
        runCurrent()
        advanceTimeBy(60_000); runCurrent()
        assertEquals(3, resolver.requests.size)
    }

    @Test fun choosingAnotherTrackCancelsNetworkRecovery() = runTest {
        val resolver = DeferredResolver()
        val engine = FakeEngine()
        val player = PlaybackCoordinator(resolver, engine, backgroundScope)
        player.select(track("one")); runCurrent()
        resolver.requests[0].complete(stream("one")); runCurrent()
        engine.state.value = EngineSnapshot(positionMs = 15_000, error = EngineFailure.NETWORK)
        runCurrent()
        player.select(track("two")); runCurrent()
        resolver.requests[1].complete(stream("two")); runCurrent()
        advanceTimeBy(10_000); runCurrent()
        assertEquals(2, resolver.requests.size)
        assertEquals("two", player.state.value.track?.id)
        assertTrue(engine.state.value.playing)
    }

    @Test fun expiredResolvedUrlIsNeverLoaded() = runTest {
        val resolver = DeferredResolver()
        val engine = FakeEngine()
        val player = PlaybackCoordinator(resolver, engine, backgroundScope, now = { 5_000 })
        player.select(track("one")); runCurrent()
        resolver.requests.single().complete(ResolvedStream(track("one"), "https://example.test", null,
            emptyMap(), 128, 10_000)); runCurrent()
        assertTrue(engine.loaded.isEmpty())
        assertEquals(SourceFailure.UNAVAILABLE, player.state.value.sourceError)
    }

    @Test fun pausedBufferingCanResumeAndFinishedTrackCanReplay() = runTest {
        val resolver = DeferredResolver()
        val engine = FakeEngine()
        val player = PlaybackCoordinator(resolver, engine, backgroundScope)
        player.select(track("one")); runCurrent()
        resolver.requests.single().complete(stream("one")); runCurrent()
        engine.state.value = engine.state.value.copy(playing = false, buffering = true)
        runCurrent()
        player.toggle(); runCurrent()
        assertFalse(engine.state.value.playRequested)
        player.toggle(); runCurrent()
        assertTrue(engine.state.value.playRequested)
        engine.state.value = engine.state.value.copy(playing = false, buffering = false,
            ended = true, positionMs = 100_000)
        runCurrent()
        player.toggle(); runCurrent()
        assertEquals(0, engine.state.value.positionMs)
        assertTrue(engine.state.value.playing)
        assertEquals(1, resolver.requests.size)
    }

    @Test fun naturalEndUsesPreparedSuccessorWithoutSecondResolution() = runTest {
        val resolver = DeferredResolver()
        val engine = FakeEngine()
        val player = PlaybackCoordinator(resolver, engine, backgroundScope)
        player.select(listOf(track("one"), track("two")), 0); runCurrent()
        resolver.requests[0].complete(stream("one")); runCurrent()
        engine.state.value = engine.state.value.copy(positionMs = 60_000); runCurrent()
        assertEquals(2, resolver.requests.size)
        resolver.requests[1].complete(stream("two")); runCurrent()
        engine.state.value = engine.state.value.copy(ended = true, playing = false, positionMs = 100_000); runCurrent()
        assertEquals(listOf("one", "two"), engine.loaded)
        assertEquals(2, resolver.requests.size)
    }

    @Test fun queueEditDiscardsPreparedSuccessor() = runTest {
        val resolver = DeferredResolver()
        val engine = FakeEngine()
        val player = PlaybackCoordinator(resolver, engine, backgroundScope)
        player.select(listOf(track("one"), track("two"), track("three")), 0); runCurrent()
        resolver.requests[0].complete(stream("one")); runCurrent()
        engine.state.value = engine.state.value.copy(positionMs = 60_000); runCurrent()
        resolver.requests[1].complete(stream("two")); runCurrent()
        player.remove(player.state.value.queue.entries[1].id); runCurrent()
        engine.state.value = engine.state.value.copy(ended = true, playing = false, positionMs = 100_000); runCurrent()
        assertEquals("three", player.state.value.track?.id)
        assertEquals(3, resolver.requests.size)
        resolver.requests[2].complete(stream("three")); runCurrent()
        assertEquals(listOf("one", "three"), engine.loaded)
    }

    @Test fun trimAndSleepFadeShareOneGain() = runTest {
        val engine = FakeEngine()
        val player = PlaybackCoordinator(DeferredResolver(), engine, backgroundScope)
        player.setTrimDb(-6)
        player.setFadeVolume(.5f)
        assertEquals(.25f, engine.gain, .01f)
        player.setFadeVolume(1f)
        assertEquals(.5f, engine.gain, .01f)
    }

    @Test fun savedTrimIsAppliedBeforeEachTrackLoads() = runTest {
        val resolver = DeferredResolver()
        val engine = FakeEngine()
        val player = PlaybackCoordinator(resolver, engine, backgroundScope)
        val first = CatalogItem("first", CatalogKind.SONG, "First", "Artist",
            "https://www.youtube.com/watch?v=AAAAAAAAAAA")
        val second = CatalogItem("second", CatalogKind.SONG, "Second", "Artist",
            "https://www.youtube.com/watch?v=BBBBBBBBBBB")
        player.setTrackTrims(mapOf("AAAAAAAAAAA" to -6, "BBBBBBBBBBB" to -12))
        player.select(listOf(first, second), 0); runCurrent()
        resolver.requests[0].complete(ResolvedStream(first, "https://example.test/a", null, emptyMap(), 128, null))
        runCurrent()
        player.next(); runCurrent()
        resolver.requests[1].complete(ResolvedStream(second, "https://example.test/b", null, emptyMap(), 128, null))
        runCurrent()
        assertEquals(.5f, engine.gainAtLoad[0], .01f)
        assertEquals(.25f, engine.gainAtLoad[1], .01f)
    }

    @Test fun relatedAutoplayAddsOnlyNewSongsAtNaturalQueueEnd() = runTest {
        val resolver = DeferredResolver()
        val engine = FakeEngine()
        val related = object : RelatedSource {
            var requests = 0
            override suspend fun related(seed: CatalogItem, limit: Int): List<CatalogItem> {
                requests++
                return listOf(track("one"), track("two"), track("two"), track("three"))
            }
        }
        val player = PlaybackCoordinator(resolver, engine, backgroundScope, relatedSource = related)
        player.setRelatedAutoplayEnabled(true)
        player.select(track("one")); runCurrent()
        resolver.requests[0].complete(stream("one")); runCurrent()
        engine.state.value = engine.state.value.copy(ended = true, playing = false); runCurrent()
        assertEquals(1, related.requests)
        assertEquals(listOf("one", "two", "three"), player.state.value.queue.entries.map { it.track.id })
        assertEquals("two", player.state.value.track?.id)
        resolver.requests[1].complete(stream("two")); runCurrent()
        assertEquals(listOf("one", "two"), engine.loaded)
    }

    @Test fun endOfQueueTimerWinsOverRelatedAutoplay() = runTest {
        var requests = 0
        val related = object : RelatedSource {
            override suspend fun related(seed: CatalogItem, limit: Int): List<CatalogItem> {
                requests++
                return listOf(track("two"))
            }
        }
        val resolver = DeferredResolver()
        val engine = FakeEngine()
        val player = PlaybackCoordinator(resolver, engine, backgroundScope, relatedSource = related)
        player.setRelatedAutoplayEnabled(true)
        player.onNaturalEnd = { true }
        player.select(track("one")); runCurrent()
        resolver.requests[0].complete(stream("one")); runCurrent()
        engine.state.value = engine.state.value.copy(ended = true, playing = false); runCurrent()
        assertEquals(0, requests)
        assertFalse(player.state.value.engine.playRequested)
    }

    private class DeferredResolver : StreamResolver {
        val requests = mutableListOf<CompletableDeferred<ResolvedStream>>()
        override suspend fun resolve(track: CatalogItem) = CompletableDeferred<ResolvedStream>()
            .also { requests += it }.await()
    }

    private class FakeEngine : PlayerEngine {
        override val state = MutableStateFlow(EngineSnapshot())
        val loaded = mutableListOf<String>()
        val gainAtLoad = mutableListOf<Float>()
        var gain = 1f
        override fun load(stream: ResolvedStream, positionMs: Long) {
            loaded += stream.track.id
            gainAtLoad += gain
            state.value = EngineSnapshot(positionMs = positionMs, durationMs = 100_000, seekable = true)
        }
        override fun play() { state.value = state.value.copy(playing = true, playRequested = true) }
        override fun pause() { state.value = state.value.copy(playing = false, playRequested = false) }
        override fun seekTo(positionMs: Long) { state.value = state.value.copy(positionMs = positionMs, ended = false) }
        override fun stop() { state.value = EngineSnapshot() }
        override fun setVolume(volume: Float) { gain = volume }
        override fun release() { stop() }
    }

    private fun track(id: String) = CatalogItem(id, CatalogKind.SONG, id, "Artist", "https://example.test/$id")
    private fun stream(id: String) = ResolvedStream(track(id), "https://example.test/audio", "audio/webm", emptyMap(), 128, null)
}
