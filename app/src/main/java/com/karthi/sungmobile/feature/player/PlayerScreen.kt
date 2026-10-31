package com.karthi.sungmobile.feature.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.R
import com.karthi.sungmobile.app.PlayerActions
import com.karthi.sungmobile.core.designsystem.artwork.MaterialArtworkFade
import com.karthi.sungmobile.core.designsystem.icon.*
import com.karthi.sungmobile.core.playback.PlaybackState
import com.karthi.sungmobile.feature.lyrics.LyricsUiState
import kotlinx.coroutines.launch
import coil3.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(state: PlaybackState, actions: PlayerActions, onBack: () -> Unit, modifier: Modifier = Modifier,
    openQueue: () -> Unit = {}, openLyrics: () -> Unit = {},
    ambientBackdrop: Boolean = true, backdropArtworkUrl: String? = null,
    expandQueueRequest: Boolean = false, onQueueRequestHandled: () -> Unit = {},
    lyricsState: LyricsUiState = LyricsUiState(),
    moreContent: @Composable () -> Unit = {},
    resumeContent: @Composable () -> Unit = {}) {
    var more by remember { mutableStateOf(false) }
    if (more) PlayerMoreSheet(actions, { more = false }, moreContent)
    val scope = rememberCoroutineScope()
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.PartiallyExpanded,
        enabledValues = setOf(SheetValue.PartiallyExpanded, SheetValue.Expanded))
    LaunchedEffect(expandQueueRequest) {
        if (expandQueueRequest) {
            sheetState.expand()
            onQueueRequestHandled()
        }
    }
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)
    val expanded = sheetState.currentValue == SheetValue.Expanded
    BackHandler(expanded) { scope.launch { sheetState.partialExpand() } }
    BoxWithConstraints(modifier.fillMaxSize()) {
        val sheetHeight = (maxHeight - 24.dp).coerceAtLeast(200.dp)
        val horizontalPadding = if (maxWidth < 360.dp) 16.dp else 24.dp
        val heroHeight = (maxHeight - PlayerQueuePeekHeight - 16.dp).coerceAtLeast(400.dp)
        BottomSheetScaffold(scaffoldState = scaffoldState, sheetPeekHeight = PlayerQueuePeekHeight,
            sheetDragHandle = null, sheetShape = MaterialTheme.shapes.extraLarge,
            sheetContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            containerColor = if (ambientBackdrop && backdropArtworkUrl != null) Color.Transparent
                else MaterialTheme.colorScheme.surface,
            sheetContent = {
                PlayerQueueSheet(state, actions, expanded, onToggle = {
                    scope.launch {
                        if (expanded) sheetState.partialExpand()
                        else { openQueue(); sheetState.expand() }
                    }
                }, onCollapse = { scope.launch { sheetState.partialExpand() } },
                    modifier = Modifier.height(sheetHeight))
            }, modifier = Modifier.fillMaxSize()) { insets ->
            val scrollState = rememberScrollState()
            LaunchedEffect(state.queue.currentId) { scrollState.scrollTo(0) }
            Column(Modifier.fillMaxSize().padding(insets).verticalScroll(scrollState)
                .background(MaterialTheme.colorScheme.surface)) {
                Box(Modifier.fillMaxWidth().height(heroHeight).clipToBounds()) {
                    if (ambientBackdrop && backdropArtworkUrl != null) {
                        Box(Modifier.matchParentSize().background(MaterialTheme.colorScheme.surface)) {
                            AsyncImage(model = backdropArtworkUrl, contentDescription = null,
                                modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop)
                            // Keep the artwork and its Material 3 fade in the scrolling hero.
                            // Scrolling lyrics therefore carries the fade upward with the image.
                            MaterialArtworkFade(Modifier.matchParentSize())
                        }
                    }
                    Column(
                        Modifier.fillMaxSize().padding(horizontal = horizontalPadding, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            FilledTonalIconButton(onBack, Modifier.size(48.dp)) {
                                SungIcon(SungSymbol.Back, stringResource(R.string.close_player))
                            }
                            Text(stringResource(R.string.now_playing),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.weight(1f))
                            FilledTonalIconButton({ more = true }, Modifier.size(48.dp).testTag("player_more")) {
                                SungIcon(SungSymbol.More, "More playback options")
                            }
                        }
                        PlayerSwipeArea(state, actions.previous, actions.next,
                            Modifier.fillMaxWidth().weight(1f))
                        PlayerTrackDetails(state)
                        PlayerSeek(state, actions.seek)
                        PlayerTransport(state, actions)
                        resumeContent()
                    }
                }
                Column(Modifier.fillMaxWidth().padding(horizontal = horizontalPadding),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Spacer(Modifier.height(144.dp))
                    PlayerLyricsPreview(state, lyricsState, openLyrics)
                    Text(stringResource(R.string.background_notice), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 24.dp))
                }
            }
        }
    }
}
