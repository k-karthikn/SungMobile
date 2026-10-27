package com.karthi.sungmobile.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.karthi.sungmobile.core.designsystem.theme.SungTheme
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.CatalogKind
import com.karthi.sungmobile.core.model.lyrics.LyricLine
import com.karthi.sungmobile.core.model.lyrics.LyricsDocument
import com.karthi.sungmobile.core.model.lyrics.LyricsOrigin
import com.karthi.sungmobile.core.playback.EngineSnapshot
import com.karthi.sungmobile.core.playback.PlaybackState
import com.karthi.sungmobile.feature.lyrics.LyricsUiState
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
class PlayerLyricsPreviewTest {
    @get:Rule val compose = createComposeRule()
    private val track = CatalogItem("song", CatalogKind.SONG, "Song", "Artist",
        "https://www.youtube.com/watch?v=RB-RcX5DS5A")

    @Test fun cardOpensFullLyricsAndDisappearsForMissingOrStaleLyrics() {
        val playback = PlaybackState(track = track, engine = EngineSnapshot(positionMs = 1_000))
        val document = LyricsDocument(LyricsOrigin.YOUTUBE, "Test", lines = listOf(
            LyricLine(0, "Current line"), LyricLine(2_000, "Next line"),
            LyricLine(4_000, "Later line")))
        var lyrics by mutableStateOf(LyricsUiState(track.id, document = document))
        var opened = 0
        compose.setContent {
            SungTheme {
                Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).padding(24.dp)) {
                    PlayerLyricsPreview(playback, lyrics) { opened++ }
                }
            }
        }
        compose.onNodeWithTag("lyrics_preview").assertExists()
        compose.onNodeWithTag("lyrics_preview_line_0").assertExists()
        compose.onNodeWithTag("show_lyrics").performClick()
        assertEquals(1, opened)
        val file = File("build/outputs/player-ui/lyrics-preview.png").apply { parentFile?.mkdirs() }
        compose.onRoot().captureRoboImage(file.absolutePath)

        compose.runOnIdle { lyrics = LyricsUiState(track.id) }
        compose.onNodeWithTag("lyrics_preview").assertDoesNotExist()
        compose.runOnIdle { lyrics = LyricsUiState("another-song", document = document) }
        compose.onNodeWithTag("lyrics_preview").assertDoesNotExist()
    }
}
