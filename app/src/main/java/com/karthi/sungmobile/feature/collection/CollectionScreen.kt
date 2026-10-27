package com.karthi.sungmobile.feature.collection

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import com.karthi.sungmobile.core.designsystem.theme.LocalSungMotionEnabled
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.model.*
import com.karthi.sungmobile.core.playback.PlaybackControl
import com.karthi.sungmobile.feature.library.LibrarySongRow
import com.karthi.sungmobile.feature.library.LocalLibraryUi
import com.karthi.sungmobile.core.model.library.*
import com.karthi.sungmobile.core.designsystem.icon.*

@Composable
fun CollectionScreen(state: CollectionState, model: CollectionViewModel, operations: CollectionActionsViewModel,
    player: PlaybackControl, searchArtist: (String) -> Unit, showPlayer: () -> Unit, modifier: Modifier = Modifier) {
    val playback by player.state.collectAsStateWithLifecycle()
    val library = LocalLibraryUi.current
    val motion = LocalSungMotionEnabled.current
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp,
        top = 8.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item("collection_header") {
            CollectionHeader(state, playback.track != null,
                { operations.cancel(); model.close() },
                { operations.cancel(); model.refresh() }, showPlayer)
        }
        item("collection_actions") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CollectionActionRow(state.entries.isNotEmpty(), state.loading, playback.connected) {
                    operations.begin(state, it)
                }
                if ((state.route?.startMs ?: 0) > 0) Text(
                    "Play all starts at ${state.route!!.startMs / 1000}s. Tap a song to start it from the beginning.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (state.page?.partial == true) Text("Some entries are unavailable from YouTube.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        val notice = state.page?.notice
        if (notice != null || state.error != null || state.loading ||
            (state.page != null && state.entries.isEmpty())) item("collection_status") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (notice is CatalogNotice.ArtistBrowseUnavailable) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("This artist's music catalog is unavailable from the current source.")
                            OutlinedButton({ model.close(); searchArtist(notice.artistName) }) {
                                SungIcon(SungSymbol.Search, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Search this artist")
                            }
                        }
                    }
                }
                if (state.error != null) {
                    Text(when (state.error) {
                        SourceFailure.UNSUPPORTED -> "This YouTube Music collection link is not supported by the current adapter."
                        SourceFailure.EXPIRED_PAGE -> "This page has expired. Refresh the collection."
                        SourceFailure.AUTHENTICATION_REQUIRED -> "YouTube requires authentication for this content."
                        SourceFailure.UNAVAILABLE -> "This collection is unavailable or private."
                        else -> "Couldn't load this collection. Check your connection and retry."
                    }, color = MaterialTheme.colorScheme.error)
                    TextButton(model::retry) { Text(if (state.error == SourceFailure.EXPIRED_PAGE) "Refresh" else "Retry") }
                }
                if (state.loading) com.karthi.sungmobile.core.designsystem.component.SungLoading(label = "Loading collection…")
                else if (state.page != null && state.entries.isEmpty() && notice == null) Text("No playable songs were returned.")
            }
        }
        if (state.entries.isNotEmpty()) item("collection_song_heading") {
            Text(if (state.displayKind() == CatalogKind.ARTIST) "Videos" else "Songs",
                style = MaterialTheme.typography.titleMedium)
        }
        itemsIndexed(state.entries, key = { _, entry -> entry.id }) { index, entry ->
            LibrarySongRow(LibrarySong(entry.id, entry.track, entry.track.libraryKeyOrNull() in library.liked),
                false, {}, { if (playback.connected) { operations.cancel(); player.select(state.entries.map { it.track }, index) } },
                { if (playback.connected) player.enqueue(entry.track, true) },
                { if (playback.connected) player.enqueue(entry.track, false) }, null, null,
                modifier = if (motion) Modifier.animateItem() else Modifier, playbackEnabled = playback.connected)
        }
        items(state.page?.unavailable.orEmpty()) { entry ->
            Text("${entry.title} · ${entry.reason}", style = MaterialTheme.typography.bodyMedium)
        }
        if (state.page?.next != null) item { OutlinedButton(model::more, enabled = !state.loading) { Text("Load more") } }
    }
}
