package com.karthi.sungmobile.feature.library

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.captureRoboImage
import com.karthi.sungmobile.core.designsystem.theme.SungTheme
import com.karthi.sungmobile.core.designsystem.theme.ThemeMode
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.CatalogKind
import com.karthi.sungmobile.core.model.library.*
import java.io.File
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w393dp-h852dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ListeningStatisticsUiTest {
    @get:Rule val compose = createComposeRule()
    private fun track(index: Int) = CatalogItem("track$index", CatalogKind.SONG,
        listOf("her (feat. ZVC)", "Pookkal Pookkum", "Story of a Warrior", "Missing Piece", "Another song", "Last song")[index],
        "Artist", "https://www.youtube.com/watch?v=RB-RcX5DS5A")
    private val stats get() = ListeningStats(qualifiedPlays = 37, listenedMs = 4_740_000,
        uniqueSongs = 12,
        topSongs = (0 until 6).map { TopSong(track(it), 10 - it, (19 - it).toLong() * 60_000) },
        recentEvents = (0 until 7).map { index ->
            ListeningEvent("event$index", track(index % 6),
                System.currentTimeMillis() - index * 86_400_000L,
                System.currentTimeMillis() - index * 86_400_000L, 120_000)
        },
        dailyActivity = listOf(DailyListening(LocalDate.now().minusDays(1).toString(), 4_560_000, 5),
            DailyListening(LocalDate.now().toString(), 180_000, 1)))

    @Test fun overviewAndFiltersContainOnlyStatisticsActions() {
        compose.setContent { SungTheme { ListeningStatisticsScreen(
            AdvancedLibraryState(page = AdvancedPage.STATISTICS, stats = stats), {}, {}, {}) } }
        compose.onAllNodesWithText("1h 19m").assertCountEquals(2)
        compose.onNodeWithText("Your week").assertDoesNotExist()
        compose.onNodeWithText("37 qualified plays").assertExists()
        compose.onNodeWithTag("stats_total").assertIsDisplayed()
        compose.onNodeWithTag("stats_activity").assertIsDisplayed()
        compose.onNodeWithTag("stats_activity_total").assertTextEquals("1h 19m")
        compose.onAllNodesWithText("1h16m").assertCountEquals(2)
        capture("statistics-overview")
        compose.onNodeWithTag("stats_30_days").performClick()
        compose.onNodeWithText("in the last 30 days").assertExists()
        compose.onNodeWithTag("stats_activity_total").assertTextEquals("1h 19m")
        compose.onNodeWithTag("listening_statistics").performScrollToKey("top_songs")
        compose.onAllNodesWithText("her (feat. ZVC)").onFirst().assertExists()
        compose.onNodeWithTag("stats_top_1").assertIsDisplayed()
        compose.onNodeWithTag("stats_top_2").assertIsDisplayed()
        compose.onNodeWithTag("stats_top_3").assertIsDisplayed()
        compose.onAllNodesWithContentDescription("Play", substring = true).assertCountEquals(0)
        compose.onAllNodesWithContentDescription("Options", substring = true).assertCountEquals(0)
        capture("statistics-light")
    }

    @Test fun darkLargeTextAndPrivateModeRemainReadable() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.8f)) {
                SungTheme(ThemeMode.DARK) { ListeningStatisticsScreen(
                    AdvancedLibraryState(page = AdvancedPage.STATISTICS, stats = stats), {}, {}, {}) }
            }
        }
        compose.onNodeWithText("Listening statistics").assertIsDisplayed()
        compose.onNodeWithTag("listening_statistics").performScrollToKey("activity")
        compose.onNodeWithTag("stats_7_days").assertIsDisplayed()
        capture("statistics-dark-large")
    }

    @Test fun activityWindowIncludesZeroDaysAndBothEndpoints() {
        val today = LocalDate.parse("2026-10-25")
        val daily = listOf(DailyListening("2026-10-19", 1_000, 1),
            DailyListening("2026-10-25", 2_000, 2))
        val seven = activityWindow(daily, 7, today)
        assertEquals(7, seven.size)
        assertEquals("2026-10-19", seven.first().day)
        assertEquals(0L, seven[1].listenedMs)
        assertEquals(2_000L, seven.last().listenedMs)
        assertEquals(30, activityWindow(daily, 30, today).size)
    }

    @Test fun privateModeDoesNotShowListeningRecords() {
        compose.setContent { SungTheme { ListeningStatisticsScreen(
            AdvancedLibraryState(page = AdvancedPage.STATISTICS, stats = stats.copy(privateMode = true)),
            {}, {}, {}) } }
        compose.onNodeWithText("Statistics are hidden").assertExists()
        compose.onNodeWithText("Top songs").assertDoesNotExist()
    }

    private fun capture(name: String) {
        val file = File("build/outputs/statistics-ui/$name.png").apply { parentFile?.mkdirs() }
        compose.onRoot().captureRoboImage(file.absolutePath)
    }
}
