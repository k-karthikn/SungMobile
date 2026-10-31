package com.karthi.sungmobile.app

import android.view.Window
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.karthi.sungmobile.core.designsystem.theme.SungTheme
import com.karthi.sungmobile.feature.collection.*
import com.karthi.sungmobile.feature.history.*
import com.karthi.sungmobile.feature.home.*
import com.karthi.sungmobile.core.model.CatalogKind
import com.karthi.sungmobile.feature.library.*
import com.karthi.sungmobile.feature.local.LocalSongsContent
import com.karthi.sungmobile.feature.download.DownloadHost
import com.karthi.sungmobile.navigation.*
import com.karthi.sungmobile.playback.PlaybackConnection
import com.karthi.sungmobile.feature.lyrics.LyricsViewModel
import com.karthi.sungmobile.feature.convenience.*
import com.karthi.sungmobile.feature.backup.BackupSettingsSection
import com.karthi.sungmobile.feature.appearance.SpotifyConnectionSettingsSection
import com.karthi.sungmobile.feature.spotify.SpotifyLibraryImportViewModel
import com.karthi.sungmobile.core.model.discovery.RecommendationPreferences
import kotlinx.coroutines.launch

/** Application composition only: feature state and work remain in their own ViewModels. */
@Composable
fun SungRoot(window: Window, container: AppContainer, model: SungViewModel, browser: LibraryBrowserViewModel,
    libraryActions: LibraryActionsViewModel, advancedLibrary: AdvancedLibraryViewModel,
    history: HistoryViewModel, home: HomeViewModel, discovery: DiscoveryFeedViewModel,
    collection: CollectionViewModel, collectionActions: CollectionActionsViewModel, links: LinkIntakeViewModel,
    lyrics: LyricsViewModel, convenience: ConvenienceViewModel,
    spotifyLibraryImport: SpotifyLibraryImportViewModel) {
    val catalog by model.catalogState.collectAsStateWithLifecycle()
    val chrome = remember(model.player) { model.player.state.chromeUpdates() }
    val initialPlayback = remember(model.player) { model.player.state.value.withoutClock() }
    val player by chrome.collectAsStateWithLifecycle(initialPlayback)
    val preferences by container.appearance.state.collectAsStateWithLifecycle()
    val settingsError by container.appearance.error.collectAsStateWithLifecycle()
    val library by browser.page.collectAsStateWithLifecycle()
    val advancedLibraryState by advancedLibrary.state.collectAsStateWithLifecycle()
    val policy by history.policy.collectAsStateWithLifecycle()
    val historyError by history.error.collectAsStateWithLifecycle()
    val searches by history.searches.collectAsStateWithLifecycle()
    val homeState by home.state.collectAsStateWithLifecycle()
    val discoveryState by discovery.state.collectAsStateWithLifecycle()
    val details by collection.state.collectAsStateWithLifecycle()
    val operation by collectionActions.state.collectAsStateWithLifecycle()
    val saveBatch by collectionActions.save.collectAsStateWithLifecycle()
    val intake by links.state.collectAsStateWithLifecycle()
    val lyricsState by lyrics.state.collectAsStateWithLifecycle()
    val lyricsSettings by lyrics.preferences.collectAsStateWithLifecycle()
    val convenienceState by convenience.state.collectAsStateWithLifecycle()
    val advanced by container.settings.advancedPlayback.collectAsStateWithLifecycle(
        initialValue = com.karthi.sungmobile.core.data.settings.AdvancedPlaybackSettings())
    val recommendations by container.settings.recommendations.collectAsStateWithLifecycle(
        initialValue = RecommendationPreferences())
    val settingsScope = rememberCoroutineScope()
    var searchRequest by remember { mutableIntStateOf(0) }
    LaunchedEffect(details.route?.id) { collectionActions.cancel() }
    LaunchedEffect(player.track?.id, player.engine.durationMs) {
        convenience.loadResume(player.track, player.engine.durationMs)
    }
    SungTheme(preferences.theme, preferences.colors, preferences.reduceMotion,
        preferences.colorScheme, preferences.accentColor, preferences.useArtworkAccent,
        player.track?.artworkUrl) {
        val view = LocalView.current
        val light = MaterialTheme.colorScheme.surface.luminance() > .5f
        SideEffect { WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = light; isAppearanceLightNavigationBars = light
        } }
        DownloadHost { LibraryHost(container.library, libraryActions, player) {
            val libraryUi = LocalLibraryUi.current
            LaunchedEffect(saveBatch) { saveBatch?.let { libraryUi.add(it); collectionActions.savedHandled() } }
            SungApp(catalog, player,
                CatalogActions(model::editQuery, model::chooseKind, model::search, model::open, model::loadMore,
                    collection::close, model::retry, model::searchArtistSongs,
                    { model.player.enqueue(it, true) }, { model.player.enqueue(it, false) }, model::setActive),
                PlayerActions(model.player::toggle, model.player::seekTo, model.player::stop,
                    model.player::next, model.player::previous, model.player::setRepeat, model.player::setShuffle,
                    model.player::jump, model.player::remove, model.player::move, model.player::clearUpcoming,
                    model.player::undo, { (model.player as PlaybackConnection).connect() }),
                preferences, container.appearance::update, settingsError,
                libraryContent = { modifier, openSessions, openSettings, openSearch ->
                    LibraryScreen(browser, library, libraryActions, advancedLibrary, advancedLibraryState,
                        openSettings, modifier, openSessions, openSearch, homeState, player.connected,
                        refreshMixes = home::refresh,
                        localContent = { localModifier ->
                            LocalSongsContent(container.localAudio, player.connected, model.player::select,
                                model.player::enqueue, localModifier)
                        })
                },
                historyContent = { modifier, back -> HistoryScreen(policy, history, back, modifier) },
                privateMode = policy?.privateMode == true, historyError = historyError,
                recentSearches = { RecentSearches(searches, { model.recall(it.query, it.kind) }, history::removeSearch) },
                hasRecentSearches = searches.isNotEmpty(),
                detailId = details.route?.id, closeDetail = collection::close,
                collectionContent = { modifier, showPlayer -> CollectionScreen(details, collection, collectionActions, model.player,
                    { model.searchArtistSongs(it); searchRequest++ }, showPlayer, modifier) },
                homeContent = { modifier, search, nowPlaying, appearance, showLibrary ->
                    DiscoveryHomeScreen(discoveryState, discovery::refresh, discovery::ensureFresh,
                        search, appearance, showLibrary,
                        { advancedLibrary.openStatistics(); showLibrary() },
                        { query -> model.recall(query, CatalogKind.SONG); searchRequest++ },
                        { shelf, item ->
                            if (item.kind == CatalogKind.SONG) {
                                val songs = shelf.items.filter { it.kind == CatalogKind.SONG }
                                val index = songs.indexOfFirst { it.id == item.id }
                                if (index >= 0) model.player.select(songs, index)
                            } else collection.open(CollectionRoute(item.url, item.title, item.kind, artworkUrl = item.artworkUrl))
                        }, player.connected, modifier,
                        onRecommendationFeedback = discovery::setRecommendationFeedback,
                        onHomeChipSelected = discovery::selectHomeChip,
                        artistSignals = homeState.data,
                        onArtistSelected = { artist -> model.searchArtistSongs(artist); searchRequest++ })
                }, searchRequest = searchRequest,
                lyricsState = lyricsState, lyricsSettings = lyricsSettings,
                onLoadLyrics = { lyrics.load(player.track, policy?.privateMode == true) },
                onCancelLyrics = lyrics::cancel,
                onRetryLyrics = { lyrics.load(player.track, policy?.privateMode == true, refresh = true) },
                onLyricsSettings = lyrics::setPreferences,
                onLyricsOffset = { player.track?.let { track -> lyrics.setOffset(track, it) } },
                onClearLyricsCache = lyrics::clearCache,
                backupContent = { BackupSettingsSection(container.backup) },
                spotifyContent = { SpotifyConnectionSettingsSection(container.spotifySession,
                    container.spotifySessionValidator, spotifyLibraryImport) },
                recommendationsContent = {
                    RecommendationSettingsSection(recommendations,
                        { enabled -> settingsScope.launch { container.settings.setRecommendationsEnabled(enabled) } },
                        { settingsScope.launch { container.settings.clearRecommendationFeedback() } })
                },
                resumeContent = { state ->
                    PlayerResumeControls(state, convenienceState.resume,
                        convenience::resume, convenience::startOver)
                },
                playerMoreContent = { state, openSessions ->
                    val connection = model.player as PlaybackConnection
                    androidx.compose.foundation.layout.Column {
                        PlayerPlaybackOptions(state, connection::setSpeed,
                            connection::armTimer, connection::cancelTimer)
                        androidx.compose.material3.TextButton(openSessions) { androidx.compose.material3.Text("Saved sessions") }
                        PlayerAdvancedControls(state.track, advanced, policy?.privateMode == true,
                            { enabled -> settingsScope.launch { container.settings.setSmartShuffle(enabled) } },
                            { enabled -> settingsScope.launch { container.settings.setRelatedAutoplay(enabled) } },
                            { id, db -> settingsScope.launch { container.settings.setTrackTrim(id, db) } })
                    }
                },
                volumeContent = { state ->
                    TrackVolumeControl(state.track, advanced) { id, db ->
                        settingsScope.launch { container.settings.setTrackTrim(id, db) }
                    }
                },
                sessionsContent = { state, back ->
                    SessionsScreen(convenienceState, state, back, convenience::save, convenience::rename,
                        convenience::delete, convenience::restore, convenience::clearMessage)
                }, statisticsPageOpen = advancedLibraryState.page == AdvancedPage.STATISTICS,
                livePlayback = model.player.state)
            CollectionOperationDialog(operation, collectionActions)
            LinkIntakeDialog(intake, links::dismiss) { link -> collection.open(CollectionRoute(link.url, link.label, startMs = link.startMs)) }
        } }
    }
}
