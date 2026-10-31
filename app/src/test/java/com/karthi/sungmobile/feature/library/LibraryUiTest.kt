package com.karthi.sungmobile.feature.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.lifecycle.ViewModelStore
import com.github.takahirom.roborazzi.captureRoboImage
import com.karthi.sungmobile.core.designsystem.theme.*
import com.karthi.sungmobile.core.model.library.*
import com.karthi.sungmobile.feature.library.components.LibraryHeader
import com.karthi.sungmobile.feature.library.components.LibraryDestination
import com.karthi.sungmobile.feature.library.components.LibraryListControls
import com.karthi.sungmobile.feature.library.components.LibraryPlaylistControls
import com.karthi.sungmobile.feature.home.HomeState
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

    @Test fun libraryTabsAndTopActionsRemainReachable() {
        var selected = LibraryDestination.LIKED
        var sessions = false
        var settings = false
        var search = false
        compose.setContent { SungTheme { Column {
            LibraryHeader(selected, { if (it == LibraryDestination.SESSIONS) sessions = true else selected = it },
                { settings = true }, { search = true })
        } } }
        compose.onNodeWithText("Mixes").performScrollTo().performClick()
        assertEquals(LibraryDestination.MIXES, selected)
        compose.onNodeWithText("Recents").performScrollTo().performClick()
        assertEquals(LibraryDestination.RECENTS, selected)
        compose.onNodeWithText("Sessions").performScrollTo().performClick()
        assertTrue(sessions)
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithContentDescription("Search").performClick()
        assertTrue(settings)
        assertTrue(search)
    }

    @Test fun splitPlayMenuKeepsShuffleAndQueueActions() {
        var shuffled = false
        var next = false
        var queued = false
        compose.setContent { SungTheme {
            LibraryListControls("", "Filter liked songs", {}, LibrarySort.DEFAULT, {}, false, true,
                {}, { shuffled = true }, { next = true }, { queued = true }, {})
        } }
        compose.onNodeWithContentDescription("Play all songs").assertExists()
        compose.onNodeWithContentDescription("More play options").performClick()
        compose.onNodeWithContentDescription("Close play options").assertExists()
        compose.onNodeWithText("Shuffle").performClick()
        assertTrue(shuffled)
        compose.onNodeWithContentDescription("More play options").performClick()
        compose.onNodeWithText("Add to queue").performClick()
        assertTrue(queued)
        compose.onNodeWithContentDescription("More play options").performClick()
        compose.onNodeWithText("Play next").performClick()
        assertTrue(next)
    }

    @Test fun libraryFiltersOpenOnlyWhenRequested() {
        var expanded by mutableStateOf(false)
        compose.setContent { SungTheme {
            LibraryListControls("", "Filter liked songs", {}, LibrarySort.DEFAULT, {}, false, true,
                {}, {}, {}, {}, {}, expanded, { expanded = !expanded })
        } }
        compose.onNodeWithText("Filter liked songs").assertDoesNotExist()
        compose.onNodeWithText("A–Z").assertDoesNotExist()
        compose.onNodeWithContentDescription("Show filters and sort").performClick()
        compose.onNodeWithText("Filter liked songs").assertIsDisplayed()
        compose.onNodeWithText("A–Z").assertIsDisplayed()
        compose.onNodeWithContentDescription("Hide filters and sort").performClick()
        compose.onNodeWithText("Filter liked songs").assertDoesNotExist()
    }

    @Test fun playlistFiltersAreCollapsedBesideNewPlaylist() {
        var expanded by mutableStateOf(false)
        compose.setContent { SungTheme {
            LibraryPlaylistControls("", {}, LibrarySort.DEFAULT, {}, expanded,
                { expanded = !expanded }, {})
        } }
        compose.onNodeWithText("New playlist").assertIsDisplayed()
        compose.onNodeWithText("Filter playlists").assertDoesNotExist()
        compose.onNodeWithContentDescription("Show filters and sort").performClick()
        compose.onNodeWithText("Filter playlists").assertIsDisplayed()
        compose.onNodeWithText("A–Z").assertIsDisplayed()
    }

    @Test fun cleanupPageKeepsDestructiveActionsBehindReview() {
        val repo = TestLocalLibrary()
        val player = RecordingPlayer()
        val advanced = AdvancedLibraryViewModel(TestAdvancedRepository(), repo, TestCatalog(), player)
            .also { store.put("cleanup", it) }
        val entries = listOf(LibrarySong("first", testSong, true), LibrarySong("second", testSong, true))
        val state = AdvancedLibraryState(page = AdvancedPage.CLEANUP, playlistId = "playlist",
            playlistName = "Evening favorites", entries = entries, selected = setOf("second"),
            revisions = listOf(PlaylistRevision("version", "playlist", "Evening favorites", 1_000L, 2)))
        compose.setContent { SungTheme { AdvancedLibraryScreen(advanced, state) } }
        compose.onNodeWithText("Duplicates").assertIsDisplayed()
        compose.onNodeWithText("Review removal (1)").performClick()
        compose.onNodeWithText("Remove 1 playlist entries?").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Remove 1 playlist entries?").assertDoesNotExist()
        val dir = File("build/outputs/library-ui").apply { mkdirs() }
        compose.onRoot().captureRoboImage(File(dir, "playlist-cleanup.png").absolutePath)
        compose.onNodeWithTag("advanced_library_list").performScrollToNode(hasText("Saved versions (1)"))
        compose.onNodeWithText("Saved versions (1)").assertIsDisplayed()
    }

    @Test fun selectedPlaylistVersionOpensAnImmediatePreview() {
        val repo = TestLocalLibrary()
        val player = RecordingPlayer()
        val advanced = AdvancedLibraryViewModel(TestAdvancedRepository(), repo, TestCatalog(), player)
            .also { store.put("version-preview", it) }
        val revision = PlaylistRevision("version", "playlist", "Old favorites", 1_000L, 1)
        val state = AdvancedLibraryState(page = AdvancedPage.CLEANUP, playlistId = "playlist",
            playlistName = "Current favorites", revisionDetail = RevisionDetail(revision,
                listOf(RevisionEntry("old", "song", "Old song"))))
        compose.setContent { SungTheme { AdvancedLibraryScreen(advanced, state) } }
        compose.onNodeWithText("Version preview").assertIsDisplayed()
        compose.onNodeWithText("Show songs").performClick()
        compose.onNodeWithText("1. Old song").assertIsDisplayed()
    }

    @Test fun savedPlaylistUsesTheSameCollapsibleFilters() {
        val repo = TestLocalLibrary()
        val player = RecordingPlayer()
        val browser = LibraryBrowserViewModel(repo).also { store.put("playlist-browser", it) }
        val actions = LibraryActionsViewModel(repo, player).also { store.put("playlist-actions", it) }
        val advanced = AdvancedLibraryViewModel(TestAdvancedRepository(), repo, TestCatalog(), player)
            .also { store.put("playlist-advanced", it) }
        val page = LibraryPage(query = LibraryQuery(LibraryCollection.Playlist("playlist")),
            songs = listOf(LibrarySong("song", testSong, true)), title = "Evening favorites", loading = false)
        compose.setContent { SungTheme {
            LibraryScreen(browser, page, actions, advanced, AdvancedLibraryState(), {})
        } }
        compose.onNodeWithText("Filter this playlist").assertDoesNotExist()
        compose.onNodeWithContentDescription("Show filters and sort").performClick()
        compose.onNodeWithText("Filter this playlist").assertIsDisplayed()
        compose.onNodeWithText("Playlist order").assertIsDisplayed()
    }

    @Test fun mixesShowRealCardsAndSavedSmartList() {
        val repo = TestLocalLibrary()
        val player = RecordingPlayer()
        val actions = LibraryActionsViewModel(repo, player).also { store.put("mix-actions", it) }
        val songs = listOf(testSong, testSong.copy(id = "second", title = "Second song"))
        val smart = SmartPlaylist("smart-1", "Evening mix", SmartRule())
        var opened: String? = null
        var refreshed = false
        compose.setContent { SungTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
            LibraryMixesContent(HomeState(likedMix = songs, loading = false),
                SmartOverview(listOf(smart), loading = false), true, actions, { refreshed = true }, { opened = it }, {})
        } } }
        compose.onNodeWithContentDescription("Refresh mixes").performClick()
        assertTrue(refreshed)
        compose.onNodeWithTag("play_library_mix_Liked shuffle").performClick()
        assertEquals(songs, player.batches.single())
        compose.onNodeWithText("Evening mix").performScrollTo().performClick()
        assertEquals("smart-1", opened)
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
