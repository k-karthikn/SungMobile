package com.karthi.sungmobile.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.karthi.sungmobile.core.designsystem.component.SungLoading
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
    val showLikedMix = state.likedMix.size >= 2
    val showRecentMix = !state.data.privateMode && state.recentMix.size >= 2
    LazyColumn(modifier.fillMaxSize().testTag("discovery_home"), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item("header", "header") {
            HomeHeader(search, nowPlaying, appearance, refresh, playback.hasTrack, state.loading)
            if (state.loading) SungLoading(Modifier.padding(top = 12.dp), "Loading your music…")
            if (state.error) {
                Text(stringResource(R.string.home_read_error), Modifier.padding(top = 12.dp))
                TextButton(refresh) { Text(stringResource(R.string.retry)) }
            }
        }
        if (showLikedMix || showRecentMix) item("mixes", "mixes") {
            Column(Modifier.padding(top = 8.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HomeSectionHeading(stringResource(R.string.home_mixes_title))
                val cardWidth = if (LocalDensity.current.fontScale > 1.3f) 256.dp else 212.dp
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (showLikedMix) item("liked_mix", "mix") {
                        HomeMixCard(stringResource(R.string.liked_shuffle), state.likedMix, playback.connected,
                            { player.playCollection(it, false) }, libraryUi.add, Modifier.width(cardWidth))
                    }
                    if (showRecentMix) item("recent_mix", "mix") {
                        HomeMixCard(stringResource(R.string.recent_mix), state.recentMix, playback.connected,
                            { player.playCollection(it, false) }, libraryUi.add, Modifier.width(cardWidth))
                    }
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
            item("pinned_shelf", "shelf") {
                LazyRow(Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.data.pinned, key = { it.id }, contentType = { "pinned_playlist" }) { playlist ->
                        HomePinnedPlaylistCard(playlist, { library(LibraryCollection.Playlist(playlist.id)) })
                    }
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
