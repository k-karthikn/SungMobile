package com.karthi.sungmobile.core.playback

import com.karthi.sungmobile.core.model.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SleepTimerControllerTest {
    @Test fun durationReplacementAndCancellationPauseOnlyAtLatestDeadline() = runTest {
        val engine = Engine()
        val playback = PlaybackCoordinator(resolver, engine, backgroundScope)
        val timer = SleepTimerController(playback, backgroundScope) { testScheduler.currentTime }
        playback.select(track("one")); runCurrent()
        timer.arm(SleepMode.DURATION, 1)
        advanceTimeBy(30_000); runCurrent()
        timer.arm(SleepMode.DURATION, 1)
        advanceTimeBy(31_000); runCurrent()
        assertTrue(engine.state.value.playing)
        advanceTimeBy(29_000); runCurrent()
        assertFalse(engine.state.value.playing)
        assertNull(playback.state.value.sleepTimer)
        timer.arm(SleepMode.DURATION, 1)
        timer.cancel()
        advanceTimeBy(60_000); runCurrent()
        assertNull(playback.state.value.sleepTimer)
    }

    @Test fun endTrackStopsBeforeAutomaticNextAndManualNextCancelsIt() = runTest {
        val engine = Engine()
        val playback = PlaybackCoordinator(resolver, engine, backgroundScope)
        val timer = SleepTimerController(playback, backgroundScope) { testScheduler.currentTime }
        playback.select(listOf(track("one"), track("two")), 0); runCurrent()
        timer.arm(SleepMode.END_TRACK)
        engine.state.value = engine.state.value.copy(ended = true, playing = false)
        runCurrent()
        assertEquals("one", playback.state.value.track?.id)
        assertFalse(playback.state.value.engine.playRequested)
        assertNull(playback.state.value.sleepTimer)
        playback.next(); runCurrent()
        timer.arm(SleepMode.END_TRACK)
        playback.previous(); runCurrent()
        timer.onPlaybackChanged()
        assertNull(playback.state.value.sleepTimer)
    }

    @Test fun endQueueWaitsForLastNaturalCompletionAndFadeResetsOnTrackChange() = runTest {
        val engine = Engine()
        val playback = PlaybackCoordinator(resolver, engine, backgroundScope)
        val timer = SleepTimerController(playback, backgroundScope) { testScheduler.currentTime }
        playback.select(listOf(track("one"), track("two")), 0); runCurrent()
        timer.arm(SleepMode.END_QUEUE)
        engine.state.value = engine.state.value.copy(ended = true, playing = false); runCurrent()
        assertEquals("two", playback.state.value.track?.id)
        assertNotNull(playback.state.value.sleepTimer)
        engine.state.value = engine.state.value.copy(ended = true, playing = false); runCurrent()
        assertFalse(playback.state.value.engine.playRequested)
        assertNull(playback.state.value.sleepTimer)

        playback.select(listOf(track("one"), track("two")), 0); runCurrent()
        timer.arm(SleepMode.DURATION, 1, fade = true)
        advanceTimeBy(55_000); runCurrent()
        assertTrue(engine.outputVolume < 1f)
        playback.next(); runCurrent()
        timer.onPlaybackChanged()
        assertEquals(1f, engine.outputVolume)
        assertFalse(playback.state.value.sleepTimer!!.fade)
    }

    private val resolver = object : StreamResolver {
        override suspend fun resolve(track: CatalogItem) = ResolvedStream(track,
            "https://example.test/audio", "audio/webm", emptyMap(), 128, null)
    }
    private fun track(id: String) = CatalogItem(id, CatalogKind.SONG, id, "Artist",
        "https://www.youtube.com/watch?v=dQw4w9WgXcQ", durationMs = 100_000)
    private class Engine : PlayerEngine {
        override val state = MutableStateFlow(EngineSnapshot())
        var outputVolume = 1f
        override fun load(stream: ResolvedStream, positionMs: Long) {
            state.value = EngineSnapshot(positionMs = positionMs, durationMs = 100_000, seekable = true)
        }
        override fun play() { state.value = state.value.copy(playing = true, playRequested = true) }
        override fun pause() { state.value = state.value.copy(playing = false, playRequested = false) }
        override fun seekTo(positionMs: Long) { state.value = state.value.copy(positionMs = positionMs) }
        override fun stop() { state.value = EngineSnapshot() }
        override fun release() {}
        override fun setVolume(volume: Float) { outputVolume = volume }
    }
}
