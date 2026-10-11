package com.karthi.sungmobile.core.playback

import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.karthi.sungmobile.core.model.*
import com.karthi.sungmobile.core.playback.session.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(ExperimentalCoroutinesApi::class)
@androidx.annotation.OptIn(UnstableApi::class)
class SessionPlayerTest {
    private val songs = listOf("a", "b").map { CatalogItem(it, CatalogKind.SONG, "Title $it", "Artist $it", "https://www.youtube.com/watch?v=$it", durationMs = 100_000) }
    private class Engine : PlayerEngine {
        override val state = MutableStateFlow(EngineSnapshot())
        override fun load(stream: ResolvedStream, positionMs: Long) { state.value = EngineSnapshot(ready = true, durationMs = 100_000, positionMs = positionMs, seekable = true) }
        override fun play() { state.value = state.value.copy(playing = true, playRequested = true) }
        override fun pause() { state.value = state.value.copy(playing = false, playRequested = false) }
        override fun seekTo(positionMs: Long) { state.value = state.value.copy(positionMs = positionMs) }
        override fun stop() { state.value = EngineSnapshot() }
        override fun release() = Unit
    }
    private val resolver = object : StreamResolver {
        override suspend fun resolve(track: CatalogItem) = ResolvedStream(track, "https://private.example/ephemeral", null, emptyMap(), 128, null)
    }
    @Test fun mediaButtonsControlTheSameQueueAndMetadata() = runTest {
        val coordinator = PlaybackCoordinator(resolver, Engine(), backgroundScope)
        val session = SessionPlayer(coordinator, backgroundScope)
        coordinator.select(songs, 0); runCurrent()
        assertTrue(session.isPlaying)
        assertEquals("Title a", session.mediaMetadata.title)
        assertEquals(2, session.mediaItemCount)
        assertNull(session.currentMediaItem?.localConfiguration)
        session.seekToNext(); runCurrent()
        assertEquals("b", coordinator.state.value.track?.id)
        session.pause(); runCurrent()
        assertFalse(coordinator.state.value.engine.playRequested)
        session.seekTo(12_000); runCurrent()
        assertEquals(12_000, coordinator.state.value.engine.positionMs)
        session.play(); runCurrent()
        assertTrue(session.isPlaying)
        session.release(); coordinator.release()
    }
    @Test fun repeatMappingAndShuffleAreConsistentAcrossSession() = runTest {
        val coordinator = PlaybackCoordinator(resolver, Engine(), backgroundScope)
        val session = SessionPlayer(coordinator, backgroundScope)
        coordinator.select(songs, 0); runCurrent()
        session.repeatMode = Player.REPEAT_MODE_ONE; runCurrent()
        assertEquals(RepeatMode.ONE, coordinator.state.value.queue.repeat)
        assertEquals(Player.REPEAT_MODE_ONE, session.repeatMode)
        session.repeatMode = Player.REPEAT_MODE_ALL; runCurrent()
        assertEquals(RepeatMode.ALL, coordinator.state.value.queue.repeat)
        assertEquals(Player.REPEAT_MODE_ALL, session.repeatMode)
        session.shuffleModeEnabled = true; runCurrent()
        assertTrue(coordinator.state.value.queue.shuffled)
        session.release(); coordinator.release()
    }
    @Test fun standardPlaylistCommandsKeepDuplicateInstancesDistinct() = runTest {
        val coordinator = PlaybackCoordinator(resolver, Engine(), backgroundScope)
        val session = SessionPlayer(coordinator, backgroundScope)
        session.setMediaItems(listOf(QueueEntry("first", songs[0]).toMediaItem(), QueueEntry("second", songs[0]).toMediaItem()))
        session.play(); runCurrent()
        session.removeMediaItem(0); runCurrent()
        assertEquals("second", coordinator.state.value.queue.currentId)
        assertEquals("second", session.currentMediaItem?.mediaId)
        assertEquals(1, session.mediaItemCount)
        session.release(); coordinator.release()
    }
    @Test fun resolvingCanBePausedFromNotificationWithoutLatePlayback() = runTest {
        val stream = CompletableDeferred<ResolvedStream>()
        val resolver = object : StreamResolver { override suspend fun resolve(track: CatalogItem) = stream.await() }
        val coordinator = PlaybackCoordinator(resolver, Engine(), backgroundScope)
        val session = SessionPlayer(coordinator, backgroundScope)
        coordinator.select(songs, 0); runCurrent()
        assertEquals(Player.STATE_BUFFERING, session.playbackState)
        assertTrue(session.playWhenReady)
        session.pause(); runCurrent()
        stream.complete(this@SessionPlayerTest.resolver.resolve(songs[0])); runCurrent()
        assertFalse(session.playWhenReady)
        assertFalse(session.isPlaying)
        session.release(); coordinator.release()
    }
    @Test fun errorCanBeRetriedThroughStandardPlayCommand() = runTest {
        var failed = false
        val resolver = object : StreamResolver {
            override suspend fun resolve(track: CatalogItem): ResolvedStream {
                if (!failed) { failed = true; throw SourceException(SourceFailure.NETWORK) }
                return this@SessionPlayerTest.resolver.resolve(track)
            }
        }
        val coordinator = PlaybackCoordinator(resolver, Engine(), backgroundScope)
        val session = SessionPlayer(coordinator, backgroundScope)
        coordinator.select(songs, 0); runCurrent()
        assertNotNull(session.playerError)
        session.prepare(); session.play(); runCurrent()
        assertNull(session.playerError)
        assertTrue(session.isPlaying)
        session.release(); coordinator.release()
    }
}
