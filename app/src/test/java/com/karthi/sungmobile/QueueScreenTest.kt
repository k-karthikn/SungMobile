package com.karthi.sungmobile

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.karthi.sungmobile.app.PlayerActions
import com.karthi.sungmobile.core.designsystem.theme.SungTheme
import com.karthi.sungmobile.core.model.*
import com.karthi.sungmobile.core.playback.PlaybackState
import com.karthi.sungmobile.feature.queue.QueueScreen
import com.karthi.sungmobile.feature.queue.queueArtist
import com.karthi.sungmobile.feature.convenience.PlayerPlaybackOptions
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

    @Test fun nowPlayingUsesArtworkBackdropWithoutQueueArtworkCards() {
        compose.setContent { SungTheme { PlayerScreen(state, PlayerActions({}, {}, {}), {}) } }
        compose.onNodeWithTag("artwork_previous").assertDoesNotExist()
        compose.onNodeWithTag("artwork_next").assertDoesNotExist()
        compose.onNodeWithTag("player_swipe_area").assertExists()
    }
    @Test fun queueSwipeAreaSkipsToNextAndMoreRetainsStop() {
        var stopped = false
        var movedNext = false
        val firstQueue = QueueState(listOf(QueueEntry("first", song), QueueEntry("second", song)), "first")
        compose.setContent { SungTheme { PlayerScreen(state.copy(queue = firstQueue),
            PlayerActions({}, {}, { stopped = true }, next = { movedNext = true }), {}) } }
        compose.onNodeWithTag("player_swipe_area").performTouchInput { swipeLeft() }
        assertTrue(movedNext)
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
    @Test fun speedAndSleepTimerAppearInPlayerMoreMenu() {
        var speed: PlaybackSpeed? = null
        compose.setContent { SungTheme {
            PlayerScreen(state, PlayerActions({}, {}, {}), {}, moreContent = {
                PlayerPlaybackOptions(state, { speed = it }, { _, _, _ -> }, {})
            })
        } }
        compose.onNodeWithText("Playback").assertDoesNotExist()
        compose.onNodeWithText("Speed 1.0×").assertDoesNotExist()
        compose.onNodeWithText("Sleep timer").assertDoesNotExist()

        compose.onNodeWithTag("player_more").performClick()
        compose.onNodeWithText("Playback").assertDoesNotExist()
        compose.onNodeWithText("Speed 1.0×").performClick()
        compose.onNodeWithText("Apply").performClick()
        assertEquals(PlaybackSpeed(), speed)
        compose.onNodeWithText("Sleep timer").assertIsDisplayed()
    }
    @Test fun duplicateSongMenuActsOnTheChosenOccurrence() {
        var removed: String? = null
        compose.setContent { SungTheme { QueueScreen(state, PlayerActions({}, {}, {}, remove = { removed = it }), {}) } }
        compose.onNodeWithTag("queue_options_1").performClick()
        compose.onNodeWithText("Remove").performClick()
        assertEquals("second", removed)
    }
    @Test fun queueRowsStayCompactAndCleanUpPreviouslySavedArtistRuns() {
        assertEquals("ARJN, KDS, FIFTY4", queueArtist("ARJN · , · KDS · , · FIFTY4 · ,"))
        assertEquals("KR\$NA & Dhanda Nyoliwala", queueArtist("KR\$NA · & · Dhanda Nyoliwala · 203M plays"))
        val tracks = (0 until 8).map { index ->
            QueueEntry("row$index", song.copy(id = "song$index", title = "KALYANI (Remix) $index",
                subtitle = "ARJN · , · KDS · , · FIFTY4 · ,", durationMs = 235_000))
        }
        compose.setContent { SungTheme { QueueScreen(state.copy(queue = QueueState(tracks, "row0")),
            PlayerActions({}, {}, {}), {}, embedded = true) } }
        compose.onAllNodesWithText("ARJN, KDS, FIFTY4").onFirst().assertExists()
        compose.onAllNodesWithText("3:55").onFirst().assertExists()
        compose.onNodeWithTag("queue_entry_7").performScrollTo().assertIsDisplayed()
        val file = File("build/outputs/player-ui/queue-compact.png").apply { parentFile.mkdirs() }
        compose.onRoot().captureRoboImage(file.absolutePath)
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
        compose.onNodeWithTag("open_queue").performClick()
        assertTrue(opened)
        val file = File("build/outputs/player-ui/player-controls.png").apply { parentFile.mkdirs() }
        compose.onRoot().captureRoboImage(file.absolutePath)
    }

    @Test fun longTrackTitlesDoNotMoveSeekOrNextControls() {
        var displayed by mutableStateOf(state)
        compose.setContent { SungTheme { PlayerScreen(displayed, PlayerActions({}, {}, {}), {}) } }
        val nextBefore = compose.onNodeWithTag("next_track").fetchSemanticsNode().boundsInRoot.top
        val seekBefore = compose.onNodeWithTag("seek_slider").fetchSemanticsNode().boundsInRoot.top

        compose.runOnIdle {
            displayed = displayed.copy(track = song.copy(
                title = "A very long song title with many more words than the player can fit on two lines, " +
                    "even on a wide screen, followed by another long phrase",
                subtitle = "A very long artist and channel credit that should not wrap below the title"))
        }
        compose.waitForIdle()
        compose.onNodeWithTag("player_title").assertTextContains("A very long song title", substring = true)
        assertEquals(seekBefore, compose.onNodeWithTag("seek_slider").fetchSemanticsNode().boundsInRoot.top, .5f)
        assertEquals(nextBefore, compose.onNodeWithTag("next_track").fetchSemanticsNode().boundsInRoot.top, .5f)
    }

    @Test fun queuePeeksOnPlayerAndExpandsWithSwipe() {
        compose.setContent { SungTheme { PlayerScreen(state, PlayerActions({}, {}, {}), {}) } }
        compose.onNodeWithTag("open_queue").assertHeightIsEqualTo(72.dp)
        compose.onNodeWithTag("open_queue").assertIsDisplayed().performTouchInput { swipeUp() }
        compose.onNodeWithTag("queue_now_playing").assertIsDisplayed()
        compose.onNodeWithTag("queue_options_1").assertIsDisplayed()
        compose.onNodeWithTag("open_queue").performTouchInput { swipeDown() }
        compose.onNodeWithTag("queue_now_playing").assertIsNotDisplayed()
        compose.onNodeWithTag("open_queue").performClick()
        compose.onNodeWithTag("queue_now_playing").assertIsDisplayed()
    }

    @Test fun expressiveTransportKeepsRepeatAndShuffleActions() {
        var repeat: RepeatMode? = null
        var shuffle: Boolean? = null
        compose.setContent { SungTheme { PlayerScreen(state, PlayerActions({}, {}, {},
            repeat = { repeat = it }, shuffle = { shuffle = it }), {}) } }
        compose.onNodeWithTag("repeat_toggle").performScrollTo().performClick()
        compose.onNodeWithTag("shuffle_toggle").performScrollTo().performClick()
        assertEquals(RepeatMode.ALL, repeat)
        assertEquals(true, shuffle)
    }
}
