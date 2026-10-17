package com.karthi.sungmobile.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.model.*
import com.karthi.sungmobile.core.model.library.*
import com.karthi.sungmobile.core.playback.PlaybackControl
import com.karthi.sungmobile.feature.library.LocalLibraryUi
import com.karthi.sungmobile.feature.search.CatalogRow

@Composable
fun DiscoveryHomeScreen(state: HomeState, refresh: () -> Unit, player: PlaybackControl,
    search: () -> Unit, nowPlaying: () -> Unit, appearance: () -> Unit, library: (LibraryCollection?) -> Unit,
    modifier: Modifier = Modifier) {
    val playback by player.state.collectAsStateWithLifecycle()
    val libraryUi = LocalLibraryUi.current
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row { Text("Sung", Modifier.weight(1f), style = MaterialTheme.typography.headlineLarge)
                TextButton(appearance, Modifier.testTag("appearance")) { Text("Appearance") } }
            FilledTonalButton(search, Modifier.fillMaxWidth().testTag("home_search")) { Text("Search music or paste a YouTube link") }
            Row { TextButton(nowPlaying, enabled = playback.track != null) { Text("Now playing") }
                TextButton(refresh) { Text("Refresh mixes") } }
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (state.error) { Text("Could not read Home data."); TextButton(refresh) { Text("Retry") } }
        }
        if (state.data.pinned.isNotEmpty()) {
            item { Text("Pinned playlists", style = MaterialTheme.typography.titleLarge) }
            items(state.data.pinned, key = { "playlist:${it.id}" }) { playlist ->
                OutlinedButton({ library(LibraryCollection.Playlist(playlist.id)) }, Modifier.fillMaxWidth()) { Text("${playlist.name} · ${playlist.songCount} songs") }
            }
        }
        item {
            Text("Your library", style = MaterialTheme.typography.titleLarge)
            Row { TextButton({ library(LibraryCollection.Likes) }) { Text("Liked songs") }; TextButton({ library(null) }) { Text("Playlists") } }
            if (state.likedMix.size >= 2) MixCard("Liked shuffle", state.likedMix, player, libraryUi.add)
            if (!state.data.privateMode && state.recentMix.size >= 2) MixCard("Recent mix", state.recentMix, player, libraryUi.add)
        }
        if (state.data.recent.isNotEmpty() && !state.data.privateMode) {
            item { TextButton({ library(LibraryCollection.Recent) }) { Text("Recently played · View all") } }
            items(state.data.recent.take(12), key = { "recent:${it.id}" }) { song ->
                CatalogRow(song, false, { player.select(state.data.recent, state.data.recent.indexOf(song)) },
                    playNext = { player.enqueue(song, true) }, append = { player.enqueue(song, false) }, enabled = playback.connected)
            }
        }
        if (state.data.liked.isNotEmpty()) {
            item { TextButton({ library(LibraryCollection.Likes) }) { Text("Liked songs · View all") } }
            items(state.data.liked.take(12), key = { "like:${it.id}" }) { song ->
                CatalogRow(song, false, { player.select(state.data.liked, state.data.liked.indexOf(song)) },
                    playNext = { player.enqueue(song, true) }, append = { player.enqueue(song, false) }, enabled = playback.connected)
            }
        }
        if (!state.loading && state.data.liked.isEmpty() && state.data.recent.isEmpty() && state.data.pinned.isEmpty()) item {
            Text("Find a song to get started. Like songs and pin playlists to make them available here.")
        }
    }
}

@Composable
private fun MixCard(title: String, songs: List<CatalogItem>, player: PlaybackControl, save: (List<CatalogItem>) -> Unit) {
    val playback by player.state.collectAsStateWithLifecycle()
    var preview by remember { mutableStateOf<List<CatalogItem>?>(null) }
    // Do not retain an obsolete history-derived preview after clearing or changing privacy.
    LaunchedEffect(songs) { preview = null }
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("$title · ${songs.size} songs", style = MaterialTheme.typography.titleMedium)
            Row {
                TextButton({ preview = songs.toList() }) { Text("View") }
                TextButton({ player.playCollection(songs.toList(), false) }, enabled = playback.connected) { Text("Play mix") }
                TextButton({ save(songs.toList()) }) { Text("Save") }
            }
        }
    }
    preview?.let { snapshot -> AlertDialog(onDismissRequest = { preview = null }, title = { Text(title) }, text = {
        LazyColumn(Modifier.heightIn(max = 360.dp)) { items(snapshot, key = { it.id }) { Text("${it.title} · ${it.subtitle}", Modifier.padding(vertical = 8.dp)) } }
    }, confirmButton = { TextButton({ player.playCollection(snapshot, false); preview = null }, enabled = playback.connected) { Text("Play") } },
        dismissButton = { TextButton({ preview = null }) { Text("Close") } }) }
}
