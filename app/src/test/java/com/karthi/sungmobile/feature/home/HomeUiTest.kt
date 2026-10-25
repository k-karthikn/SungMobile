package com.karthi.sungmobile.feature.home

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
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
import com.karthi.sungmobile.core.model.discovery.HomeShelf
import com.karthi.sungmobile.core.playback.*
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
    private val loaded = AtomicInteger()
    private val imageErrors = CopyOnWriteArrayList<String>()
    private val track by lazy { item("RB-RcX5DS5A", CatalogKind.SONG, "The Scientist") }
    private val album by lazy { item("MPREb_abcdefghijkl", CatalogKind.ALBUM, "A quiet album") }
    private var feed by mutableStateOf(DiscoveryFeedState(initialLoading = false))
    private var opened: CatalogItem? = null
    private var mood: String? = null
    private var refreshed = 0
    private var statisticsOpened = false

    @OptIn(coil3.annotation.DelicateCoilApi::class)
    @Before fun images() {
        SingletonImageLoader.setUnsafe(ImageLoader.Builder(ApplicationProvider.getApplicationContext<Context>())
            .components { add(BitmapFactoryDecoder.Factory()) }.eventListenerFactory {
                object : EventListener() {
                    override fun onSuccess(request: ImageRequest, result: SuccessResult) { loaded.incrementAndGet() }
                    override fun onError(request: ImageRequest, result: ErrorResult) { imageErrors.add(result.throwable.toString()) }
                }
            }.build())
        feed = DiscoveryFeedState(shelves = listOf(
            HomeShelf("quick", "Quick picks", listOf(track, track.copy(id = "dvgZkm1xWPE", title = "Viva La Vida"))),
            HomeShelf("albums", "New releases", listOf(album))), initialLoading = false)
    }

    @Test fun lightHome() { show(); capture("home-provider-light") }
    @Test fun darkHome() { show(dark = true); capture("home-provider-dark") }
    @Test fun reducedMotion() { show(dark = true, reduced = true); capture("home-provider-reduced") }
    @Test @Config(sdk = [35], qualifiers = "w320dp-h640dp-xhdpi")
    fun narrowHome() { show(); capture("home-provider-narrow") }
    @Test @Config(sdk = [35], qualifiers = "w1000dp-h800dp-xhdpi")
    fun expandedHome() { show(); capture("home-provider-expanded") }
    @Test fun largeText() { show(fontScale = 2f); capture("home-provider-large-text") }

    @Test fun cardsActDirectlyAndHomeDoesNotDuplicateLibrary() {
        show()
        compose.onNodeWithTag("home_card_quick_0").performClick()
        compose.runOnIdle { assertEquals(track, opened) }
        compose.onNodeWithTag("discovery_home").performScrollToKey("albums")
        compose.onNodeWithTag("home_card_albums_0").performClick()
        compose.runOnIdle { assertEquals(album, opened) }
        compose.onNodeWithText("Your mixes").assertDoesNotExist()
        compose.onNodeWithText("Recently played").assertDoesNotExist()
        compose.onNodeWithText("Pinned playlists").assertDoesNotExist()
        compose.onNodeWithText("Liked songs").assertDoesNotExist()
        compose.onNodeWithTag("discovery_home").performScrollToKey("header")
        compose.onNodeWithContentDescription("Refresh Home").performClick()
        assertEquals(1, refreshed)
        compose.onNodeWithTag("home_listening_statistics").performClick()
        assertTrue(statisticsOpened)
    }

    @Test fun moodShortcutAndOfflineStateKeepNavigationUsable() {
        show()
        compose.onNodeWithTag("mood_focus").performClick()
        assertEquals("instrumental focus", mood)
        compose.runOnIdle { feed = DiscoveryFeedState(initialLoading = false, error = SourceFailure.NETWORK) }
        compose.onNodeWithText("Could not load music suggestions.").assertExists()
        compose.onNodeWithText("Retry").performClick()
        assertEquals(1, refreshed)
        compose.onNodeWithText("Open Library").performClick()
        compose.onNodeWithText("Your local library is ready for liked songs and playlists.").assertExists()
    }

    private fun show(dark: Boolean = false, fontScale: Float = 1f, reduced: Boolean = false) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                SungTheme(if (dark) ThemeMode.DARK else ThemeMode.LIGHT, reduceMotion = reduced) {
                    SungApp(CatalogState(), PlaybackState(),
                        CatalogActions({}, {}, {}, {}, {}, {}, {}, {}),
                        PlayerActions({}, {}, {}), Appearance(), {},
                        homeContent = { modifier, search, _, appearance, library ->
                            DiscoveryHomeScreen(feed, { refreshed++ }, {}, search, appearance, library,
                                { statisticsOpened = true },
                                { mood = it }, { _, item -> opened = item }, true, modifier)
                        })
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

    private fun item(id: String, kind: CatalogKind, title: String): CatalogItem {
        val art = File("build/tmp/home-fixtures/RB-RcX5DS5A.jpg").apply {
            parentFile!!.mkdirs()
            if (!exists()) writeBytes(this@HomeUiTest.javaClass.getResourceAsStream("/artwork/RB-RcX5DS5A.jpg")!!.use { it.readBytes() })
        }
        val url = when (kind) {
            CatalogKind.SONG -> "https://www.youtube.com/watch?v=$id"
            else -> "https://music.youtube.com/browse/$id"
        }
        return CatalogItem(id, kind, title, "Artist", url, art.toURI().toString(), 180_000)
    }
}
