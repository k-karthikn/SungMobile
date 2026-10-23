package com.karthi.sungmobile.feature.library

import androidx.activity.compose.BackHandler
import com.karthi.sungmobile.feature.library.components.*
import com.karthi.sungmobile.core.designsystem.component.SungFilterField
import com.karthi.sungmobile.core.designsystem.icon.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.designsystem.theme.LocalSungMotionEnabled
import com.karthi.sungmobile.core.model.library.*

@Composable
fun LibraryScreen(browser: LibraryBrowserViewModel, page: LibraryPage, actions: LibraryActionsViewModel,
    advanced: AdvancedLibraryViewModel, advancedState: AdvancedLibraryState,
    openHistory: () -> Unit, modifier: Modifier = Modifier, openSessions: () -> Unit = {}) {
    if (advancedState.page != AdvancedPage.CLOSED) {
        AdvancedLibraryScreen(advanced, advancedState, modifier)
        return
    }
    var selected by remember(page.query.collection) { mutableStateOf(emptySet<String>()) }
    var selecting by remember(page.query.collection) { mutableStateOf(false) }
    var create by remember { mutableStateOf(false) }
    var remove by remember { mutableStateOf<Set<String>?>(null) }
    val collection = page.query.collection
    val playlist = collection as? LibraryCollection.Playlist
    val library = LocalLibraryUi.current
    val motion = LocalSungMotionEnabled.current
    val rows = page.songs.filter { it.rowId in selected }
    LaunchedEffect(page.songs, page.loading) {
        if (!page.loading) selected = selected.intersect(page.songs.map { it.rowId }.toSet())
    }
    BackHandler(playlist != null || selecting) { if (selecting) { selecting = false; selected = emptySet() } else browser.open(null) }
    if (create) PlaylistNameDialog("Create playlist", dismiss = { create = false }, save = { actions.create(it) })
    remove?.let { ids -> ConfirmLibraryChange("Remove selected songs?", "Only these playlist entries will be removed.",
        { remove = null }, { playlist?.let { actions.remove(it.id, ids) }; selected = emptySet() }) }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LibraryHeader(collection, browser::open, openSessions, openHistory, advanced::openSmartList, advanced::openStatistics)
            if (playlist != null) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    IconButton({ browser.open(null) }) { SungIcon(SungSymbol.Back, "Back to playlists") }
                    Text(page.title, style = MaterialTheme.typography.headlineSmall)
                }
                TextButton({ advanced.openCleanup(playlist.id) }) { Text("Cleanup & versions") }
            }
            SungFilterField(page.query.filter, browser::filter, when (collection) {
                null -> "Filter playlists"
                LibraryCollection.Likes -> "Filter liked songs"
                LibraryCollection.Recent -> "Filter recently played"
                else -> "Filter this playlist"
            })
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(page.query.sort == LibrarySort.DEFAULT, { browser.sort(LibrarySort.DEFAULT) }, { Text(if (playlist != null) "Playlist order" else "Latest") }, shape = CircleShape)
                FilterChip(page.query.sort == LibrarySort.TITLE, { browser.sort(LibrarySort.TITLE) }, { Text("A–Z") }, shape = CircleShape)
            }
            if (collection == null) FilledTonalButton({ create = true }) { Text("New playlist") }
            else Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton({ actions.play(collection, false) }, enabled = page.songs.isNotEmpty()) { Text("Play all") }
                OutlinedButton({ actions.play(collection, true) }, enabled = page.songs.isNotEmpty()) { Text("Shuffle all") }
                if (!selecting) OutlinedButton({ selecting = true; selected = emptySet() }, enabled = page.songs.isNotEmpty()) { Text("Select") }
            }
            if (selecting) LibrarySelectionBar(selected.size, { selecting = false; selected = emptySet() },
                { selected = page.songs.map { it.rowId }.toSet() },
                { actions.like(rows.map { it.track }, it) }, { library.add(rows.map { it.track }) },
                { actions.enqueue(rows.map { it.track }, it) }, if (playlist != null) ({ remove = selected.toSet() }) else null)
            if (page.error) { Text("Could not load the library."); TextButton(browser::retry) { Text("Retry") } }
            if (page.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (!page.loading && !page.error && page.songs.isEmpty() && page.playlists.isEmpty()) Text(if (page.query.filter.isNotBlank()) "No matches. Try a different filter." else "No items yet. Like songs or create a playlist to save them here.", Modifier.padding(vertical = 24.dp), style = MaterialTheme.typography.bodyLarge)
            }
        }
        items(page.playlists, key = { it.id }) { item -> PlaylistRow(item, { browser.open(LibraryCollection.Playlist(item.id)) }, actions) }
        items(page.songs, key = { it.rowId }) { row ->
            LibrarySongRow(row, row.rowId in selected, { selected = if (it) selected + row.rowId else selected - row.rowId },
                { collection?.let { actions.playFrom(it, row.rowId) } }, { actions.enqueue(listOf(row.track), true) }, { actions.enqueue(listOf(row.track), false) },
                if (playlist != null && page.query.sort == LibrarySort.DEFAULT && page.query.filter.isEmpty()) ({ actions.move(playlist.id, row.rowId, it) }) else null,
                if (playlist != null) ({ remove = setOf(row.rowId) }) else null,
                (if (motion) Modifier.animateItem() else Modifier).testTag("library_song_${row.rowId}"), selectionMode = selecting)
        }
        if (page.more) item { OutlinedButton(browser::more) { Text("Load more") } }
    }
}
