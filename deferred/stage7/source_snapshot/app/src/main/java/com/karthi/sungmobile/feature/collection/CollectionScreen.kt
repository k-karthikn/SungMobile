package com.karthi.sungmobile.feature.collection

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import com.karthi.sungmobile.core.designsystem.theme.LocalSungMotionEnabled
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.designsystem.artwork.SungArtwork
import com.karthi.sungmobile.core.model.*
import com.karthi.sungmobile.core.playback.PlaybackControl
import com.karthi.sungmobile.feature.search.CatalogRow

@Composable
fun CollectionScreen(state: CollectionState, model: CollectionViewModel, operations: CollectionActionsViewModel,
    player: PlaybackControl, searchArtist: (String) -> Unit, showPlayer: () -> Unit, modifier: Modifier = Modifier) {
    val playback by player.state.collectAsStateWithLifecycle()
    val motion = LocalSungMotionEnabled.current
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row { TextButton({ operations.cancel(); model.close() }) { Text("Back") }; TextButton(showPlayer, enabled = playback.track != null) { Text("Now playing") } }
            TextButton({ operations.cancel(); model.refresh() }, enabled = !state.loading) { Text("Refresh collection") }
            Text(state.page?.title ?: state.route?.title.orEmpty(), style = MaterialTheme.typography.headlineMedium)
            state.page?.details?.let { details ->
                SungArtwork(details.artworkUrl, Modifier.size(96.dp))
                Text(details.creator)
                Text(if (details.channelVideos) "YouTube channel videos" else (state.route?.kind ?: details.kind).name.lowercase())
                Text("${state.entries.size} loaded" + (details.totalCount?.let { " · $it reported by YouTube" } ?: ""))
            }
            if ((state.route?.startMs ?: 0) > 0) Text("Play all starts this song at ${state.route!!.startMs / 1000}s. Tap the song to play from the beginning.")
            if (state.page?.partial == true) Text("YouTube returned partial content; some entries may be unavailable.")
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                CollectionAction.entries.forEach { action -> TextButton({ operations.begin(state, action) }, enabled = state.entries.isNotEmpty() && !state.loading && (action in setOf(CollectionAction.SAVE, CollectionAction.DOWNLOAD) || playback.connected)) { Text(action.label) } }
            }
            val notice = state.page?.notice
            if (notice is CatalogNotice.ArtistBrowseUnavailable) {
                Text("This artist's music catalog is unavailable from the current source.")
                TextButton({ model.close(); searchArtist(notice.artistName) }) { Text("Search songs by this artist") }
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
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            else if (state.page != null && state.entries.isEmpty() && notice == null) Text("No playable songs were returned.")
        }
        itemsIndexed(state.entries, key = { _, entry -> entry.id }) { index, entry ->
            CatalogRow(entry.track, false, { operations.cancel(); player.select(state.entries.map { it.track }, index) },
                modifier = if (motion) Modifier.animateItem() else Modifier,
                playNext = { player.enqueue(entry.track, true) }, append = { player.enqueue(entry.track, false) }, enabled = playback.connected)
        }
        items(state.page?.unavailable.orEmpty()) { entry ->
            Text("${entry.title} · ${entry.reason}", style = MaterialTheme.typography.bodyMedium)
        }
        if (state.page?.next != null) item { OutlinedButton(model::more, enabled = !state.loading) { Text("Load more") } }
    }
}
