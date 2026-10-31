package com.karthi.sungmobile.feature.lyrics

import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import android.os.SystemClock
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import com.karthi.sungmobile.R
import com.karthi.sungmobile.core.data.settings.LyricsSettings
import com.karthi.sungmobile.core.designsystem.component.SungLoading
import com.karthi.sungmobile.core.model.lyrics.*
import com.karthi.sungmobile.core.playback.PlaybackState
import com.karthi.sungmobile.core.designsystem.theme.LocalSungMotionEnabled
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LyricsScreen(playback: PlaybackState, state: LyricsUiState, settings: LyricsSettings,
    onBack: () -> Unit, onRetry: () -> Unit, onSeek: (Long) -> Unit, onOffset: (Int) -> Unit,
    modifier: Modifier = Modifier, onEnableOnline: () -> Unit = {}) {
    val track = playback.track
    val document = state.document?.takeIf { state.videoId == track?.id }
    val timed = document?.lines.orEmpty()
    var lyricPositionMs by remember(track?.id) { mutableLongStateOf(playback.engine.positionMs) }
    var anchorPositionMs by remember(track?.id) { mutableLongStateOf(playback.engine.positionMs) }
    var anchorElapsedMs by remember(track?.id) { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var clockRunning by remember(track?.id) { mutableStateOf(false) }
    var playbackRate by remember(track?.id) { mutableFloatStateOf(playback.speed.multiplier) }
    LaunchedEffect(track?.id, playback.engine.positionMs, playback.engine.playing,
        playback.engine.buffering, playback.speed.multiplier) {
        anchorPositionMs = playback.engine.positionMs
        anchorElapsedMs = SystemClock.elapsedRealtime()
        clockRunning = playback.engine.playing && !playback.engine.buffering
        playbackRate = playback.speed.multiplier
        lyricPositionMs = playback.engine.positionMs
    }
    LaunchedEffect(track?.id) {
        while (true) {
            kotlinx.coroutines.delay(40)
            lyricPositionMs = if (clockRunning) {
                anchorPositionMs + ((SystemClock.elapsedRealtime() - anchorElapsedMs) * playbackRate).toLong()
            } else anchorPositionMs
        }
    }
    val current = currentLyricLine(timed, lyricPositionMs, state.offsetMs)
    var follow by remember(track?.id) { mutableStateOf(true) }
    var showShareDialog by remember(track?.id) { mutableStateOf(false) }
    var sharingCard by remember(track?.id) { mutableStateOf(false) }
    var shareError by remember(track?.id) { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val list = rememberLazyListState()
    val motionEnabled = LocalSungMotionEnabled.current
    val dragged by list.interactionSource.collectIsDraggedAsState()
    LaunchedEffect(dragged) { if (dragged) follow = false }
    LaunchedEffect(current, follow, timed, settings.textSizeSp) { if (follow && current >= 0) {
        if (motionEnabled) list.animateScrollToItem(current) else list.scrollToItem(current)
    } }
    Column(modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LyricsTopBar(document?.synchronized, document?.wordSynchronized, onBack)
        LyricsTrackCard(track, document?.attribution)
        if (document != null && track != null) {
            FilledTonalButton(
                onClick = { showShareDialog = true; shareError = null },
                modifier = Modifier.fillMaxWidth().testTag("lyrics_share_card"),
                enabled = !sharingCard,
            ) { Text("Create lyric share card") }
        }
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
                            FilledTonalButton(onEnableOnline) { Text("Search online lyrics") }
                        FilledTonalButton(onRetry) { Text(stringResource(R.string.retry)) }
                    }
                }
            }
            timed.isNotEmpty() -> BoxWithConstraints(Modifier.weight(1f)) {
                LazyColumn(state = list, modifier = Modifier.fillMaxSize().testTag("timed_lyrics_list"),
                    // Trailing space lets even the final line reach the top of the viewport.
                    contentPadding = PaddingValues(top = 12.dp, bottom = maxHeight),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(timed, key = { index, _ -> index }) { index, line ->
                        TimedLyricRow(line, index, current, settings, playback.engine.seekable,
                            state.offsetMs, playback.engine.durationMs, lyricPositionMs, onSeek)
                    }
                }
            }
            else -> LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 24.dp)) {
                item { PlainLyrics(document.plainText.orEmpty(), settings) }
            }
        }
    }
    if (showShareDialog && document != null && track != null) {
        LyricShareDialog(
            initialQuote = lyricShareQuote(document, current),
            trackTitle = track.title,
            sharing = sharingCard,
            error = shareError,
            onDismiss = { if (!sharingCard) showShareDialog = false },
            onShare = { quote ->
                scope.launch {
                    sharingCard = true
                    shareError = null
                    try {
                        val file = withContext(Dispatchers.IO) {
                            renderLyricShareCard(context, quote, track.title, track.subtitle, document.attribution)
                        }
                        context.startActivity(lyricCardChooser(context, file, track.title))
                        showShareDialog = false
                    } catch (_: Exception) {
                        shareError = "Could not create the image. Please try again."
                    } finally {
                        sharingCard = false
                    }
                }
            },
        )
    }
}
