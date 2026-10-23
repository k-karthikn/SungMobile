package com.karthi.sungmobile.feature.home

import android.content.Context
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.core.app.ApplicationProvider
import coil3.ImageLoader
import coil3.EventListener
import coil3.SingletonImageLoader
import coil3.decode.BitmapFactoryDecoder
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.ErrorResult
import com.github.takahirom.roborazzi.captureRoboImage
import com.karthi.sungmobile.app.*
import com.karthi.sungmobile.core.designsystem.theme.*
import com.karthi.sungmobile.core.model.*
import com.karthi.sungmobile.core.model.discovery.HomeData
import com.karthi.sungmobile.core.model.library.*
import com.karthi.sungmobile.core.playback.*
import com.karthi.sungmobile.feature.library.*
import com.karthi.sungmobile.testing.RecordingPlayer
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.CopyOnWriteArrayList
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w393dp-h852dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeUiTest {
    @get:Rule val compose = createComposeRule()
    private val tracks = listOf(song("RB-RcX5DS5A", "The Scientist"), song("dvgZkm1xWPE", "Viva La Vida"))
    private val player = RecordingPlayer().apply {
        state.value = PlaybackState(track = tracks[0], engine = EngineSnapshot(
            playing = true, playRequested = true, positionMs = 32_000, durationMs = 180_000, seekable = true))
    }
    private var home by mutableStateOf(HomeState(
        HomeData(liked = tracks, recent = tracks, pinned = listOf(LocalPlaylist("pin", "Evening favorites", true, 2))),
        tracks, tracks.reversed(), loading = false))
    private var likes by mutableStateOf(setOf(tracks[0].libraryKey()))
    private var toggles = 0
    private var saved: List<CatalogItem>? = null
    private var sought = -1L
    private var chromeCompositions = 0
    private var displayedPosition = -1L
    private var displayedTimer = -1L
    private val loaded = AtomicInteger()
    private val imageErrors = CopyOnWriteArrayList<String>()

    @OptIn(coil3.annotation.DelicateCoilApi::class)
    @Before fun images() {
        SingletonImageLoader.setUnsafe(ImageLoader.Builder(ApplicationProvider.getApplicationContext<Context>())
            .components { add(BitmapFactoryDecoder.Factory()) }.eventListenerFactory {
                object : EventListener() {
                    override fun onSuccess(request: ImageRequest, result: SuccessResult) { loaded.incrementAndGet() }
                    override fun onError(request: ImageRequest, result: ErrorResult) { imageErrors.add(result.throwable.toString()) }
                }
            }.build())
    }

    @Test fun lightHome() { show(); capture("home-light") }
    @Test fun darkHome() { show(dark = true); capture("home-dark") }
    @Test fun largeText() { show(fontScale = 2f); capture("home-large-text")
        compose.onNodeWithTag("discovery_home").performScrollToKey("recent_heading")
        capture("home-large-text-list")
    }
    @Test fun reducedMotion() { show(dark = true, reduced = true); capture("home-dark-reduced-motion") }
    @Test @Config(sdk = [35], qualifiers = "w320dp-h640dp-xhdpi")
    fun narrowHome() { show(); capture("home-narrow") }
    @Test @Config(sdk = [35], qualifiers = "w1000dp-h800dp-xhdpi")
    fun expandedHome() { show(); capture("home-expanded") }

    @Test fun heartAndPlusDoNotStartPlaybackAndMixActionsKeepTheirTargets() {
        show()
        compose.onNodeWithContentDescription("Save Liked shuffle to a playlist").performClick()
        compose.runOnIdle { assertEquals(tracks, saved); assertTrue(player.batches.isEmpty()) }
        compose.onNodeWithTag("discovery_home").performScrollToKey("recent_heading")
        compose.onAllNodesWithTag("home_like_${tracks[0].id}").onFirst()
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).performClick()
        compose.runOnIdle { assertFalse(tracks[0].libraryKey() in likes); assertEquals(1, toggles); assertTrue(player.batches.isEmpty()) }
        compose.onAllNodesWithTag("home_actions_${tracks[0].id}").onFirst().performClick()
        compose.onNodeWithText("Add to playlist").assertIsDisplayed()
        compose.onNodeWithText("Play next").performClick()
        compose.runOnIdle { assertEquals(listOf(tracks[0]), player.batches.single()) }
        compose.onNodeWithTag("discovery_home").performScrollToKey("mixes")
        compose.onNodeWithTag("play_mix_Liked shuffle").performClick()
        compose.runOnIdle { assertEquals(tracks, player.batches.last()) }
    }

    @Test fun privateModeRemovesRecentAndHomeScrollSurvivesNavigation() {
        show()
        compose.onNodeWithTag("discovery_home").performScrollToKey("pinned_heading")
        compose.onNodeWithText("Pinned playlists").assertIsDisplayed()
        compose.onNodeWithTag("nav_search").performClick()
        compose.onNodeWithTag("nav_home").performClick()
        compose.onNodeWithText("Pinned playlists").assertIsDisplayed()
        compose.runOnIdle { home = home.copy(data = home.data.copy(privateMode = true)) }
        compose.onNodeWithTag("discovery_home").performScrollToKey("header")
        compose.onNodeWithText("Recent mix").assertDoesNotExist()
        compose.onNodeWithText("Recently played").assertDoesNotExist()
        compose.onNodeWithText("Liked shuffle").assertExists()
    }

    @Test fun clockUpdatesStayOutOfChromeButPlayerReceivesLiveSeekAndTimer() {
        show()
        compose.waitForIdle()
        val before = chromeCompositions
        compose.runOnIdle { player.state.value = player.state.value.copy(engine = player.state.value.engine.copy(positionMs = 52_000)) }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(before, chromeCompositions) }
        compose.onNodeWithTag("mini_player").performClick()
        compose.onNodeWithTag("seek_forward").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(62_000, sought)
            player.state.value = player.state.value.copy(sleepTimer = SleepTimerStatus(SleepMode.DURATION, 43_000))
        }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(52_000, displayedPosition); assertEquals(43_000, displayedTimer) }
        compose.runOnIdle { player.state.value = player.state.value.copy(sleepTimer = SleepTimerStatus(SleepMode.DURATION, 42_000)) }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(42_000, displayedTimer) }
    }

    @Test fun emptyHomeKeepsSearchAndAppearanceReachable() {
        home = HomeState(loading = false)
        show()
        compose.onNodeWithTag("home_search").performClick()
        compose.onNodeWithTag("search_input").assertIsDisplayed()
        compose.onNodeWithTag("nav_home").performClick()
        compose.onNodeWithTag("appearance").performClick()
        compose.onNodeWithText("Sung blue").assertExists()
    }

    private fun show(dark: Boolean = false, fontScale: Float = 1f, reduced: Boolean = false) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                SungTheme(if (dark) ThemeMode.DARK else ThemeMode.LIGHT, reduceMotion = reduced) {
                    val flow = remember { player.state.chromeUpdates() }
                    val initialPlayback = remember { player.state.value.withoutClock() }
                    val chrome by flow.collectAsStateWithLifecycle(initialPlayback)
                    SideEffect { chromeCompositions++ }
                    CompositionLocalProvider(LocalLibraryUi provides LibraryUi(likes, { track ->
                        toggles++; val key = track.libraryKey(); likes = if (key in likes) likes - key else likes + key
                    }, { saved = it })) {
                        SungApp(CatalogState(), chrome, CatalogActions({}, {}, {}, {}, {}, {}, {}, {}),
                            PlayerActions({}, { sought = it }, {}), Appearance(), {}, livePlayback = player.state,
                            homeContent = { modifier, search, nowPlaying, appearance, _ ->
                                DiscoveryHomeScreen(home, {}, player, search, nowPlaying, appearance, {}, modifier)
                            }, convenienceContent = { state, _ ->
                                SideEffect { displayedPosition = state.engine.positionMs; displayedTimer = state.sleepTimer?.remainingMs ?: -1L }
                            })
                    }
                }
            }
        }
    }

    private fun capture(name: String) {
        compose.waitUntil(15_000) { loaded.get() >= 2 || imageErrors.isNotEmpty() }
        assertTrue("Artwork errors: $imageErrors", imageErrors.isEmpty())
        compose.waitForIdle()
        val dir = File("build/outputs/home-ui").apply { mkdirs() }
        compose.onRoot().captureRoboImage(File(dir, "$name.png").absolutePath)
    }

    private fun song(id: String, title: String): CatalogItem {
        val art = File("build/tmp/home-fixtures/$id.jpg").apply {
            parentFile!!.mkdirs()
            writeBytes(this@HomeUiTest.javaClass.getResourceAsStream("/artwork/$id.jpg")!!.use { it.readBytes() })
        }
        return CatalogItem(id, CatalogKind.SONG, title, "Coldplay", "https://www.youtube.com/watch?v=$id", art.toURI().toString(), 180_000)
    }
}
