package com.karthi.sungmobile.feature.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.designsystem.theme.LocalSungMotionEnabled
import com.karthi.sungmobile.core.model.library.*

@Composable
fun LibraryScreen(browser: LibraryBrowserViewModel, page: LibraryPage, actions: LibraryActionsViewModel,
    openHistory: () -> Unit, modifier: Modifier = Modifier) {
    var selected by remember(page.query.collection) { mutableStateOf(emptySet<String>()) }
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
    BackHandler(playlist != null) { browser.open(null) }
    if (create) PlaylistNameDialog("Create playlist", dismiss = { create = false }, save = { actions.create(it) })
    remove?.let { ids -> ConfirmLibraryChange("Remove selected songs?", "Only these playlist entries will be removed.",
        { remove = null }, { playlist?.let { actions.remove(it.id, ids) }; selected = emptySet() }) }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("Library", style = MaterialTheme.typography.headlineMedium)
            TextButton(openHistory) { Text("History & privacy") }
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                FilterChip(collection == LibraryCollection.Likes, { browser.open(LibraryCollection.Likes) }, { Text("Liked songs") })
                FilterChip(collection == null || playlist != null, { browser.open(null) }, { Text("Playlists") })
                FilterChip(collection == LibraryCollection.Recent, { browser.open(LibraryCollection.Recent) }, { Text("Recent") })
            }
            Text(page.title, style = MaterialTheme.typography.titleLarge)
            if (playlist != null) TextButton({ browser.open(null) }) { Text("Back to playlists") }
            OutlinedTextField(page.query.filter, browser::filter, Modifier.fillMaxWidth(), label = { Text("Filter this list") }, singleLine = true)
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                FilterChip(page.query.sort == LibrarySort.DEFAULT, { browser.sort(LibrarySort.DEFAULT) }, { Text(if (playlist != null) "Playlist order" else "Latest") })
                FilterChip(page.query.sort == LibrarySort.TITLE, { browser.sort(LibrarySort.TITLE) }, { Text("A–Z") })
            }
            if (collection == null) TextButton({ create = true }) { Text("New playlist") }
            else Row(Modifier.horizontalScroll(rememberScrollState())) {
                TextButton({ actions.play(collection, false) }, enabled = page.songs.isNotEmpty()) { Text("Play all") }
                TextButton({ actions.play(collection, true) }, enabled = page.songs.isNotEmpty()) { Text("Shuffle all") }
                TextButton({ selected = page.songs.map { it.rowId }.toSet() }) { Text("Select loaded") }
            }
            if (selected.isNotEmpty()) LibrarySelectionBar(selected.size, { selected = emptySet() },
                { actions.like(rows.map { it.track }, it) }, { library.add(rows.map { it.track }) },
                { actions.enqueue(rows.map { it.track }, it) }, if (playlist != null) ({ remove = selected.toSet() }) else null)
            if (page.error) { Text("Could not load the library."); TextButton(browser::retry) { Text("Retry") } }
            if (page.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (!page.loading && !page.error && page.songs.isEmpty() && page.playlists.isEmpty()) Text("No items yet. Like songs or create a playlist to save them here.")
        }
        items(page.playlists, key = { it.id }) { item -> PlaylistRow(item, { browser.open(LibraryCollection.Playlist(item.id)) }, actions) }
        items(page.songs, key = { it.rowId }) { row ->
            LibrarySongRow(row, row.rowId in selected, { selected = if (it) selected + row.rowId else selected - row.rowId },
                { collection?.let { actions.playFrom(it, row.rowId) } }, { actions.enqueue(listOf(row.track), true) }, { actions.enqueue(listOf(row.track), false) },
                if (playlist != null && page.query.sort == LibrarySort.DEFAULT && page.query.filter.isEmpty()) ({ actions.move(playlist.id, row.rowId, it) }) else null,
                if (playlist != null) ({ remove = setOf(row.rowId) }) else null,
                if (motion) Modifier.animateItem() else Modifier)
        }
        if (page.more) item { OutlinedButton(browser::more) { Text("Load more") } }
    }
}

@Composable
private fun LibrarySelectionBar(count: Int, clear: () -> Unit, like: (Boolean) -> Unit, add: () -> Unit,
    enqueue: (Boolean) -> Unit, remove: (() -> Unit)?) {
    Text("$count selected")
    Row(Modifier.horizontalScroll(rememberScrollState())) {
        TextButton(clear) { Text("Clear selection") }
        TextButton({ like(true) }) { Text("Like") }
        TextButton({ like(false) }) { Text("Unlike") }
        TextButton(add) { Text("Add to playlist") }
        TextButton({ enqueue(true) }) { Text("Play next") }
        TextButton({ enqueue(false) }) { Text("Add to queue") }
        if (remove != null) TextButton(remove) { Text("Remove") }
    }
}
