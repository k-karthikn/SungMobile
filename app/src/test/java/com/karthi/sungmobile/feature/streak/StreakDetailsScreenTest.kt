package com.karthi.sungmobile.feature.streak

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.karthi.sungmobile.core.designsystem.theme.SungTheme
import com.karthi.sungmobile.core.model.library.calculateStreakProgress
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w393dp-h852dp-xhdpi")
class StreakDetailsScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun showsStreakMetricsAndCalendar() {
        val today = LocalDate.of(2026, 11, 11)
        val progress = calculateStreakProgress(setOf(today.minusDays(2), today.minusDays(1), today), today)
        compose.setContent { SungTheme { StreakDetailsScreen(
            StreakUiState(progress, loading = false), {}, {}, today = today) } }
        compose.onNodeWithText("Listening streak").assertIsDisplayed()
        compose.onNodeWithTag("streak_hero").assertIsDisplayed()
        compose.onNodeWithTag("streak_calendar").assertExists()
        compose.onNodeWithTag("streak_current_count").assertExists()
        compose.onNodeWithTag("streak_month_reset")
            .assertContentDescriptionEquals("November 2026. Return to current month")
        compose.onNodeWithContentDescription("11, today, active, streak day 3").assertExists()
        compose.onNodeWithTag("streak_month_next").assertIsNotEnabled()
    }

    @Test fun privateModeDoesNotShowCalendar() {
        compose.setContent { SungTheme { StreakDetailsScreen(
            StreakUiState(com.karthi.sungmobile.core.model.library.StreakProgress(privateMode = true), loading = false), {}, {}) } }
        compose.onNodeWithText("Your streak is private").assertExists()
        compose.onNodeWithTag("streak_calendar").assertDoesNotExist()
    }

    @Test fun monthArrowsMoveOneMonthAndNextIsDisabledAtCurrentMonth() {
        val today = LocalDate.of(2026, 11, 11)
        compose.setContent { SungTheme { StreakDetailsScreen(
            StreakUiState(loading = false), {}, {}, today = today) } }

        compose.onNodeWithTag("streak_month_previous").performClick()
        compose.onNodeWithTag("streak_month_reset")
            .assertContentDescriptionEquals("October 2026. Return to current month")
        compose.onNodeWithTag("streak_month_next").assertIsEnabled().performClick()
        compose.onNodeWithTag("streak_month_reset")
            .assertContentDescriptionEquals("November 2026. Return to current month")
        compose.onNodeWithTag("streak_month_next").assertIsNotEnabled()
    }

    @Test fun horizontalSwipeChangesMonthAndHeaderTapReturnsToCurrentMonth() {
        val today = LocalDate.of(2026, 11, 11)
        compose.setContent { SungTheme { StreakDetailsScreen(
            StreakUiState(loading = false), {}, {}, today = today) } }

        compose.onNodeWithTag("streak_calendar_month_content").performTouchInput { swipeLeft() }
        compose.onNodeWithTag("streak_month_reset")
            .assertContentDescriptionEquals("November 2026. Return to current month")

        compose.onNodeWithTag("streak_calendar_month_content").performTouchInput { swipeRight() }
        compose.waitUntil(2_000) {
            compose.onAllNodesWithContentDescription("October 2026. Return to current month")
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("streak_month_reset")
            .assertContentDescriptionEquals("October 2026. Return to current month")
        compose.onNodeWithTag("streak_month_reset")
            .assertHasClickAction()
            .performClick()
        compose.onNodeWithTag("streak_month_reset")
            .assertContentDescriptionEquals("November 2026. Return to current month")
    }
}
