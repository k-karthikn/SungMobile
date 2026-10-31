package com.karthi.sungmobile.feature.spotify

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.karthi.sungmobile.core.designsystem.component.SungSettingsGroup

@Composable
fun SpotifyLibraryImportSection(viewModel: SpotifyLibraryImportViewModel, connected: Boolean) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showPlaylists by remember(viewModel) { mutableStateOf(false) }

    SungSettingsGroup("Import music") {
        Text("Bring Spotify playlists or saved tracks into your SungMobile library.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (!connected) Text("Connect Spotify first to browse your library.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = {
                    when {
                        !state.playlistsLoaded -> { showPlaylists = true; viewModel.loadPlaylists() }
                        showPlaylists -> viewModel.loadPlaylists()
                        else -> showPlaylists = true
                    }
                },
            enabled = connected && !state.loadingPlaylists && !state.working,
            modifier = Modifier.weight(1f),
            ) {
                if (state.loadingPlaylists) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text(when {
                    state.playlistsLoaded && !showPlaylists -> "Show library"
                    state.playlistsLoaded -> "Refresh library"
                    else -> "Browse Spotify library"
                })
            }
            if (state.playlistsLoaded && showPlaylists && state.previewTitle == null) {
                TextButton(onClick = { showPlaylists = false }, enabled = !state.working) { Text("Hide") }
            }
        }

        if (state.playlistsLoaded && showPlaylists && state.previewTitle == null) {
            PlaylistChoiceCard(
                title = "Liked Songs",
                subtitle = "Your saved tracks",
                artworkUrl = null,
                onClick = viewModel::previewLikedSongs,
                enabled = !state.working,
            )
            state.playlists.take(MAX_VISIBLE_PLAYLISTS).forEach { playlist ->
                PlaylistChoiceCard(
                    title = playlist.name,
                    subtitle = "${playlist.trackCount} tracks",
                    artworkUrl = playlist.artworkUrl,
                    onClick = { viewModel.previewPlaylist(playlist) },
                    enabled = !state.working,
                )
            }
            if (state.playlists.isEmpty()) {
                Text("No playlists found. You can still import Spotify Liked Songs.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (state.playlists.size > MAX_VISIBLE_PLAYLISTS) {
                Text("Showing ${MAX_VISIBLE_PLAYLISTS} playlists.",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (state.working) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (state.matchingTotal == 0) "Reading Spotify library…"
                    else "Matching songs ${state.matchingProgress} of ${state.matchingTotal}…",
                    style = MaterialTheme.typography.bodyMedium)
                if (state.matchingTotal > 0) {
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { state.matchingProgress.toFloat() / state.matchingTotal.coerceAtLeast(1) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        state.previewTitle?.let { title ->
            HorizontalDivider()
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("${state.matchedCount} matched · ${state.skippedCount} will be skipped",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            state.matches.take(PREVIEW_TRACK_LIMIT).forEach { row ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AsyncImage(
                        model = row.sungTrack?.artworkUrl ?: row.spotifyTrack.artworkUrl,
                        contentDescription = null,
                        modifier = Modifier.size(42.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    Column(Modifier.weight(1f)) {
                        Text(row.spotifyTrack.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium)
                        Text(
                            if (row.sungTrack == null) "No close YouTube Music match"
                            else "Matched · ${row.sungTrack.title}",
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (row.sungTrack == null) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            if (state.matches.size > PREVIEW_TRACK_LIMIT) {
                Text("Showing ${PREVIEW_TRACK_LIMIT} of ${state.matches.size} tracks.",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = viewModel::importPreview,
                    enabled = connected && !state.working && state.matchedCount > 0,
                    modifier = Modifier.weight(1f),
                ) { Text(if (state.previewIsLikedSongs) "Add to Liked Songs" else "Import playlist") }
                TextButton(onClick = viewModel::clearPreview, enabled = !state.working) { Text("Cancel") }
            }
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun PlaylistChoiceCard(
    title: String,
    subtitle: String,
    artworkUrl: String?,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (artworkUrl != null) {
                AsyncImage(model = artworkUrl, contentDescription = null,
                    modifier = Modifier.size(46.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
            }
            Column(Modifier.weight(1f)) {
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("Open", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

private const val MAX_VISIBLE_PLAYLISTS = 20
private const val PREVIEW_TRACK_LIMIT = 8
