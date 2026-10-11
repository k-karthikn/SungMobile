package com.karthi.sungmobile

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/** Requires the explicit instrumentation argument live=true and a networked device. */
class StageZeroDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun searchPlayPauseSeekSwitchAndBackgroundLifecycle() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("live") == "true")
        compose.onNodeWithTag("nav_search").performClick()
        compose.onNodeWithTag("search_input").performTextInput("Coldplay")
        compose.onNodeWithTag("search_submit").performClick()
        compose.waitUntil(60_000) { exists("result_0") }
        compose.onNodeWithTag("result_0").performScrollTo().performClick()
        compose.waitUntil(10_000) { exists("mini_player") }
        compose.onNodeWithTag("mini_player").performClick()
        compose.waitUntil(60_000) { text("player_status") == "Playing" }
        compose.waitUntil(15_000) { position() >= 2 }
        val firstTitle = text("player_title")
        compose.onNodeWithTag("play_pause").performClick()
        compose.waitUntil(5_000) { text("player_status") == "Paused" }
        val pausedAt = position()
        compose.onNodeWithTag("seek_forward").performClick()
        compose.waitUntil(5_000) { position() >= pausedAt + 9 }
        compose.onNodeWithTag("play_pause").performClick()
        compose.waitUntil(15_000) { text("player_status") == "Playing" }
        compose.onNodeWithContentDescription("Close player").performClick()
        compose.onNodeWithTag("catalog_list").performScrollToNode(hasTestTag("result_1"))
        compose.onNodeWithTag("result_1").performClick()
        compose.onNodeWithTag("mini_player").performClick()
        compose.waitUntil(60_000) { text("player_status") == "Playing" && text("player_title") != firstTitle }
        compose.waitUntil(15_000) { position() >= 2 }
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.waitUntil(5_000) { text("player_status") == "Playing" }
        val secondTitle = text("player_title")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("player_title").assertTextEquals(secondTitle)
        compose.onNodeWithTag("player_status").assertTextEquals("Playing")
        compose.onNodeWithTag("stop").performScrollTo().performClick()
        compose.onNodeWithTag("player_status").assertTextEquals("Paused")
    }

    private fun exists(tag: String) = compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    private fun text(tag: String): String = compose.onAllNodesWithTag(tag).fetchSemanticsNodes()
        .firstOrNull()?.config?.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text }.orEmpty()
    private fun position(): Int {
        val parts = text("player_position").split(':').mapNotNull(String::toIntOrNull)
        return if (parts.size == 2) parts[0] * 60 + parts[1] else 0
    }
}
