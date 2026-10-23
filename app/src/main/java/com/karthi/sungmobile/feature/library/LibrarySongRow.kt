package com.karthi.sungmobile.feature.library

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.designsystem.component.SungSongListItem
import com.karthi.sungmobile.core.designsystem.icon.*
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.karthi.sungmobile.core.model.library.*

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LibrarySongRow(row: LibrarySong, selected: Boolean, select: (Boolean) -> Unit,
    play: () -> Unit, next: () -> Unit, append: () -> Unit, move: ((Int) -> Unit)?, remove: (() -> Unit)?,
    modifier: Modifier = Modifier, selectionMode: Boolean = false, playbackEnabled: Boolean = true) {
    var menu by remember { mutableStateOf(false) }
    val library = LocalLibraryUi.current
    SungSongListItem(row.track.title, row.track.subtitle, row.track.artworkUrl,
        { if (selectionMode) select(!selected) else play() }, modifier, selected = selected, enabled = selectionMode || playbackEnabled) {
            if (selectionMode) Checkbox(selected, select, Modifier.semantics { contentDescription = "Select ${row.track.title}" })
            else IconToggleButton(row.liked, { library.toggle(row.track) }, shapes = IconButtonDefaults.toggleableShapes()) {
                SungIcon(if (row.liked) SungSymbol.HeartFilled else SungSymbol.Heart,
                    "${if (row.liked) "Unlike" else "Like"} ${row.track.title}")
            }
            Box {
                IconButton({ menu = true }, shapes = IconButtonDefaults.shapes()) { SungIcon(SungSymbol.More, "Options for ${row.track.title}") }
                DropdownMenu(menu, { menu = false }) {
                    LibraryMenuItems(row.track) { menu = false }
                    DropdownMenuItem(text = { Text("Play next") }, enabled = playbackEnabled, onClick = { next(); menu = false })
                    DropdownMenuItem(text = { Text("Add to queue") }, enabled = playbackEnabled, onClick = { append(); menu = false })
                    if (move != null) {
                        DropdownMenuItem(text = { Text("Move up") }, onClick = { move(-1); menu = false })
                        DropdownMenuItem(text = { Text("Move down") }, onClick = { move(1); menu = false })
                    }
                    if (remove != null) DropdownMenuItem(text = { Text("Remove from playlist") }, onClick = { remove(); menu = false })
                }
            }
    }
}
