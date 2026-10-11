package com.karthi.sungmobile.core.playback

import com.karthi.sungmobile.core.model.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
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

    @Test fun networkErrorDoesNotRetryAndSeekingIsClamped() = runTest {
        val resolver = DeferredResolver()
        val engine = FakeEngine()
        val player = PlaybackCoordinator(resolver, engine, backgroundScope)
        player.select(track("one")); runCurrent()
        resolver.requests[0].complete(stream("one")); runCurrent()
        player.seekTo(500_000); runCurrent()
        assertEquals(100_000, engine.state.value.positionMs)
        player.seekTo(-1); runCurrent()
        assertEquals(0, engine.state.value.positionMs)
        engine.state.value = EngineSnapshot(error = EngineFailure.NETWORK); runCurrent()
        assertEquals(1, resolver.requests.size)
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

    private class DeferredResolver : StreamResolver {
        val requests = mutableListOf<CompletableDeferred<ResolvedStream>>()
        override suspend fun resolve(track: CatalogItem) = CompletableDeferred<ResolvedStream>()
            .also { requests += it }.await()
    }

    private class FakeEngine : PlayerEngine {
        override val state = MutableStateFlow(EngineSnapshot())
        val loaded = mutableListOf<String>()
        override fun load(stream: ResolvedStream, positionMs: Long) {
            loaded += stream.track.id
            state.value = EngineSnapshot(positionMs = positionMs, durationMs = 100_000, seekable = true)
        }
        override fun play() { state.value = state.value.copy(playing = true, playRequested = true) }
        override fun pause() { state.value = state.value.copy(playing = false, playRequested = false) }
        override fun seekTo(positionMs: Long) { state.value = state.value.copy(positionMs = positionMs, ended = false) }
        override fun stop() { state.value = EngineSnapshot() }
        override fun release() { stop() }
    }

    private fun track(id: String) = CatalogItem(id, CatalogKind.SONG, id, "Artist", "https://example.test/$id")
    private fun stream(id: String) = ResolvedStream(track(id), "https://example.test/audio", "audio/webm", emptyMap(), 128, null)
}
