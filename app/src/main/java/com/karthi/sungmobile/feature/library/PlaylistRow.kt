package com.karthi.sungmobile.feature.library

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.model.library.LocalPlaylist

@Composable
internal fun PlaylistRow(playlist: LocalPlaylist, open: () -> Unit, actions: LibraryActionsViewModel) {
    var menu by remember { mutableStateOf(false) }
    var rename by remember { mutableStateOf(false) }
    var delete by remember { mutableStateOf(false) }
    if (rename) PlaylistNameDialog("Rename playlist", playlist.name, dismiss = { rename = false }, save = { actions.rename(playlist.id, it) })
    if (delete) ConfirmLibraryChange("Delete playlist?", "Songs in Likes and the playing queue will remain.",
        { delete = false }, { actions.delete(playlist.id) })
    Surface(onClick = open, color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.medium) {
        Row(Modifier.fillMaxWidth().padding(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(playlist.name, style = MaterialTheme.typography.titleMedium)
                Text("${playlist.songCount} songs${if (playlist.pinned) " • Pinned" else ""}")
            }
            Box {
                TextButton({ menu = true }) { Text("More") }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem(text = { Text(if (playlist.pinned) "Unpin" else "Pin") }, onClick = { actions.pin(playlist); menu = false })
                    DropdownMenuItem(text = { Text("Rename") }, onClick = { rename = true; menu = false })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { delete = true; menu = false })
                }
            }
        }
    }
}
