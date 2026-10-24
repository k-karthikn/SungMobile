package com.karthi.sungmobile

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.captureRoboImage
import com.karthi.sungmobile.core.model.*
import com.karthi.sungmobile.app.CatalogState
import com.karthi.sungmobile.app.CatalogActions
import com.karthi.sungmobile.feature.search.SearchScreen
import com.karthi.sungmobile.core.designsystem.theme.SungTheme
import com.karthi.sungmobile.core.designsystem.theme.ThemeMode
import com.karthi.sungmobile.feature.library.LibraryUi
import com.karthi.sungmobile.feature.library.LocalLibraryUi
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w393dp-h852dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SearchHostTest {
    @get:Rule val compose = createComposeRule()

    @Test fun unavailableCatalogOffersRetryAndBack() {
        var retries = 0
        var backs = 0
        compose.setContent {
            SungTheme {
                SearchScreen(CatalogState(error = SourceFailure.NETWORK, inCollection = true), null,
                    CatalogActions({}, {}, {}, {}, {}, { backs++ }, { retries++ }, {}))
            }
        }
        compose.onNodeWithTag("catalog_error").assertIsDisplayed()
        compose.onNodeWithText("Try again").performClick()
        compose.onNodeWithText("Back to previous results").performClick()
        compose.runOnIdle { assertEquals(1, retries); assertEquals(1, backs) }
    }

    @Test fun collectionRowOpensItsActualProviderIdentity() {
        val item = CatalogItem("album-id", CatalogKind.ALBUM, "A real mapped title", "Artist", "https://example.test")
        var opened: CatalogItem? = null
        compose.setContent {
            SungTheme {
                SearchScreen(CatalogState(page = CatalogPage("Albums", listOf(item))), null,
                    CatalogActions({}, {}, {}, { opened = it }, {}, {}, {}, {}))
            }
        }
        compose.onNodeWithTag("result_0").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(item, opened) }
    }

    @Test fun unavailableArtistOffersSongSearchWithoutRetryLoop() {
        var query = ""
        val page = CatalogPage("John Michael Howell", emptyList(),
            notice = CatalogNotice.ArtistBrowseUnavailable("John Michael Howell"))
        compose.setContent {
            SungTheme {
                SearchScreen(CatalogState(page = page, inCollection = true), null,
                    CatalogActions({}, {}, {}, {}, {}, {}, {}, { query = it }))
            }
        }
        compose.onNodeWithTag("artist_notice").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("catalog_error").assertDoesNotExist()
        compose.onNodeWithTag("artist_search_songs").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("John Michael Howell", query) }
    }

    @Test fun categoryClearAndSubmitKeepTheirExistingCallbacks() {
        var state by mutableStateOf(CatalogState(query = "https://youtu.be/RB-RcX5DS5A"))
        var submits = 0
        compose.setContent {
            SungTheme {
                SearchScreen(state, null, CatalogActions(
                    { state = state.copy(query = it) }, { state = state.copy(kind = it) },
                    { submits++ }, {}, {}, {}, {}, {}))
            }
        }
        compose.onNodeWithTag("filter_album").performClick()
        compose.runOnIdle { assertEquals(CatalogKind.ALBUM, state.kind) }
        compose.onNodeWithTag("search_submit").performClick()
        compose.runOnIdle { assertEquals(1, submits) }
        compose.onNodeWithContentDescription("Clear search").performClick()
        compose.runOnIdle { assertEquals("", state.query) }
        compose.onNodeWithTag("search_submit").assertIsNotEnabled()
    }

    @Test fun songResultKeepsLikeQueueAndCurrentTrackCue() {
        val song = CatalogItem("RB-RcX5DS5A", CatalogKind.SONG, "The Scientist", "Coldplay", "https://youtu.be/RB-RcX5DS5A")
        var likes = 0
        var next: CatalogItem? = null
        var queued: CatalogItem? = null
        compose.setContent {
            SungTheme {
                CompositionLocalProvider(LocalLibraryUi provides LibraryUi(toggle = { likes++ })) {
                    SearchScreen(CatalogState(page = CatalogPage("Songs", listOf(song))), song.id,
                        CatalogActions({}, {}, {}, {}, {}, {}, {}, {},
                            playNext = { next = it }, append = { queued = it }), playing = true)
                }
            }
        }
        compose.onNodeWithContentDescription("Playing").assertExists()
        compose.onNodeWithTag("search_like_${song.id}").performScrollTo().performClick()
        compose.onNodeWithTag("search_actions_${song.id}").performClick()
        compose.onNodeWithText("Play next").performClick()
        compose.onNodeWithTag("search_actions_${song.id}").performClick()
        compose.onNodeWithText("Add to queue").performClick()
        compose.runOnIdle { assertEquals(1, likes); assertEquals(song, next); assertEquals(song, queued) }
        val output = File("build/outputs/search-ui/search-results.png").apply { parentFile!!.mkdirs() }
        compose.onRoot().captureRoboImage(output.absolutePath)
    }

    @Test fun reducedMotionStillShowsSearchLoading() {
        compose.setContent {
            SungTheme(reduceMotion = true) {
                SearchScreen(CatalogState(loading = true), null,
                    CatalogActions({}, {}, {}, {}, {}, {}, {}, {}))
            }
        }
        compose.onNodeWithTag("catalog_loading").performScrollTo()
        compose.onNodeWithText("Searching…").assertIsDisplayed()
    }

    @Test fun darkLargeTextKeepsSearchAndResultReachable() {
        val song = CatalogItem("RB-RcX5DS5A", CatalogKind.SONG, "The Scientist", "Coldplay", "https://youtu.be/RB-RcX5DS5A")
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                SungTheme(ThemeMode.DARK) { Surface(Modifier.fillMaxSize()) {
                    SearchScreen(CatalogState(query = "Coldplay", page = CatalogPage("Songs", listOf(song))),
                        null, CatalogActions({}, {}, {}, {}, {}, {}, {}, {}))
                } }
            }
        }
        compose.onNodeWithTag("search_input").assertIsDisplayed()
        compose.onNodeWithTag("search_submit").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("result_0").performScrollTo().assertIsDisplayed()
        val output = File("build/outputs/search-ui/search-dark-large-text.png").apply { parentFile!!.mkdirs() }
        compose.onRoot().captureRoboImage(output.absolutePath)
    }
}
