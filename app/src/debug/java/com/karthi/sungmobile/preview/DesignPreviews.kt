package com.karthi.sungmobile.preview

import android.content.res.Configuration
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.karthi.sungmobile.app.*
import com.karthi.sungmobile.core.designsystem.theme.SungTheme
import com.karthi.sungmobile.core.model.*
import com.karthi.sungmobile.core.playback.*
import com.karthi.sungmobile.feature.player.PlayerScreen

/** Android Studio-only preview fixtures. Nothing is injected into the running catalog. */
private val previewTrack = CatalogItem("preview", CatalogKind.SONG, "A favorite song", "Artist name", "")
private val previewPlayer = PlaybackState(track = previewTrack,
    engine = EngineSnapshot(positionMs = 102_000, durationMs = 268_000, seekable = true))
private val catalogActions = CatalogActions({}, {}, {}, {}, {}, {}, {}, {})
private val playerActions = PlayerActions({}, {}, {})

@Preview(name = "Home · Light", widthDp = 393, heightDp = 852)
@Preview(name = "Home · Dark", widthDp = 393, heightDp = 852, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Home · Large text", widthDp = 393, heightDp = 852, fontScale = 2f)
@Composable
private fun HomePreview() {
    SungTheme {
        SungApp(CatalogState(), PlaybackState(), catalogActions, playerActions, Appearance(), {})
    }
}

@Preview(name = "Player · Light", widthDp = 393, heightDp = 852)
@Preview(name = "Player · Dark", widthDp = 393, heightDp = 852, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Player · Large text", widthDp = 393, heightDp = 852, fontScale = 2f)
@Composable
private fun PlayerPreview() {
    SungTheme { Surface { PlayerScreen(previewPlayer, playerActions, {}) } }
}
