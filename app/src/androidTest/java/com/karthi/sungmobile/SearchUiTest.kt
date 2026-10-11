package com.karthi.sungmobile

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.karthi.sungmobile.core.model.*
import com.karthi.sungmobile.app.CatalogState
import com.karthi.sungmobile.app.CatalogActions
import com.karthi.sungmobile.feature.search.SearchScreen
import com.karthi.sungmobile.core.designsystem.theme.SungTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SearchUiTest {
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
}
