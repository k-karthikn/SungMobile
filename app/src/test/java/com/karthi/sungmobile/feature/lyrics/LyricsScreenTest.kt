package com.karthi.sungmobile.feature.lyrics

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.graphics.Color
import com.karthi.sungmobile.core.data.settings.LyricsSettings
import com.karthi.sungmobile.core.designsystem.theme.SungTheme
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.CatalogKind
import com.karthi.sungmobile.core.model.lyrics.LyricLine
import com.karthi.sungmobile.core.model.lyrics.LyricsDocument
import com.karthi.sungmobile.core.model.lyrics.LyricsOrigin
import com.karthi.sungmobile.core.playback.EngineSnapshot
import com.karthi.sungmobile.core.playback.PlaybackState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w393dp-h852dp-xhdpi")
class LyricsScreenTest {
    @get:Rule val compose = createComposeRule()

    private val track = CatalogItem("song", CatalogKind.SONG, "Sample song", "Artist",
        "https://www.youtube.com/watch?v=RB-RcX5DS5A")
    private val lines = (0..30).map { LyricLine(it * 1_000L, "Line $it") }

    @Test fun activeTimedLineStaysNearViewportTop() {
        var position by mutableLongStateOf(15_000L)
        compose.setContent {
            SungTheme(reduceMotion = true) {
                LyricsScreen(PlaybackState(track = track,
                    engine = EngineSnapshot(positionMs = position, durationMs = 31_000, seekable = true)),
                    LyricsUiState(track.id, document = LyricsDocument(LyricsOrigin.YOUTUBE,
                        "Test provider", lines = lines)), LyricsSettings(), {}, {}, {}, {})
            }
        }
        fun assertAtTop(index: Int) {
            compose.waitForIdle()
            val viewport = compose.onNodeWithTag("timed_lyrics_list").getUnclippedBoundsInRoot()
            val active = compose.onNodeWithText("Line $index").getUnclippedBoundsInRoot()
            assertTrue("Active lyric top ${active.top} should be near ${viewport.top}",
                kotlin.math.abs(active.top.value - viewport.top.value) < 60f)
        }
        assertAtTop(15)
        compose.runOnIdle { position = 0L }
        assertAtTop(0)
        compose.runOnIdle { position = 30_000L }
        assertAtTop(30)
    }

    @Test fun currentLineAccentPreservesWordsAndProviderCreditIsNotDuplicated() {
        val original = "Darlin', I'll love you"
        val styled = emphasizedLyric(original, Color.Blue, Color.White)
        assertEquals(original, styled.text)
        assertEquals(listOf("Darlin',", "I'll", "love", "you"), styled.spanStyles.map {
            styled.text.substring(it.start, it.end)
        })
        assertEquals(2, styled.spanStyles.count { it.item.color == Color.Blue })
        assertTrue(styled.spanStyles.map { it.item.fontFamily }.distinct().size >= 3)
        assertEquals(0, emphasizedLyric("తెలుగు", Color.Blue, Color.White).spanStyles.size)
        assertEquals(0, emphasizedLyric("Love தமிழ்", Color.Blue, Color.White).spanStyles.size)
        assertTrue(supportsPosterFace("I love you!"))
        assertTrue(!supportsPosterFace("मेरा दिल"))
        assertEquals("Source: LyricFind", lyricsSourceLabel("Source: LyricFind", "Artist"))
        assertEquals("Artist", lyricsSourceLabel(null, "Artist"))
    }

    @Test fun offsetControlsKeepTapToSeekAccurate() {
        var offset by mutableIntStateOf(0)
        var sought = -1L
        compose.setContent {
            SungTheme {
                LyricsScreen(PlaybackState(track = track,
                    engine = EngineSnapshot(positionMs = 8_000, durationMs = 31_000, seekable = true)),
                    LyricsUiState(track.id, document = LyricsDocument(LyricsOrigin.YOUTUBE,
                        "Test provider", lines = lines), offsetMs = offset), LyricsSettings(), {}, {},
                    { sought = it }, { offset = it })
            }
        }
        compose.onNodeWithContentDescription("Increase timing offset by 0.25 seconds").performClick()
        compose.runOnIdle { assertEquals(250, offset) }
        compose.onNodeWithTag("timed_lyrics_list").performTouchInput { swipeUp() }
        compose.onNodeWithText("Return to current line").assertExists().performClick()
        compose.onNodeWithText("Line 9").performClick()
        compose.runOnIdle { assertEquals(8_750L, sought) }
    }
}
