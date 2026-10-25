package com.karthi.sungmobile.feature.lyrics

import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import com.karthi.sungmobile.R
import com.karthi.sungmobile.core.data.settings.LyricsSettings
import com.karthi.sungmobile.core.designsystem.component.SungLoading
import com.karthi.sungmobile.core.model.lyrics.*
import com.karthi.sungmobile.core.playback.PlaybackState
import com.karthi.sungmobile.core.designsystem.theme.LocalSungMotionEnabled
import kotlinx.coroutines.flow.first

@Composable
fun LyricsScreen(playback: PlaybackState, state: LyricsUiState, settings: LyricsSettings,
    onBack: () -> Unit, onRetry: () -> Unit, onSeek: (Long) -> Unit, onOffset: (Int) -> Unit,
    modifier: Modifier = Modifier, onEnableOnline: () -> Unit = {}) {
    val track = playback.track
    val document = state.document?.takeIf { state.videoId == track?.id }
    val timed = document?.lines.orEmpty()
    val current = currentLyricLine(timed, playback.engine.positionMs, state.offsetMs)
    var follow by remember(track?.id) { mutableStateOf(true) }
    val list = rememberLazyListState()
    val motionEnabled = LocalSungMotionEnabled.current
    val dragged by list.interactionSource.collectIsDraggedAsState()
    LaunchedEffect(dragged) { if (dragged) follow = false }
    LaunchedEffect(current, follow) { if (follow && current >= 0) {
        val viewport = snapshotFlow { list.layoutInfo.viewportSize.height }.first { it > 0 }
        val offset = -(viewport / 3)
        if (motionEnabled) list.animateScrollToItem(current, offset) else list.scrollToItem(current, offset)
    } }
    Column(modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LyricsTopBar(document?.synchronized, onBack)
        LyricsTrackCard(track, document?.attribution)
        if (timed.isNotEmpty()) {
            LyricsTimingCard(state.offsetMs, follow, onOffset, { follow = true })
        }
        when {
            state.loading || (state.videoId != track?.id && track != null) ->
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    SungLoading(Modifier.testTag("lyrics_loading"), "Loading lyrics…")
                }
            document == null -> {
                Surface(shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Column(Modifier.fillMaxWidth().padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(state.message ?: stringResource(R.string.lyrics_missing), style = MaterialTheme.typography.bodyLarge)
                        if (track?.isLocalSong() == true && !settings.lrclibEnabled)
                            FilledTonalButton(onEnableOnline) { Text("Search LRCLIB online") }
                        FilledTonalButton(onRetry) { Text(stringResource(R.string.retry)) }
                    }
                }
            }
            timed.isNotEmpty() -> BoxWithConstraints(Modifier.weight(1f)) {
                val breathingRoom = maxHeight / 3
                LazyColumn(state = list, modifier = Modifier.fillMaxSize().testTag("timed_lyrics_list"),
                    contentPadding = PaddingValues(vertical = breathingRoom),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(timed, key = { index, _ -> index }) { index, line ->
                        TimedLyricRow(line, index, current, settings, playback.engine.seekable,
                            state.offsetMs, playback.engine.durationMs, onSeek)
                    }
                }
            }
            else -> LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 24.dp)) {
                item { PlainLyrics(document.plainText.orEmpty(), settings) }
            }
        }
    }
}
