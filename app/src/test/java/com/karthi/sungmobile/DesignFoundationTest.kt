package com.karthi.sungmobile

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.karthi.sungmobile.app.*
import com.karthi.sungmobile.core.designsystem.theme.*
import com.karthi.sungmobile.core.model.*
import com.karthi.sungmobile.core.playback.*
import com.karthi.sungmobile.feature.player.PlayerScreen
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Before
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.EventListener
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.ErrorResult
import coil3.decode.BitmapFactoryDecoder
import java.util.concurrent.CopyOnWriteArrayList

/** Real Compose renders, offline fixture data; never packaged in the app. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w393dp-h852dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DesignFoundationTest {
    @get:Rule val compose = createComposeRule()
    private val scientist = track("RB-RcX5DS5A", "The Scientist")
    private val viva = track("dvgZkm1xWPE", "Viva La Vida")
    private val catalog = CatalogState(query = "Coldplay", page = CatalogPage("Coldplay", listOf(scientist, viva)))
    private val player = PlaybackState(track = scientist, engine = EngineSnapshot(
        playing = true, playRequested = true, positionMs = 102_000, durationMs = 268_000, seekable = true))
    private val catalogActions = CatalogActions({}, {}, {}, {}, {}, {}, {}, {})
    private val playerActions = PlayerActions({}, {}, {})
    private val loadedArtwork = AtomicInteger()
    private val artworkErrors = CopyOnWriteArrayList<String>()

    @OptIn(coil3.annotation.DelicateCoilApi::class)
    @Before fun imageLoader() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        SingletonImageLoader.setUnsafe(ImageLoader.Builder(context)
            // Robolectric cannot use Android ImageDecoder's file-descriptor path. Exercise
            // Coil's real file fetching/caching with its BitmapFactory decoder on the host.
            .components { add(BitmapFactoryDecoder.Factory()) }.eventListenerFactory {
            object : EventListener() {
                override fun onSuccess(request: ImageRequest, result: SuccessResult) { loadedArtwork.incrementAndGet() }
                override fun onError(request: ImageRequest, result: ErrorResult) {
                    artworkErrors.add("${request.data}: ${result.throwable}")
                }
            }
        }.build())
    }

    @Test fun homeLight() = render("home-light")
    @Test fun homeDark() = render("home-dark", dark = true)
    @Test fun homeLargeText() = render("home-large-text", fontScale = 2f)
    @Test fun homeDarkLargeText() = render("home-dark-large-text", dark = true, fontScale = 2f)
    @Test fun homeDarkReducedMotion() = render("home-dark-reduced-motion", dark = true, reduced = true)
    @Test fun playerLight() = render("player-light", fullPlayer = true)
    @Test fun playerDark() = render("player-dark", dark = true, fullPlayer = true)
    @Test fun playerLargeText() = render("player-large-text", fontScale = 2f, fullPlayer = true)
    @Test fun playerDarkLargeText() = render("player-dark-large-text", dark = true, fontScale = 2f, fullPlayer = true)
    @Test fun playerDarkReducedMotion() = render("player-dark-reduced-motion", dark = true, reduced = true, fullPlayer = true)

    @Test
    @Config(sdk = [35], qualifiers = "w320dp-h640dp-xhdpi")
    fun narrowHome() = render("home-320dp")

    @Test
    @Config(sdk = [35], qualifiers = "w1000dp-h800dp-xhdpi")
    fun expandedHome() = render("home-expanded")

    @Test fun freshInstallHasUsefulSearchAndHonestLibrary() {
        compose.setContent {
            SungTheme {
                SungApp(CatalogState(), PlaybackState(), catalogActions, playerActions, Appearance(), {})
            }
        }
        capture("home-empty", waitForArtwork = false)
        compose.onNodeWithTag("home_search").performClick()
        compose.onNodeWithTag("search_input").assertIsDisplayed()
        capture("search-empty", waitForArtwork = false)
        compose.onNodeWithTag("nav_library").performClick()
        compose.onNodeWithText("Your local library is ready for liked songs and playlists.").assertIsDisplayed()
        capture("library-empty", waitForArtwork = false)
        compose.onNodeWithTag("nav_search").performClick()
        compose.onNodeWithTag("search_input").assertIsDisplayed()
    }

    @Test fun appearanceIsReachableFromHome() {
        compose.setContent {
            SungTheme {
                SungApp(CatalogState(), PlaybackState(), catalogActions, playerActions, Appearance(), {})
            }
        }
        compose.onNodeWithTag("appearance").performClick()
        compose.onNodeWithText("Appearance & Home").assertIsDisplayed()
        compose.onNodeWithText("Spotify").assertIsDisplayed()
        compose.onNodeWithText("Appearance & Home").performClick()
        compose.onNodeWithText("System").assertIsDisplayed()
        compose.onNodeWithText("Sung blue").assertIsDisplayed()
        capture("appearance", waitForArtwork = false)
    }

    @Test fun miniPlayerAndFullPlayerUseTheSameCallbacks() {
        var toggles = 0
        var sought = -1L
        compose.setContent {
            SungTheme {
                SungApp(catalog, player, catalogActions, PlayerActions({ toggles++ }, { sought = it }, {}), Appearance(), {})
            }
        }
        compose.onNodeWithTag("mini_play_pause").performClick()
        compose.onNodeWithTag("mini_player").performClick()
        compose.onNodeWithTag("player_title").assertTextEquals("The Scientist")
        compose.onNodeWithTag("play_pause").performScrollTo().performClick()
        compose.onNodeWithTag("seek_forward").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(2, toggles); assertEquals(112_000L, sought) }
        compose.onNodeWithContentDescription("Close player").performScrollTo().performClick()
        compose.onNodeWithTag("mini_player").assertIsDisplayed()
    }

    @Test fun miniPlayerTransportAndMenuUseExistingActionsWithoutOpeningPlayer() {
        val first = QueueEntry(track = scientist)
        val second = QueueEntry(track = viva)
        val queued = player.copy(queue = QueueState(listOf(first, second), currentId = first.id))
        var previous = 0
        var next = 0
        var shuffled: Boolean? = null
        compose.setContent {
            SungTheme {
                SungApp(catalog, queued, catalogActions,
                    PlayerActions({}, {}, {}, next = { next++ }, previous = { previous++ },
                        shuffle = { shuffled = it }), Appearance(), {},
                    volumeContent = { androidx.compose.material3.Text("Song volume available") })
            }
        }
        compose.onNodeWithTag("mini_previous").performClick()
        compose.onNodeWithTag("mini_next").performClick()
        compose.onNodeWithTag("player_title").assertDoesNotExist()
        compose.onNodeWithText("1:42").assertIsDisplayed()
        compose.onNodeWithText("4:28").assertIsDisplayed()
        compose.onNodeWithTag("mini_more").performClick()
        compose.onNodeWithText("Shuffle").performClick()
        compose.runOnIdle {
            assertEquals(1, previous)
            assertEquals(1, next)
            assertEquals(true, shuffled)
        }
        compose.onNodeWithTag("mini_more").performClick()
        compose.onNodeWithText("This song's volume").performClick()
        compose.onNodeWithText("Song volume available").assertIsDisplayed()
    }

    @Test fun miniPlayerQueueActionOpensTheExistingQueueSheet() {
        val first = QueueEntry(track = scientist)
        val second = QueueEntry(track = viva)
        val queued = player.copy(queue = QueueState(listOf(first, second), currentId = first.id))
        compose.setContent {
            SungTheme {
                SungApp(catalog, queued, catalogActions, playerActions, Appearance(), {})
            }
        }
        compose.onNodeWithTag("mini_more").performClick()
        compose.onNodeWithText("Queue (2)").performClick()
        compose.onNodeWithTag("queue_now_playing").assertIsDisplayed()
        compose.onNodeWithTag("open_queue").assertContentDescriptionContains("Collapse queue, Queue (2)")
    }

    @Test fun statisticsPageHidesMiniPlayerAndNavigation() {
        var statisticsOpen by mutableStateOf(false)
        compose.setContent {
            SungTheme {
                SungApp(catalog, player, catalogActions, playerActions, Appearance(), {},
                    homeContent = { _, _, _, _, openLibrary ->
                        androidx.compose.material3.Button({ statisticsOpen = true; openLibrary() }) {
                            androidx.compose.material3.Text("Open statistics")
                        }
                    },
                    libraryContent = { _, _, _, _ ->
                        androidx.compose.material3.Text("Statistics page")
                    }, statisticsPageOpen = statisticsOpen)
            }
        }
        compose.onNodeWithTag("mini_player").assertIsDisplayed()
        compose.onNodeWithText("Open statistics").performClick()
        compose.onNodeWithText("Statistics page").assertIsDisplayed()
        compose.onNodeWithTag("mini_player").assertDoesNotExist()
        compose.onNodeWithTag("nav_library").assertDoesNotExist()
    }

    private fun render(name: String, dark: Boolean = false, fontScale: Float = 1f,
        reduced: Boolean = false, fullPlayer: Boolean = false) {
        var motion = true
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                SungTheme(if (dark) ThemeMode.DARK else ThemeMode.LIGHT, reduceMotion = reduced) {
                    val enabled = LocalSungMotionEnabled.current
                    SideEffect { motion = enabled }
                    if (fullPlayer) androidx.compose.material3.Surface {
                        PlayerScreen(player, playerActions, {})
                    } else SungApp(catalog, player, catalogActions, playerActions, Appearance(), {})
                }
            }
        }
        compose.waitForIdle()
        if (reduced) compose.runOnIdle { assertFalse(motion) }
        capture(name)
        if (fullPlayer) {
            compose.onNodeWithTag("play_pause").performScrollTo().assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
            compose.onNodeWithTag("player_more").performScrollTo().performClick()
            compose.onNodeWithTag("stop").performScrollTo().assertIsDisplayed()
            capture("$name-controls")
        }
    }

    private fun capture(name: String, waitForArtwork: Boolean = true) {
        if (waitForArtwork) {
            compose.waitUntil(15_000) { loadedArtwork.get() > 0 || artworkErrors.isNotEmpty() }
            assertTrue("Artwork failed: $artworkErrors", artworkErrors.isEmpty())
        }
        compose.waitForIdle()
        val dir = File("build/outputs/design-foundation").apply { mkdirs() }
        compose.onRoot().captureRoboImage(File(dir, "$name.png").absolutePath)
    }

    private fun track(id: String, title: String): CatalogItem {
        // AGP may package JVM resources in a jar. Coil needs an actual readable file URI.
        val artwork = File("build/tmp/design-fixtures/$id.jpg").apply {
            parentFile!!.mkdirs()
            writeBytes(this@DesignFoundationTest.javaClass.getResourceAsStream("/artwork/$id.jpg")!!.use { it.readBytes() })
        }
        return CatalogItem(id, CatalogKind.SONG, title, "Coldplay",
            "https://www.youtube.com/watch?v=$id", artwork.toURI().toString(), 268_000)
    }
}
