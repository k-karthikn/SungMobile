package com.karthi.sungmobile.playback

import android.os.Bundle
import android.os.Looper
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.*
import com.google.common.util.concurrent.SettableFuture
import com.karthi.sungmobile.core.model.*
import com.karthi.sungmobile.core.playback.*
import com.karthi.sungmobile.core.playback.session.*
import com.karthi.sungmobile.app.AppContainer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Real Media3 session/controller protocol, with only audio decoding and network replaced. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@androidx.annotation.OptIn(UnstableApi::class)
class MediaControllerIntegrationTest {
    private class Engine : PlayerEngine {
        override val state = MutableStateFlow(EngineSnapshot())
        override fun load(stream: ResolvedStream, positionMs: Long) { state.value = EngineSnapshot(ready = true, seekable = true, durationMs = 100_000, positionMs = positionMs) }
        override fun play() { state.value = state.value.copy(playing = true, playRequested = true) }
        override fun pause() { state.value = state.value.copy(playing = false, playRequested = false) }
        override fun seekTo(positionMs: Long) { state.value = state.value.copy(positionMs = positionMs) }
        override fun stop() { state.value = EngineSnapshot() }
        override fun release() = Unit
    }
    private val song = CatalogItem("a", CatalogKind.SONG, "Track", "Artist", "https://www.youtube.com/watch?v=dQw4w9WgXcQ", durationMs = 100_000)
    private val resolver = object : StreamResolver {
        override suspend fun resolve(track: CatalogItem) = ResolvedStream(track, "https://example.test/audio", null, emptyMap(), 128, null)
    }

    @Test fun controllerEditsQueueReceivesUndoAndDisconnectDoesNotStopAudio() {
        val context = RuntimeEnvironment.getApplication()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val engine = Engine()
        val coordinator = PlaybackCoordinator(resolver, engine, scope)
        val timer = SleepTimerController(coordinator, scope, android.os.SystemClock::elapsedRealtime)
        val container = AppContainer(context)
        val bridge = SessionPlayer(coordinator, scope)
        val ready = SettableFuture.create<Unit>().apply { set(Unit) }
        val session = MediaSession.Builder(context, bridge)
            .setCallback(PlaybackSessionCallback(context.applicationInfo.uid, ready, coordinator, timer, container, scope)).build()
        scope.launch { coordinator.state.map { it.sessionStatus() }.distinctUntilChanged().collect { session.setSessionExtras(it.toBundle()) } }
        val future = MediaController.Builder(context, session.token).buildAsync()
        try {
            await { future.isDone }
            val controller = future.get()
            controller.setMediaItems(listOf(QueueEntry("first", song).toMediaItem(), QueueEntry("second", song).toMediaItem()))
            controller.prepare(); controller.play()
            await { controller.isPlaying && controller.mediaItemCount == 2 }
            assertEquals("Artist", controller.mediaMetadata.artist)
            assertTrue(controller.playbackSnapshot()!!.connected)
            val beforeClock = controller.playbackSnapshot()!!
            controller.seekTo(9_000)
            await { engine.state.value.positionMs == 9_000L && bridge.currentPosition >= 9_000L }
            // Remote position delivery is scheduled on Android's clock. Real Thread.sleep in
            // await() does not advance Robolectric's virtual clock or deliver that heartbeat.
            shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(3))
            await { controller.currentPosition >= 9_000L }
            val clock = controller.positionSnapshot(beforeClock)!!
            assertSame(beforeClock.queue, clock.queue)
            assertTrue(clock.engine.positionMs >= 9_000L)
            controller.removeMediaItem(0)
            await { controller.currentMediaItem?.mediaId == "second" && controller.playbackSnapshot()?.canUndo == true }
            assertNull(controller.positionSnapshot(beforeClock))
            controller.sendCustomCommand(SessionCommand(PlaybackCommands.UNDO, Bundle.EMPTY), Bundle.EMPTY)
            await { controller.mediaItemCount == 2 && controller.playbackSnapshot()?.canUndo == false }
            assertEquals("second", controller.currentMediaItem?.mediaId)
            coordinator.setStorageError(true)
            await { controller.playbackSnapshot()?.storageError == true }
            controller.pause(); await { !engine.state.value.playRequested }
            controller.seekTo(12_000); await { engine.state.value.positionMs == 12_000L }
            controller.play(); await { engine.state.value.playing }
            controller.sendCustomCommand(SessionCommand(PlaybackCommands.SPEED, Bundle.EMPTY), Bundle().apply {
                putInt(PlaybackCommands.SPEED_TENTHS, 12)
                putBoolean(PlaybackCommands.PRESERVE_PITCH, false)
            })
            await { controller.playbackSnapshot()?.speed == PlaybackSpeed(12, false) }
            controller.sendCustomCommand(SessionCommand(PlaybackCommands.TIMER, Bundle.EMPTY), Bundle().apply {
                putString(PlaybackCommands.TIMER_MODE, SleepMode.END_TRACK.name)
            })
            await { controller.playbackSnapshot()?.sleepTimer?.mode == SleepMode.END_TRACK }
            val saved = kotlinx.coroutines.runBlocking {
                container.convenience.save("Test session", QueueSnapshot(coordinator.state.value.queue, 12_000), PlaybackSpeed(12, false))
            }
            val restored = controller.sendCustomCommand(SessionCommand(PlaybackCommands.RESTORE_SESSION, Bundle.EMPTY), Bundle().apply {
                putString(PlaybackCommands.SESSION_ID, saved.id)
            })
            await { restored.isDone }
            assertEquals(SessionResult.RESULT_SUCCESS, restored.get().resultCode)
            assertEquals(12_000L, coordinator.state.value.engine.positionMs)
            await { controller.currentMediaItem?.mediaId == "second" }
            assertFalse(engine.state.value.playing)
            assertEquals("second", controller.currentMediaItem?.mediaId)
            controller.play(); await { engine.state.value.playing }
            MediaController.releaseFuture(future)
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(engine.state.value.playing)
        } finally {
            MediaController.releaseFuture(future)
            session.release(); bridge.release(); coordinator.release(); scope.cancel()
        }
    }

    @Test fun untrustedClientsCannotConnectOrInjectMedia() {
        val context = RuntimeEnvironment.getApplication()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val coordinator = PlaybackCoordinator(resolver, Engine(), scope)
        val bridge = SessionPlayer(coordinator, scope)
        val ready = SettableFuture.create<Unit>().apply { set(Unit) }
        val callback = PlaybackSessionCallback(context.applicationInfo.uid, ready, coordinator)
        val session = MediaSession.Builder(context, bridge).setCallback(callback).build()
        try {
            val caller = MediaSession.ControllerInfo.createTestOnlyControllerInfo("untrusted.app", context.applicationInfo.uid + 100,
                1, 1, 100, false, Bundle.EMPTY, false)
            assertFalse(callback.onConnectAsync(session, caller).get().isAccepted)
            assertTrue(callback.onAddMediaItems(session, caller, listOf(QueueEntry(track = song).toMediaItem())).get().isEmpty())
        } finally { session.release(); bridge.release(); coordinator.release(); scope.cancel() }
    }

    private fun await(condition: () -> Boolean) {
        val deadline = System.nanoTime() + 5_000_000_000L
        while (!condition() && System.nanoTime() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        assertTrue("Media3 state did not arrive before timeout", condition())
    }
}
