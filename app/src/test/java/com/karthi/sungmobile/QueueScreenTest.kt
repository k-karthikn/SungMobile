package com.karthi.sungmobile

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.captureRoboImage
import com.karthi.sungmobile.app.PlayerActions
import com.karthi.sungmobile.core.designsystem.theme.SungTheme
import com.karthi.sungmobile.core.model.*
import com.karthi.sungmobile.core.playback.PlaybackState
import com.karthi.sungmobile.feature.queue.QueueScreen
import com.karthi.sungmobile.feature.player.PlayerScreen
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w393dp-h852dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class QueueScreenTest {
    @get:Rule val compose = createComposeRule()
    private val song = CatalogItem("song", CatalogKind.SONG, "Same song twice", "Artist", "https://www.youtube.com/watch?v=song")
    private val entries = listOf(QueueEntry("first", song), QueueEntry("second", song))
    private val state = PlaybackState(track = song, queue = QueueState(entries, "first"), canUndo = true)

    @Test fun artworkPreviewsUseQueueOccurrencesAndHideMissingNeighbors() {
        var jumped: String? = null
        val third = QueueEntry("third", song.copy(id = "other", title = "Other song"))
        val queue = QueueState(listOf(third, entries[1], entries[0]), "second", shuffled = true)
        compose.setContent { SungTheme { PlayerScreen(state.copy(queue = queue),
            PlayerActions({}, {}, {}, jump = { jumped = it }), {}) } }
        compose.onNodeWithContentDescription("Previous: Other song").performClick()
        assertEquals("third", jumped)
        compose.onNodeWithTag("artwork_next").performClick()
        assertEquals("first", jumped)
    }
    @Test fun firstOccurrenceHasOnlyNextPreviewAndMoreRetainsStop() {
        var stopped = false
        compose.setContent { SungTheme { PlayerScreen(state, PlayerActions({}, {}, { stopped = true }), {}) } }
        compose.onNodeWithTag("artwork_previous").assertDoesNotExist()
        compose.onNodeWithTag("artwork_next").assertExists()
        compose.onNodeWithTag("player_more").performClick()
        compose.onNodeWithTag("stop").performScrollTo().performClick()
        assertTrue(stopped)
    }
    @Test fun advancedOptionsRemainActionableInsideTheMoreSheet() {
        var smart = false
        var related = false
        compose.setContent { SungTheme { PlayerScreen(state, PlayerActions({}, {}, {}), {}, moreContent = {
            com.karthi.sungmobile.feature.convenience.PlayerAdvancedControls(song,
                com.karthi.sungmobile.core.data.settings.AdvancedPlaybackSettings(), false,
                { smart = it }, { related = it }, { _, _ -> })
        }) } }
        compose.onNodeWithTag("player_more").performClick()
        compose.onNodeWithContentDescription("Smart shuffle").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Related autoplay").performScrollTo().performClick()
        assertTrue(smart)
        assertTrue(related)
        compose.onNodeWithTag("stop").performScrollTo().assertIsDisplayed()
        val file = File("build/outputs/player-ui/more-options.png").apply { parentFile?.mkdirs() }
        compose.onRoot().captureRoboImage(file.absolutePath)
    }
    @Test fun duplicateSongMenuActsOnTheChosenOccurrence() {
        var removed: String? = null
        compose.setContent { SungTheme { QueueScreen(state, PlayerActions({}, {}, {}, remove = { removed = it }), {}) } }
        compose.onNodeWithTag("queue_options_1").performClick()
        compose.onNodeWithText("Remove").performClick()
        assertEquals("second", removed)
    }
    @Test fun largeTextKeepsQueueActionsReachable() {
        var undone = false
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(2f, 2f)) {
                SungTheme { QueueScreen(state, PlayerActions({}, {}, {}, undo = { undone = true }), {}) }
            }
        }
        compose.onNodeWithTag("queue_undo").performClick()
        assertTrue(undone)
        compose.onNodeWithTag("queue_list").performScrollToNode(hasTestTag("queue_options_1"))
        compose.onNodeWithTag("queue_options_1").assertIsDisplayed()
        val file = File("build/outputs/player-ui/queue-large-text.png").apply { parentFile.mkdirs() }
        compose.onRoot().captureRoboImage(file.absolutePath)
    }
    @Test fun playerQueueAndNextControlsAreReachable() {
        var opened = false
        var next = false
        compose.setContent { SungTheme { PlayerScreen(state, PlayerActions({}, {}, {}, next = { next = true }), {}, openQueue = { opened = true }) } }
        compose.onNodeWithTag("next_track").performScrollTo().performClick()
        assertTrue(next)
        compose.onNodeWithTag("open_queue").performScrollTo().performClick()
        assertTrue(opened)
        val file = File("build/outputs/player-ui/player-controls.png").apply { parentFile.mkdirs() }
        compose.onRoot().captureRoboImage(file.absolutePath)
    }
}
