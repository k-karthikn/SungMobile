package com.karthi.sungmobile.feature.library

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.designsystem.artwork.SungArtwork
import com.karthi.sungmobile.core.model.library.*

@Composable
internal fun LibrarySongRow(row: LibrarySong, selected: Boolean, select: (Boolean) -> Unit,
    play: () -> Unit, next: () -> Unit, append: () -> Unit, move: ((Int) -> Unit)?, remove: (() -> Unit)?,
    modifier: Modifier = Modifier) {
    var menu by remember { mutableStateOf(false) }
    Surface(onClick = play, modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(selected, select)
            SungArtwork(row.track.artworkUrl, Modifier.size(40.dp))
            Column(Modifier.weight(1f).padding(8.dp)) {
                Text(row.track.title, style = MaterialTheme.typography.titleMedium)
                Text(row.track.subtitle, style = MaterialTheme.typography.bodySmall)
                if (row.liked) Text("Liked", style = MaterialTheme.typography.labelSmall)
            }
            Box {
                TextButton({ menu = true }) { Text("More") }
                DropdownMenu(menu, { menu = false }) {
                    LibraryMenuItems(row.track) { menu = false }
                    DropdownMenuItem(text = { Text("Play next") }, onClick = { next(); menu = false })
                    DropdownMenuItem(text = { Text("Add to queue") }, onClick = { append(); menu = false })
                    if (move != null) {
                        DropdownMenuItem(text = { Text("Move up") }, onClick = { move(-1); menu = false })
                        DropdownMenuItem(text = { Text("Move down") }, onClick = { move(1); menu = false })
                    }
                    if (remove != null) DropdownMenuItem(text = { Text("Remove from playlist") }, onClick = { remove(); menu = false })
                }
            }
        }
    }
}
