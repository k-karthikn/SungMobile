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
import com.karthi.sungmobile.feature.library.*
import com.karthi.sungmobile.navigation.*
import com.karthi.sungmobile.playback.PlaybackConnection

/** Application composition only: feature state and work remain in their own ViewModels. */
@Composable
fun SungRoot(window: Window, container: AppContainer, model: SungViewModel, browser: LibraryBrowserViewModel,
    libraryActions: LibraryActionsViewModel, history: HistoryViewModel, home: HomeViewModel,
    collection: CollectionViewModel, collectionActions: CollectionActionsViewModel, links: LinkIntakeViewModel) {
    val catalog by model.catalogState.collectAsStateWithLifecycle()
    val player by model.player.state.collectAsStateWithLifecycle()
    val preferences by container.appearance.state.collectAsStateWithLifecycle()
    val settingsError by container.appearance.error.collectAsStateWithLifecycle()
    val library by browser.page.collectAsStateWithLifecycle()
    val policy by history.policy.collectAsStateWithLifecycle()
    val historyError by history.error.collectAsStateWithLifecycle()
    val searches by history.searches.collectAsStateWithLifecycle()
    val homeState by home.state.collectAsStateWithLifecycle()
    val details by collection.state.collectAsStateWithLifecycle()
    val operation by collectionActions.state.collectAsStateWithLifecycle()
    val saveBatch by collectionActions.save.collectAsStateWithLifecycle()
    val intake by links.state.collectAsStateWithLifecycle()
    var searchRequest by remember { mutableIntStateOf(0) }
    LaunchedEffect(details.route?.id) { collectionActions.cancel() }
    SungTheme(preferences.theme, preferences.colors, preferences.reduceMotion) {
        val view = LocalView.current
        val light = MaterialTheme.colorScheme.surface.luminance() > .5f
        SideEffect { WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = light; isAppearanceLightNavigationBars = light
        } }
        LibraryHost(container.library, libraryActions, player) {
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
                libraryContent = { modifier, openHistory -> LibraryScreen(browser, library, libraryActions, openHistory, modifier) },
                historyContent = { modifier, back -> HistoryScreen(policy, history, back, modifier) },
                privateMode = policy?.privateMode == true, historyError = historyError,
                recentSearches = { RecentSearches(searches, { model.recall(it.query, it.kind) }, history::removeSearch) },
                detailId = details.route?.id, closeDetail = collection::close,
                collectionContent = { modifier, showPlayer -> CollectionScreen(details, collection, collectionActions, model.player,
                    { model.searchArtistSongs(it); searchRequest++ }, showPlayer, modifier) },
                homeContent = { modifier, search, nowPlaying, appearance, showLibrary ->
                    DiscoveryHomeScreen(homeState, home::refresh, model.player, search, nowPlaying, appearance,
                        { browser.open(it); showLibrary() }, modifier)
                }, searchRequest = searchRequest)
            CollectionOperationDialog(operation, collectionActions)
            LinkIntakeDialog(intake, links::dismiss) { link -> collection.open(CollectionRoute(link.url, link.label, startMs = link.startMs)) }
        }
    }
}
