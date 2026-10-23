package com.karthi.sungmobile.feature.library.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.designsystem.icon.*
import com.karthi.sungmobile.core.model.library.LibraryCollection

@Composable
internal fun LibraryHeader(collection: LibraryCollection?, open: (LibraryCollection?) -> Unit,
    sessions: () -> Unit, history: () -> Unit, smart: () -> Unit, statistics: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Library", Modifier.weight(1f), style = MaterialTheme.typography.displaySmall)
        Box {
            IconButton({ menu = true }) { SungIcon(SungSymbol.More, "Library options") }
            DropdownMenu(menu, { menu = false }) {
                listOf("History & privacy" to history, "Smart playlists" to smart, "Listening statistics" to statistics).forEach { (label, action) ->
                    DropdownMenuItem(text = { Text(label) }, onClick = { menu = false; action() })
                }
            }
        }
    }
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LibraryTab("Liked", SungSymbol.HeartFilled, collection == LibraryCollection.Likes) { open(LibraryCollection.Likes) }
        LibraryTab("Playlists", SungSymbol.QueueMusic, collection == null || collection is LibraryCollection.Playlist) { open(null) }
        LibraryTab("Recents", SungSymbol.Refresh, collection == LibraryCollection.Recent) { open(LibraryCollection.Recent) }
        LibraryTab("Sessions", SungSymbol.Bookmark, false, sessions)
    }
}

@Composable
private fun LibraryTab(label: String, icon: SungSymbol, selected: Boolean, click: () -> Unit) {
    FilterChip(selected, click, label = { Text(label) }, leadingIcon = { SungIcon(icon) },
        shape = CircleShape, modifier = Modifier.heightIn(min = 48.dp))
}
