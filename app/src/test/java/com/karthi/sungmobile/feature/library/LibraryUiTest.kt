package com.karthi.sungmobile.feature.library

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.lifecycle.ViewModelStore
import com.github.takahirom.roborazzi.captureRoboImage
import com.karthi.sungmobile.core.designsystem.theme.*
import com.karthi.sungmobile.core.model.library.*
import com.karthi.sungmobile.feature.library.components.LibraryHeader
import com.karthi.sungmobile.testing.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w393dp-h852dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LibraryUiTest {
    @get:Rule val compose = createComposeRule()
    private val store = ViewModelStore()
    @After fun clear() { store.clear() }

    @Test fun heartAndMenuDoNotStartPlaybackAndSelectionReplacesPlayback() {
        var played = 0
        var liked = 0
        var removed = false
        var selecting by mutableStateOf(false)
        var selected by mutableStateOf(false)
        val song = testSong.copy(title = "Evening song", artworkUrl = null)
        compose.setContent { SungTheme {
            CompositionLocalProvider(LocalLibraryUi provides LibraryUi(toggle = { liked++ })) {
                LibrarySongRow(LibrarySong("second", song, true), selected, { selected = it },
                    { played++ }, {}, {}, {}, { removed = true }, selectionMode = selecting)
            }
        } }
        compose.onNodeWithContentDescription("Unlike Evening song").performClick()
        assertEquals(1, liked); assertEquals(0, played)
        compose.onNodeWithContentDescription("Options for Evening song").performClick()
        compose.onNodeWithText("Remove from playlist").performClick()
        assertTrue(removed); assertEquals(0, played)
        compose.runOnIdle { selecting = true }
        compose.onNodeWithText("Evening song").performClick()
        assertTrue(selected); assertEquals(0, played)
    }

    @Test fun sessionsAndAdvancedLibraryDestinationsRemainReachable() {
        var sessions = false
        var smart = false
        compose.setContent { SungTheme { Column {
            LibraryHeader(LibraryCollection.Likes, {}, { sessions = true }, {}, { smart = true }, {})
        } } }
        compose.onNodeWithText("Sessions").performScrollTo().performClick()
        assertTrue(sessions)
        compose.onNodeWithContentDescription("Library options").performClick()
        compose.onNodeWithText("Smart playlists").performClick()
        assertTrue(smart)
    }

    @Test fun libraryLightAndSelection() = render(false, 1f)
    @Test fun libraryDarkLargeText() = render(true, 2f)

    @Test fun playlistsRender() = render(false, 1f, playlists = true)

    private fun render(dark: Boolean, scale: Float, playlists: Boolean = false) {
        val repo = TestLocalLibrary()
        val player = RecordingPlayer()
        val browser = LibraryBrowserViewModel(repo).also { store.put("browser", it) }
        val actions = LibraryActionsViewModel(repo, player).also { store.put("actions", it) }
        val advanced = AdvancedLibraryViewModel(TestAdvancedRepository(), repo, TestCatalog(), player).also { store.put("advanced", it) }
        val rows = listOf("Evening song", "A longer song title that should wrap without hiding the actions", "Morning light")
            .mapIndexed { i, title -> LibrarySong("row$i", testSong.copy(title = title, artworkUrl = null), true) }
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                SungTheme(if (dark) ThemeMode.DARK else ThemeMode.LIGHT, reduceMotion = true) { Surface {
                    LibraryScreen(browser, if (playlists) LibraryPage(query = LibraryQuery(collection = null), playlists = listOf(LocalPlaylist("a", "Evening favorites", true, 12), LocalPlaylist("b", "Morning mix", false, 7)), loading = false) else LibraryPage(songs = rows, loading = false), actions, advanced, AdvancedLibraryState(), {})
                } }
            }
        }
        val dir = File("build/outputs/library-ui").apply { mkdirs() }
        compose.onRoot().captureRoboImage(File(dir, "library-${if (playlists) "playlists" else if (dark) "dark-large" else "light"}.png").absolutePath)
        if (playlists) {
            compose.onNodeWithContentDescription("Options for Evening favorites").performClick()
            compose.onNodeWithText("Unpin").assertExists()
            compose.onNodeWithText("Rename").assertExists()
            compose.onNodeWithText("Delete").assertExists()
            return
        }
        compose.onNodeWithText("Select", useUnmergedTree = true).performScrollTo().performClick()
        compose.onNodeWithTag("library_song_row0").performScrollTo().performClick()
        compose.onNodeWithText("1 selected").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Done").performScrollTo().performClick()
        compose.onNodeWithText("1 selected").assertDoesNotExist()
    }
}
