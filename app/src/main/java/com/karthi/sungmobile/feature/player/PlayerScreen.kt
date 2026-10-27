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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.R
import com.karthi.sungmobile.app.PlayerActions
import com.karthi.sungmobile.core.designsystem.icon.*
import com.karthi.sungmobile.core.playback.PlaybackState
import com.karthi.sungmobile.feature.lyrics.LyricsUiState
import kotlinx.coroutines.launch
import coil3.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(state: PlaybackState, actions: PlayerActions, onBack: () -> Unit, modifier: Modifier = Modifier,
    openQueue: () -> Unit = {}, openLyrics: () -> Unit = {},
    ambientBackdrop: Boolean = false, backdropArtworkUrl: String? = null,
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
        if (ambientBackdrop && backdropArtworkUrl != null) {
            Box(Modifier.matchParentSize().clipToBounds().background(MaterialTheme.colorScheme.surface)) {
                AsyncImage(model = backdropArtworkUrl, contentDescription = null,
                    modifier = Modifier.matchParentSize().blur(48.dp), contentScale = ContentScale.Crop)
                Box(Modifier.matchParentSize().background(MaterialTheme.colorScheme.surface.copy(alpha = .78f)))
            }
        }
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
            Column(Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState())
                .padding(horizontal = horizontalPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onBack, Modifier.size(48.dp)) { SungIcon(SungSymbol.Back, stringResource(R.string.close_player)) }
                    Text(stringResource(R.string.now_playing), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    IconButton({ more = true }, Modifier.testTag("player_more")) { SungIcon(SungSymbol.More, "More playback options") }
                }
                PlayerArtwork(state, actions.jump, Modifier.widthIn(max = 420.dp).fillMaxWidth().align(Alignment.CenterHorizontally))
                PlayerTrackDetails(state)
                PlayerSeek(state, actions.seek)
                PlayerTransport(state, actions)
                resumeContent()
                PlayerLyricsPreview(state, lyricsState, openLyrics)
                Text(stringResource(R.string.background_notice), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 24.dp))
            }
        }
    }
}
