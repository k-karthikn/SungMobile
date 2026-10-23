package com.karthi.sungmobile.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.R
import com.karthi.sungmobile.core.model.library.*
import com.karthi.sungmobile.core.playback.PlaybackControl
import com.karthi.sungmobile.feature.library.LocalLibraryUi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private data class HomePlayback(val connected: Boolean, val hasTrack: Boolean)

/** Home observes only the playback flags it uses, never the four-per-second position updates. */
@Composable
fun DiscoveryHomeScreen(state: HomeState, refresh: () -> Unit, player: PlaybackControl,
    search: () -> Unit, nowPlaying: () -> Unit, appearance: () -> Unit, library: (LibraryCollection?) -> Unit,
    modifier: Modifier = Modifier) {
    val flags = remember(player) {
        player.state.map { HomePlayback(it.connected, it.track != null) }.distinctUntilChanged()
    }
    val initialPlayback = remember(player) { HomePlayback(player.state.value.connected, player.state.value.track != null) }
    val playback by flags.collectAsStateWithLifecycle(initialPlayback)
    val libraryUi = LocalLibraryUi.current
    val recent = remember(state.data.recent, state.data.privateMode) {
        if (state.data.privateMode) emptyList() else state.data.recent.take(12)
    }
    val liked = remember(state.data.liked) { state.data.liked.take(12) }
    LazyColumn(modifier.fillMaxSize().testTag("discovery_home"), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item("header", "header") {
            HomeHeader(search, nowPlaying, appearance, refresh, playback.hasTrack, state.loading)
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 12.dp))
            if (state.error) {
                Text(stringResource(R.string.home_read_error), Modifier.padding(top = 12.dp))
                TextButton(refresh) { Text(stringResource(R.string.retry)) }
            }
        }
        item("mixes", "mixes") {
            val showLiked = state.likedMix.size >= 2
            val showRecent = !state.data.privateMode && state.recentMix.size >= 2
            if (showLiked || showRecent) BoxWithConstraints(Modifier.padding(top = 16.dp, bottom = 8.dp)) {
                // Three 48dp controls plus padding need at least 160dp per card.
                val stack = maxWidth < 332.dp || LocalDensity.current.fontScale > 1.3f
                val likedCard: @Composable (Modifier) -> Unit = { cardModifier ->
                    HomeMixCard(stringResource(R.string.liked_shuffle), state.likedMix, playback.connected,
                        { player.playCollection(it, false) }, libraryUi.add, cardModifier)
                }
                val recentCard: @Composable (Modifier) -> Unit = { cardModifier ->
                    HomeMixCard(stringResource(R.string.recent_mix), state.recentMix, playback.connected,
                        { player.playCollection(it, false) }, libraryUi.add, cardModifier)
                }
                if (stack) Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (showLiked) likedCard(Modifier.fillMaxWidth())
                    if (showRecent) recentCard(Modifier.fillMaxWidth())
                } else Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (showLiked) likedCard(Modifier.weight(1f))
                    if (showRecent) recentCard(Modifier.weight(1f))
                    if (showLiked != showRecent) Spacer(Modifier.weight(1f))
                }
            }
        }
        if (recent.isNotEmpty()) {
            item("recent_heading", "heading") {
                HomeSectionHeading(stringResource(R.string.recently_played)) { library(LibraryCollection.Recent) }
            }
            itemsIndexed(recent, key = { _, song -> "recent:${song.id}" }, contentType = { _, _ -> "song" }) { index, song ->
                HomeSongRow(song, playback.connected, { player.select(state.data.recent, index) },
                    { player.enqueue(song, true) }, { player.enqueue(song, false) })
            }
        }
        if (state.data.pinned.isNotEmpty()) {
            item("pinned_heading", "heading") {
                HomeSectionHeading(stringResource(R.string.pinned_playlists)) { library(null) }
            }
            items(state.data.pinned, key = { "playlist:${it.id}" }, contentType = { "playlist" }) { playlist ->
                OutlinedButton({ library(LibraryCollection.Playlist(playlist.id)) }, Modifier.fillMaxWidth()) {
                    Text("${playlist.name} · ${playlist.songCount} songs")
                }
            }
        }
        if (liked.isNotEmpty()) {
            item("liked_heading", "heading") {
                HomeSectionHeading(stringResource(R.string.liked_songs)) { library(LibraryCollection.Likes) }
            }
            itemsIndexed(liked, key = { _, song -> "like:${song.id}" }, contentType = { _, _ -> "song" }) { index, song ->
                HomeSongRow(song, playback.connected, { player.select(state.data.liked, index) },
                    { player.enqueue(song, true) }, { player.enqueue(song, false) })
            }
        }
        if (!state.loading && liked.isEmpty() && recent.isEmpty() && state.data.pinned.isEmpty()) item("empty", "empty") {
            Text(stringResource(R.string.discovery_empty), Modifier.padding(vertical = 24.dp),
                style = MaterialTheme.typography.bodyLarge)
        }
    }
}
