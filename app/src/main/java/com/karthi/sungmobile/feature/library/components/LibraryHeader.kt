package com.karthi.sungmobile.feature.library.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.designsystem.icon.*

internal enum class LibraryDestination(val label: String) {
    LIKED("Liked"), PLAYLISTS("Playlists"), LOCAL("Local songs"), MIXES("Mixes"),
    RECENTS("Recents"), SESSIONS("Sessions")
}

/** Library tabs change the visible collection; Settings and Search stay in the agreed top actions. */
@Composable
internal fun LibraryHeader(selected: LibraryDestination, open: (LibraryDestination) -> Unit,
    settings: () -> Unit, search: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Library", Modifier.weight(1f), style = MaterialTheme.typography.displaySmall)
        IconButton(settings, Modifier.size(48.dp)) { SungIcon(SungSymbol.Settings, "Settings") }
        IconButton(search, Modifier.size(48.dp)) { SungIcon(SungSymbol.Search, "Search") }
    }
    SecondaryScrollableTabRow(selectedTabIndex = selected.ordinal, edgePadding = 0.dp, divider = {}) {
        LibraryDestination.entries.forEach { destination ->
            Tab(selected == destination, { open(destination) }, text = { Text(destination.label) },
                modifier = Modifier.heightIn(min = 48.dp))
        }
    }
}
