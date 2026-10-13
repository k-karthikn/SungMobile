package com.karthi.sungmobile.feature.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.library.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collectLatest

@Composable
fun PlaylistNameDialog(title: String, initial: String = "", help: String = "", dismiss: () -> Unit, save: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(onDismissRequest = dismiss, title = { Text(title) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (help.isNotEmpty()) Text(help)
            OutlinedTextField(name, { name = it.take(120) }, label = { Text("Playlist name") }, singleLine = true)
            Text("${name.trim().length}/120", style = MaterialTheme.typography.bodySmall)
        }
    }, confirmButton = { TextButton({ save(name.trim()); dismiss() }, enabled = name.trim().isNotEmpty()) { Text("Save") } },
        dismissButton = { TextButton(dismiss) { Text("Cancel") } })
}

@Composable
fun ConfirmLibraryChange(title: String, body: String, dismiss: () -> Unit, confirm: () -> Unit) {
    AlertDialog(onDismissRequest = dismiss, title = { Text(title) }, text = { Text(body) },
        confirmButton = { TextButton({ confirm(); dismiss() }) { Text("Confirm") } },
        dismissButton = { TextButton(dismiss) { Text("Cancel") } })
}

/** Paged chooser observes edits while open and never loads an entire large library. */
@Composable
fun AddToPlaylistDialog(tracks: List<CatalogItem>, repository: LibraryRepository, actions: LibraryActionsViewModel, dismiss: () -> Unit) {
    var filter by remember { mutableStateOf("") }
    var limit by remember { mutableIntStateOf(50) }
    var playlists by remember { mutableStateOf(emptyList<LocalPlaylist>()) }
    var error by remember { mutableStateOf(false) }
    var duplicates by remember { mutableStateOf(false) }
    var create by remember { mutableStateOf(false) }
    LaunchedEffect(filter, limit) {
        repository.changes.collectLatest {
            try { playlists = repository.playlists(filter, limit = limit + 1); error = false }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { error = true }
        }
    }
    if (create) PlaylistNameDialog("Create playlist", dismiss = { create = false }, save = {
        actions.create(it, tracks, duplicates); dismiss()
    })
    else AlertDialog(onDismissRequest = dismiss, title = { Text("Add ${tracks.size} song(s)") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(filter, { filter = it; limit = 50 }, label = { Text("Find playlist") }, singleLine = true)
            Row { Checkbox(duplicates, { duplicates = it }); Text("Allow duplicate songs", Modifier.padding(top = 12.dp)) }
            if (error) Text("Cannot load playlists. Close and retry.", color = MaterialTheme.colorScheme.error)
            LazyColumn(Modifier.heightIn(max = 280.dp)) {
                items(playlists.take(limit), key = { it.id }) { playlist ->
                    TextButton({ actions.add(playlist.id, tracks, duplicates); dismiss() }, Modifier.fillMaxWidth()) {
                        Text("${playlist.name} (${playlist.songCount})")
                    }
                }
                if (playlists.size > limit) item { TextButton({ limit += 50 }) { Text("Load more") } }
            }
        }
    }, confirmButton = { TextButton({ create = true }) { Text("New playlist") } },
        dismissButton = { TextButton(dismiss) { Text("Cancel") } })
}
