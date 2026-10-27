package com.karthi.sungmobile.feature.library

import androidx.activity.compose.BackHandler
import com.karthi.sungmobile.feature.library.components.*
import com.karthi.sungmobile.core.designsystem.icon.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.karthi.sungmobile.core.designsystem.theme.LocalSungMotionEnabled
import com.karthi.sungmobile.core.designsystem.component.SungLoading
import com.karthi.sungmobile.core.model.library.*
import com.karthi.sungmobile.feature.home.HomeState

@Composable
fun LibraryScreen(browser: LibraryBrowserViewModel, page: LibraryPage, actions: LibraryActionsViewModel,
    advanced: AdvancedLibraryViewModel, advancedState: AdvancedLibraryState,
    openSettings: () -> Unit, modifier: Modifier = Modifier, openSessions: () -> Unit = {},
    openSearch: () -> Unit = {}, home: HomeState = HomeState(), connected: Boolean = true,
    localContent: @Composable (Modifier) -> Unit = {}, refreshMixes: () -> Unit = {}) {
    var mixesSelected by rememberSaveable { mutableStateOf(false) }
    var localSelected by rememberSaveable { mutableStateOf(false) }
    val smart by advanced.smartOverview.collectAsStateWithLifecycle()
    val collection = page.query.collection
    var filtersExpanded by rememberSaveable(collection?.toString()) { mutableStateOf(false) }
    LaunchedEffect(collection) { mixesSelected = false }
    if (advancedState.page != AdvancedPage.CLOSED) {
        AdvancedLibraryScreen(advanced, advancedState, modifier)
        return
    }
    var selected by remember(page.query.collection) { mutableStateOf(emptySet<String>()) }
    var selecting by remember(page.query.collection) { mutableStateOf(false) }
    var create by remember { mutableStateOf(false) }
    var remove by remember { mutableStateOf<Set<String>?>(null) }
    val playlist = collection as? LibraryCollection.Playlist
    val library = LocalLibraryUi.current
    val motion = LocalSungMotionEnabled.current
    val rows = page.songs.filter { it.rowId in selected }
    LaunchedEffect(page.songs, page.loading) {
        if (!page.loading) selected = selected.intersect(page.songs.map { it.rowId }.toSet())
    }
    BackHandler(playlist != null || selecting || mixesSelected || localSelected) {
        when {
            selecting -> { selecting = false; selected = emptySet() }
            localSelected -> localSelected = false
            mixesSelected -> { mixesSelected = false; browser.open(LibraryCollection.Likes) }
            else -> browser.open(null)
        }
    }
    if (localSelected) {
        Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LibraryHeader(LibraryDestination.LOCAL, { target ->
                if (target != LibraryDestination.LOCAL) {
                    localSelected = false
                    mixesSelected = target == LibraryDestination.MIXES
                    when (target) {
                        LibraryDestination.LIKED -> browser.open(LibraryCollection.Likes)
                        LibraryDestination.PLAYLISTS -> browser.open(null)
                        LibraryDestination.RECENTS -> browser.open(LibraryCollection.Recent)
                        LibraryDestination.SESSIONS -> openSessions()
                        else -> Unit
                    }
                }
            }, openSettings, openSearch)
            localContent(Modifier.weight(1f))
        }
        return
    }
    if (create) PlaylistNameDialog("Create playlist", dismiss = { create = false }, save = { actions.create(it) })
    remove?.let { ids -> ConfirmLibraryChange("Remove selected songs?", "Only these playlist entries will be removed.",
        { remove = null }, { playlist?.let { actions.remove(it.id, ids) }; selected = emptySet() }) }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item("library_header") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val destination = when {
                    mixesSelected -> LibraryDestination.MIXES
                    collection == LibraryCollection.Likes -> LibraryDestination.LIKED
                    collection == LibraryCollection.Recent -> LibraryDestination.RECENTS
                    else -> LibraryDestination.PLAYLISTS
                }
                LibraryHeader(destination, { target ->
                    when (target) {
                        LibraryDestination.LOCAL -> { localSelected = true; mixesSelected = false }
                        LibraryDestination.MIXES -> mixesSelected = true
                        LibraryDestination.SESSIONS -> openSessions()
                        LibraryDestination.LIKED -> { mixesSelected = false; browser.open(LibraryCollection.Likes) }
                        LibraryDestination.PLAYLISTS -> { mixesSelected = false; browser.open(null) }
                        LibraryDestination.RECENTS -> { mixesSelected = false; browser.open(LibraryCollection.Recent) }
                    }
                }, openSettings, openSearch)
                if (!mixesSelected) {
                    if (playlist != null) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            IconButton({ browser.open(null) }) { SungIcon(SungSymbol.Back, "Back to playlists") }
                            Text(page.title, style = MaterialTheme.typography.headlineSmall)
                        }
                        TextButton({ advanced.openCleanup(playlist.id) }) { Text("Cleanup & versions") }
                    }
                    if (collection == null) {
                        LibraryPlaylistControls(page.query.filter, browser::filter,
                            page.query.sort, browser::sort, filtersExpanded,
                            { filtersExpanded = !filtersExpanded }, { create = true })
                    } else {
                        LibraryListControls(page.query.filter, when (collection) {
                            LibraryCollection.Likes -> "Filter liked songs"
                            LibraryCollection.Recent -> "Filter recently played"
                            else -> "Filter this playlist"
                        }, browser::filter, page.query.sort, browser::sort, playlist != null, page.songs.isNotEmpty(),
                            { actions.play(collection, false) }, { actions.play(collection, true) },
                            { actions.enqueueCollection(collection, true) }, { actions.enqueueCollection(collection, false) },
                            { selecting = true; selected = emptySet() }, filtersExpanded,
                            { filtersExpanded = !filtersExpanded })
                    }
                    if (selecting) LibrarySelectionBar(selected.size, { selecting = false; selected = emptySet() },
                        { selected = page.songs.map { it.rowId }.toSet() },
                        { actions.like(rows.map { it.track }, it) }, { library.add(rows.map { it.track }) },
                        { actions.enqueue(rows.map { it.track }, it) }, if (playlist != null) ({ remove = selected.toSet() }) else null)
                    if (page.error) { Text("Could not load the library."); TextButton(browser::retry) { Text("Retry") } }
                    if (page.loading) SungLoading(label = "Loading library…")
                    if (!page.loading && !page.error && page.songs.isEmpty() && page.playlists.isEmpty()) {
                        Text(if (page.query.filter.isNotBlank()) "No matches. Try a different filter."
                            else "No items yet. Like songs or create a playlist to save them here.",
                            Modifier.padding(vertical = 24.dp), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
        if (mixesSelected) item("mixes") {
            LibraryMixesContent(home, smart, connected, actions, refreshMixes, advanced::openSmart,
                advanced::openSmartList)
        }
        if (!mixesSelected) items(page.playlists, key = { "playlist:${it.id}" }, contentType = { "playlist" }) { item ->
            PlaylistRow(item, { browser.open(LibraryCollection.Playlist(item.id)) }, actions)
        }
        if (!mixesSelected) items(page.songs, key = { "song:${it.rowId}" }, contentType = { "song" }) { row ->
            LibrarySongRow(row, row.rowId in selected, { selected = if (it) selected + row.rowId else selected - row.rowId },
                { collection?.let { actions.playFrom(it, row.rowId) } }, { actions.enqueue(listOf(row.track), true) }, { actions.enqueue(listOf(row.track), false) },
                if (playlist != null && page.query.sort == LibrarySort.DEFAULT && page.query.filter.isEmpty()) ({ actions.move(playlist.id, row.rowId, it) }) else null,
                if (playlist != null) ({ remove = setOf(row.rowId) }) else null,
                (if (motion) Modifier.animateItem() else Modifier).testTag("library_song_${row.rowId}"), selectionMode = selecting)
        }
        if (!mixesSelected && page.more) item("more") { OutlinedButton(browser::more) { Text("Load more") } }
    }
}
