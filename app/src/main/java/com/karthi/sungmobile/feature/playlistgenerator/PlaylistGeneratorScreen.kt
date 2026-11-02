package com.karthi.sungmobile.feature.playlistgenerator

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.designsystem.artwork.SungArtwork
import com.karthi.sungmobile.core.designsystem.icon.SungIcon
import com.karthi.sungmobile.core.designsystem.icon.SungSymbol
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.library.libraryKeyOrNull

@Composable
fun PlaylistGeneratorScreen(
    viewModel: PlaylistGeneratorViewModel,
    currentTrack: CatalogItem?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val selectedSeedKeys = state.selectedSeeds.mapNotNull { it.libraryKeyOrNull() }.toSet()
    LaunchedEffect(currentTrack?.id) { viewModel.setCurrentTrack(currentTrack) }

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) { SungIcon(SungSymbol.Back, "Back to playlists") }
            Column(Modifier.weight(1f)) {
                Text("Playlist generator", style = MaterialTheme.typography.titleLarge)
                Text("Build a mix from your taste and music discovery", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (state.generating || state.searching) LinearProgressIndicator(Modifier.fillMaxWidth())
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "generator_intro") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Choose a starting point", style = MaterialTheme.typography.titleMedium)
                    Text("Pick up to four songs, or leave them unselected to use your listening taste.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (state.suggestions.isEmpty() && state.currentTrack == null && !state.privateMode) {
                        Text("No listening signals yet? Search for a song to start.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                    }
                    if (state.privateMode) {
                        Text("Private mode is on. Listening history will not be used.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
            state.currentTrack?.let { track ->
                item(key = "current_track_seed") {
                    SeedTrackRow(track, track.libraryKeyOrNull()?.let(selectedSeedKeys::contains) == true,
                        selectedLabel = "Use current song", onClick = { viewModel.toggleSeed(track) })
                }
            }
            if (state.suggestions.isNotEmpty()) {
                item(key = "taste_seeds_heading") {
                    Text("From your library", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(state.suggestions, key = { "taste:${it.libraryKeyOrNull()}" }) { track ->
                    SeedTrackRow(track, track.libraryKeyOrNull()?.let(selectedSeedKeys::contains) == true,
                        selectedLabel = "Use as starting song", onClick = { viewModel.toggleSeed(track) })
                }
            }
            item(key = "search_seed") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = viewModel::setQuery,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("Search for a starting song") },
                    )
                    IconButton(onClick = viewModel::searchSeeds, enabled = state.query.isNotBlank() && !state.searching) {
                        if (state.searching) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                        else SungIcon(SungSymbol.Search, "Search starting songs")
                    }
                }
            }
            if (state.searchResults.isNotEmpty()) {
                item(key = "search_results_heading") {
                    Text("Search results", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(state.searchResults, key = { "search:${it.libraryKeyOrNull()}" }) { track ->
                    SeedTrackRow(track, track.libraryKeyOrNull()?.let(selectedSeedKeys::contains) == true,
                        selectedLabel = "Add starting song", onClick = { viewModel.toggleSeed(track) })
                }
            }
            item(key = "mix_options") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Playlist length", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(12, 20, 30).forEach { count ->
                            FilterChip(selected = state.trackCount == count,
                                onClick = { viewModel.setTrackCount(count) },
                                label = { Text("$count songs") })
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Familiar", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.weight(1f))
                        Text("More new", style = MaterialTheme.typography.labelMedium)
                    }
                    Slider(
                        value = state.discoveryLevel / 100f,
                        onValueChange = { viewModel.setDiscoveryLevel((it * 100).toInt()) },
                        valueRange = 0f..1f,
                        modifier = Modifier.testTag("playlist_discovery_slider"),
                    )
                }
            }
            item(key = "generator_actions") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = viewModel::generate,
                        enabled = !state.generating,
                        modifier = Modifier.fillMaxWidth().testTag("generate_playlist"),
                        shape = RoundedCornerShape(24.dp),
                    ) {
                        if (state.generating) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else SungIcon(SungSymbol.Shuffle, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (state.generatedTracks.isEmpty()) "Generate playlist" else "Generate again")
                    }
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall) }
                    state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall) }
                }
            }
            if (state.generatedTracks.isNotEmpty()) {
                item(key = "generated_header") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Your mix", style = MaterialTheme.typography.headlineSmall)
                        Text(if (state.selectedSeeds.isNotEmpty())
                            "${state.generatedTracks.size} songs, based on ${state.selectedSeeds.size} starting songs."
                        else "${state.generatedTracks.size} songs, shaped by your listening taste.",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (state.generatedTracks.size < state.trackCount) {
                            Text("Found ${state.generatedTracks.size} of ${state.trackCount} requested. Add more starting songs for a fuller mix.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                        }
                        OutlinedTextField(
                            value = state.playlistName,
                            onValueChange = viewModel::setPlaylistName,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.saved,
                            singleLine = true,
                            label = { Text("Playlist name") },
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = viewModel::playPreview, modifier = Modifier.weight(1f)) {
                                SungIcon(SungSymbol.Play, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp)); Text("Play")
                            }
                            OutlinedButton(onClick = viewModel::savePlaylist,
                                enabled = !state.saving && !state.saved, modifier = Modifier.weight(1f)) {
                                if (state.saving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                else SungIcon(SungSymbol.Library, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp)); Text(when {
                                    state.saving -> "Saving"
                                    state.saved -> "Saved"
                                    else -> "Save playlist"
                                })
                            }
                        }
                    }
                }
                items(state.generatedTracks, key = { "generated:${it.libraryKeyOrNull()}" }) { track ->
                    val index = state.generatedTracks.indexOf(track)
                    GeneratedTrackRow(track, index, viewModel::playTrack, viewModel::removeGenerated)
                }
            }
        }
    }
}

@Composable
private fun SeedTrackRow(track: CatalogItem, selected: Boolean, selectedLabel: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SungArtwork(track.artworkUrl, Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)), description = null)
            Column(Modifier.weight(1f)) {
                Text(track.title.ifBlank { "Unknown song" }, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall)
                Text(track.subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(if (selected) "Selected" else selectedLabel,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun GeneratedTrackRow(track: CatalogItem, index: Int,
    play: (Int) -> Unit, remove: (CatalogItem) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { play(index) }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SungArtwork(track.artworkUrl, Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)), description = null)
        Column(Modifier.weight(1f)) {
            Text(track.title.ifBlank { "Unknown song" }, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall)
            Text(track.subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = { remove(track) }) { SungIcon(SungSymbol.Close, "Remove ${track.title}") }
    }
}
