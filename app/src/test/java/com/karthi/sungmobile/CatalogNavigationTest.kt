package com.karthi.sungmobile

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import com.karthi.sungmobile.app.*
import com.karthi.sungmobile.core.designsystem.theme.SungTheme
import com.karthi.sungmobile.core.model.*
import com.karthi.sungmobile.core.playback.PlaybackState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w393dp-h852dp-xhdpi")
class CatalogNavigationTest {
    @get:Rule val compose = createComposeRule()
    @Test fun backRestoresSearchScrollAndRecreationRetainsDetailRoute() {
        val restoration = StateRestorationTester(compose)
        var detail by mutableStateOf<String?>(null)
        var searchActive = false
        val rows = (0..30).map { CatalogItem("$it", CatalogKind.ALBUM, "Album $it", "Artist", "https://example.test/$it") }
        val catalog = CatalogState(query = "Albums", kind = CatalogKind.ALBUM, page = CatalogPage("Albums", rows))
        restoration.setContent {
            SungTheme {
                SungApp(catalog, PlaybackState(),
                    CatalogActions({}, {}, {}, { detail = it.id }, {}, {}, {}, {}, active = { searchActive = it }),
                    PlayerActions({}, {}, {}), Appearance(), {}, detailId = detail, closeDetail = { detail = null },
                    collectionContent = { _, _ -> TextButton({ detail = null }) { Text("Close detail $detail") } })
            }
        }
        compose.onNodeWithTag("nav_search").performClick()
        compose.onNodeWithTag("catalog_list").performScrollToNode(hasTestTag("result_20"))
        compose.onNodeWithTag("result_20").performClick()
        compose.onNodeWithText("Close detail 20").assertIsDisplayed()
        compose.runOnIdle { assertFalse(searchActive) }
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Close detail 20").assertIsDisplayed().performClick()
        compose.onNodeWithTag("result_20").assertIsDisplayed()
        compose.runOnIdle { assertTrue(searchActive) }
    }
}
