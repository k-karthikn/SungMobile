package com.karthi.sungmobile.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.R
import com.karthi.sungmobile.core.designsystem.icon.*
import com.karthi.sungmobile.core.designsystem.theme.SungSpacing
import com.karthi.sungmobile.core.model.CatalogKind
import com.karthi.sungmobile.core.playback.PlaybackState
import com.karthi.sungmobile.feature.appearance.AppearanceScreen
import com.karthi.sungmobile.feature.appearance.SpotifySettingsScreen
import com.karthi.sungmobile.feature.feedback.FeedbackScreen
import com.karthi.sungmobile.feature.home.HomeScreen
import com.karthi.sungmobile.feature.library.LibraryScreen
import com.karthi.sungmobile.feature.player.*
import com.karthi.sungmobile.feature.search.SearchScreen
import com.karthi.sungmobile.feature.lyrics.*
import com.karthi.sungmobile.core.data.settings.LyricsSettings
import kotlinx.coroutines.delay

private enum class Overlay { NONE, PLAYER, APPEARANCE, SPOTIFY, FEEDBACK, HISTORY, COLLECTION, LYRICS, SESSIONS }

/** Navigation belongs to the UI. Playback is observed from the service across destination changes. */
@Composable
fun SungApp(catalog: CatalogState, playback: PlaybackState, catalogActions: CatalogActions,
    playerActions: PlayerActions, appearance: Appearance, onAppearance: (Appearance) -> Unit, settingsError: Boolean = false,
    libraryContent: (@Composable (Modifier, () -> Unit, () -> Unit, () -> Unit) -> Unit)? = null,
    historyContent: (@Composable (Modifier, () -> Unit) -> Unit)? = null,
    privateMode: Boolean = false, historyError: Boolean = false,
    recentSearches: @Composable () -> Unit = {},
    hasRecentSearches: Boolean = false,
    detailId: String? = null, closeDetail: () -> Unit = {},
    collectionContent: (@Composable (Modifier, () -> Unit) -> Unit)? = null,
    homeContent: (@Composable (Modifier, () -> Unit, () -> Unit, () -> Unit, () -> Unit) -> Unit)? = null,
    searchRequest: Int = 0,
    lyricsState: LyricsUiState = LyricsUiState(), lyricsSettings: LyricsSettings = LyricsSettings(),
    onLoadLyrics: () -> Unit = {}, onRetryLyrics: () -> Unit = {}, onLyricsSettings: (LyricsSettings) -> Unit = {},
    onCancelLyrics: () -> Unit = {},
    onLyricsOffset: (Int) -> Unit = {}, onClearLyricsCache: () -> Unit = {},
    backupContent: @Composable () -> Unit = {},
    spotifyContent: @Composable () -> Unit = {},
    recommendationsContent: @Composable () -> Unit = {},
    resumeContent: @Composable (PlaybackState) -> Unit = {},
    playerMoreContent: @Composable (PlaybackState, () -> Unit) -> Unit = { _, _ -> },
    volumeContent: @Composable (PlaybackState) -> Unit = {},
    sessionsContent: @Composable (PlaybackState, () -> Unit) -> Unit = { _, _ -> },
    statisticsPageOpen: Boolean = false,
    livePlayback: kotlinx.coroutines.flow.StateFlow<PlaybackState>? = null) {
    val screenState = rememberSaveableStateHolder()
    var destination by rememberSaveable { mutableStateOf(Destination.HOME) }
    var overlay by rememberSaveable { mutableStateOf(Overlay.NONE) }
    var sessionsFromPlayer by rememberSaveable { mutableStateOf(false) }
    var miniVolume by rememberSaveable { mutableStateOf(false) }
    var miniQueueRequest by remember { mutableStateOf(false) }
    var frozenBackdropUrl by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(appearance.ambientBackdrop, appearance.backdropFollowsMusic, playback.track?.artworkUrl) {
        if (!appearance.ambientBackdrop || appearance.backdropFollowsMusic || frozenBackdropUrl == null) {
            frozenBackdropUrl = playback.track?.artworkUrl
        }
    }
    LaunchedEffect(playback.track?.id) { miniVolume = false }
    if (miniVolume && playback.track != null) AlertDialog(onDismissRequest = { miniVolume = false },
        title = { Text("Sound control") }, text = { volumeContent(playback) },
        confirmButton = { TextButton({ miniVolume = false }) { Text("Close") } })
    DisposableEffect(detailId) {
        val key = detailId?.let { "collection:$it" }
        onDispose { key?.let(screenState::removeState) }
    }
    var shownDetail by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(detailId) {
        if (detailId == shownDetail) return@LaunchedEffect
        shownDetail = detailId
        if (detailId != null) overlay = Overlay.COLLECTION
        else if (overlay == Overlay.COLLECTION) overlay = Overlay.NONE
    }
    LaunchedEffect(searchRequest) { if (searchRequest > 0) { destination = Destination.SEARCH; overlay = Overlay.NONE } }
    LaunchedEffect(playback.track, overlay) {
        if (playback.track == null && (overlay == Overlay.PLAYER || overlay == Overlay.LYRICS)) overlay = Overlay.NONE
    }
    val lyricsVisible = overlay == Overlay.PLAYER || overlay == Overlay.LYRICS
    LaunchedEffect(lyricsVisible, playback.track?.id, lyricsSettings.lrclibEnabled, privateMode) {
        if (lyricsVisible && playback.track != null) {
            onCancelLyrics()
            if (overlay == Overlay.PLAYER) delay(300)
            onLoadLyrics()
        } else onCancelLyrics()
    }
    val back = {
        when {
            overlay == Overlay.LYRICS -> overlay = Overlay.PLAYER
            overlay == Overlay.FEEDBACK -> overlay = Overlay.APPEARANCE
            overlay == Overlay.SESSIONS -> overlay = if (sessionsFromPlayer && playback.track != null) Overlay.PLAYER else Overlay.NONE
            overlay == Overlay.HISTORY -> overlay = Overlay.APPEARANCE
            overlay == Overlay.SPOTIFY -> overlay = Overlay.APPEARANCE
            overlay == Overlay.COLLECTION -> { closeDetail(); overlay = Overlay.NONE }
            overlay == Overlay.PLAYER && detailId != null -> overlay = Overlay.COLLECTION
            overlay != Overlay.NONE -> overlay = Overlay.NONE
            destination == Destination.SEARCH && catalog.inCollection -> catalogActions.back()
            else -> destination = Destination.HOME
        }
    }
    BackHandler(overlay != Overlay.NONE || destination != Destination.HOME, back)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 840.dp
        val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
        Row(Modifier.fillMaxSize()) {
            if (wide && overlay == Overlay.NONE && !statisticsPageOpen) {
                NavigationRail(Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical))) {
                    Destination.entries.forEach { target ->
                        NavigationRailItem(destination == target, { destination = target },
                            icon = { SungIcon(if (destination == target) target.selectedIcon else target.icon) },
                            label = { Text(stringResource(target.label)) }, modifier = Modifier.testTag("nav_${target.name.lowercase()}"))
                    }
                }
            }
            Scaffold(Modifier.weight(1f).imePadding(), contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    if (((overlay == Overlay.NONE && !statisticsPageOpen) ||
                        (overlay == Overlay.COLLECTION && playback.track != null)) && !keyboardVisible) {
                        Column(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(horizontal = 12.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            MiniPlayer(playback, playerActions, { overlay = Overlay.PLAYER },
                                { overlay = Overlay.LYRICS },
                                { miniQueueRequest = true; overlay = Overlay.PLAYER },
                                { miniVolume = true },
                                Modifier.widthIn(max = SungSpacing.contentMaxWidth), livePlayback)
                            if (!wide && overlay == Overlay.NONE) {
                                SungNavigationBar(destination) { destination = it }
                            }
                        }
                    }
                }) { insets ->
                Box(Modifier.fillMaxSize().padding(insets), contentAlignment = Alignment.TopCenter) {
                    val content = Modifier.widthIn(max = SungSpacing.contentMaxWidth).fillMaxSize()
                    Column(content) {
                        com.karthi.sungmobile.feature.updates.UpdateReadyBanner { overlay = Overlay.APPEARANCE }
                        if (!playback.connected) {
                            Row(Modifier.padding(horizontal = 16.dp)) {
                                Text(stringResource(R.string.player_connecting), Modifier.weight(1f))
                                TextButton(playerActions.reconnect) { Text(stringResource(R.string.retry)) }
                            }
                        }
                        if (privateMode) Text("Private mode is on • Queue restoration still saves", Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.labelMedium)
                        if (historyError || playback.historyError) Text("History could not be saved. Please retry the action.", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
                        if (playback.storageError || settingsError) Text(stringResource(R.string.storage_error),
                            Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
                        when (overlay) {
                            Overlay.COLLECTION -> screenState.SaveableStateProvider("collection:$detailId") { collectionContent?.invoke(Modifier.weight(1f)) { overlay = Overlay.PLAYER } }
                            Overlay.PLAYER -> LivePlaybackContent(playback, livePlayback) { live ->
                                PlayerScreen(live, playerActions, back, Modifier.weight(1f),
                                    { }, { overlay = Overlay.LYRICS },
                                    ambientBackdrop = appearance.ambientBackdrop,
                                    backdropArtworkUrl = if (appearance.backdropFollowsMusic) live.track?.artworkUrl else frozenBackdropUrl,
                                    expandQueueRequest = miniQueueRequest,
                                    onQueueRequestHandled = { miniQueueRequest = false },
                                    lyricsState = lyricsState,
                                    moreContent = { playerMoreContent(live) { sessionsFromPlayer = true; overlay = Overlay.SESSIONS } }) {
                                    resumeContent(live)
                                }
                            }
                            Overlay.LYRICS -> LivePlaybackContent(playback, livePlayback) { live ->
                                LyricsScreen(live, lyricsState, lyricsSettings, back, onRetryLyrics,
                                    playerActions.seek, onLyricsOffset, Modifier.weight(1f),
                                    onEnableOnline = { onLyricsSettings(lyricsSettings.copy(lrclibEnabled = true)) })
                            }
                            Overlay.SESSIONS -> LivePlaybackContent(playback, livePlayback) { live -> sessionsContent(live, back) }
                            Overlay.HISTORY -> historyContent?.invoke(content, back)
                            Overlay.FEEDBACK -> FeedbackScreen(back, content)
                            Overlay.APPEARANCE -> AppearanceScreen(appearance, onAppearance, back, content,
                                lyricsSettings, onLyricsSettings, lyricsState.cacheBytes, onClearLyricsCache,
                                onHistory = { overlay = Overlay.HISTORY },
                                onFeedback = { overlay = Overlay.FEEDBACK }, backupContent = backupContent,
                                onSpotify = { overlay = Overlay.SPOTIFY },
                                recommendationsContent = recommendationsContent)
                            Overlay.SPOTIFY -> SpotifySettingsScreen(back, content) { spotifyContent() }
                            Overlay.NONE -> when (destination) {
                                Destination.HOME -> screenState.SaveableStateProvider("home") { if (homeContent != null) homeContent(Modifier.weight(1f),
                                    { destination = Destination.SEARCH }, { overlay = Overlay.PLAYER }, { overlay = Overlay.APPEARANCE }, { destination = Destination.LIBRARY })
                                    else HomeScreen(playback.track, catalog.page?.items.orEmpty(), catalog.inCollection,
                                    { destination = Destination.SEARCH }, { overlay = Overlay.PLAYER }, { item ->
                                        catalogActions.open(item)
                                        if (item.kind != CatalogKind.SONG) destination = Destination.SEARCH
                                    }, { overlay = Overlay.APPEARANCE }, content) }
                                Destination.SEARCH -> screenState.SaveableStateProvider("search") {
                                    SearchScreen(catalog, playback.track?.id, catalogActions, Modifier.weight(1f),
                                        playback.connected, recentSearches, hasRecentSearches, playback.engine.playing)
                                }
                                Destination.LIBRARY -> Column(Modifier.weight(1f)) {
                                    if (libraryContent == null) TextButton({ sessionsFromPlayer = false; overlay = Overlay.SESSIONS },
                                        Modifier.padding(horizontal = 16.dp)) { Text("Saved sessions") }
                                    if (libraryContent != null) libraryContent(Modifier.weight(1f),
                                        { sessionsFromPlayer = false; overlay = Overlay.SESSIONS },
                                        { overlay = Overlay.APPEARANCE }, { destination = Destination.SEARCH })
                                    else Text("Your local library is ready for liked songs and playlists.", Modifier.padding(24.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
