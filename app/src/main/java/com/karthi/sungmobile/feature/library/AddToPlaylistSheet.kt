package com.karthi.sungmobile.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.designsystem.icon.SungIcon
import com.karthi.sungmobile.core.designsystem.icon.SungSymbol
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.library.LibraryRepository
import com.karthi.sungmobile.core.model.library.LocalPlaylist
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.onStart

/** Paged playlist chooser observes library changes without loading an unbounded list. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistDialog(
    tracks: List<CatalogItem>,
    repository: LibraryRepository,
    actions: LibraryActionsViewModel,
    dismiss: () -> Unit,
) {
    var filter by remember { mutableStateOf("") }
    var limit by remember { mutableIntStateOf(50) }
    var playlists by remember { mutableStateOf(emptyList<LocalPlaylist>()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var duplicates by remember { mutableStateOf(false) }
    var create by remember { mutableStateOf(false) }
    val listHeight = (LocalConfiguration.current.screenHeightDp.dp * .36f).coerceAtMost(320.dp)

    LaunchedEffect(filter, limit, retry, repository) {
        repository.changes.drop(1).onStart { emit(Unit) }.collectLatest {
            loading = true
            try {
                playlists = repository.playlists(filter, limit = limit + 1)
                error = false
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                error = true
            }
            loading = false
        }
    }

    if (create) {
        PlaylistNameDialog("Create playlist", dismiss = { create = false }, save = {
            actions.create(it, tracks, duplicates)
            dismiss()
        })
    }

    ModalBottomSheet(onDismissRequest = dismiss) {
        Column(
            Modifier.fillMaxWidth().imePadding().padding(horizontal = 20.dp).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Add to playlist", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        if (tracks.size == 1) "1 song selected" else "${tracks.size} songs selected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = dismiss) { SungIcon(SungSymbol.Close, "Close") }
            }

            OutlinedTextField(
                value = filter,
                onValueChange = { filter = it; limit = 50 },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                label = { Text("Find a playlist") },
                leadingIcon = { SungIcon(SungSymbol.Search) },
                trailingIcon = {
                    if (filter.isNotEmpty()) {
                        IconButton(onClick = { filter = "" }) { SungIcon(SungSymbol.Close, "Clear search") }
                    }
                },
            )

            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = duplicates, onCheckedChange = { duplicates = it })
                    Column(Modifier.weight(1f).padding(start = 4.dp)) {
                        Text("Allow duplicate songs", style = MaterialTheme.typography.titleSmall)
                        Text(
                            if (duplicates) "Songs already in the playlist can be added again."
                            else "Songs already in the playlist will be skipped.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            FilledTonalButton(onClick = { create = true }, modifier = Modifier.fillMaxWidth()) {
                SungIcon(SungSymbol.Plus, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Create a new playlist")
            }

            when {
                loading -> Box(Modifier.fillMaxWidth().heightIn(min = 96.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                error -> EmptyPlaylistChooserState(
                    title = "Couldn’t load playlists",
                    message = "Check your library and try again.",
                    action = "Retry",
                    onAction = { retry++ },
                )
                playlists.isEmpty() -> EmptyPlaylistChooserState(
                    title = if (filter.isBlank()) "No playlists yet" else "No playlists found",
                    message = if (filter.isBlank()) "Create a playlist above to save these songs."
                        else "Try another name or clear your search.",
                )
                else -> LazyColumn(
                    Modifier.fillMaxWidth().heightIn(max = listHeight),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(playlists.take(limit), key = { it.id }) { playlist ->
                        PlaylistDestinationRow(playlist) {
                            actions.add(playlist.id, tracks, duplicates)
                            dismiss()
                        }
                    }
                    if (playlists.size > limit) {
                        item {
                            FilledTonalButton(
                                onClick = { limit += 50 },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Load more playlists") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistDestinationRow(playlist: LocalPlaylist, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.secondaryContainer) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    SungIcon(SungSymbol.QueueMusic, modifier = Modifier.size(24.dp))
                }
            }
            Column(Modifier.weight(1f)) {
                Text(playlist.name, style = MaterialTheme.typography.titleSmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${playlist.songCount} songs", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.tertiaryContainer) {
                Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                    SungIcon(SungSymbol.Plus, "Add to ${playlist.name}", Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun EmptyPlaylistChooserState(
    title: String,
    message: String,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SungIcon(SungSymbol.QueueMusic, modifier = Modifier.size(28.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(message, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (action != null) FilledTonalButton(onClick = onAction) { Text(action) }
        }
    }
}
