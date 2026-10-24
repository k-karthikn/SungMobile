package com.karthi.sungmobile.feature.library.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.designsystem.component.SungFilterField
import com.karthi.sungmobile.core.designsystem.icon.*
import com.karthi.sungmobile.core.model.library.LibrarySort

/** List-only controls; collection commands still use the ViewModel's full snapshot. */
@Composable
internal fun LibraryListControls(filter: String, filterLabel: String, onFilter: (String) -> Unit,
    sort: LibrarySort, onSort: (LibrarySort) -> Unit, playlistOrder: Boolean,
    hasSongs: Boolean, onPlay: () -> Unit, onShuffle: () -> Unit,
    onPlayNext: () -> Unit, onAddToQueue: () -> Unit, onSelect: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LibraryPlaySplitButton(hasSongs, onPlay, onShuffle, onAddToQueue, onPlayNext)
            Spacer(Modifier.weight(1f))
            TextButton(onSelect, enabled = hasSongs, modifier = Modifier.heightIn(min = 48.dp)) { Text("Select") }
        }
        SungFilterField(filter, onFilter, filterLabel)
        LibrarySortChoices(sort, onSort, playlistOrder)
    }
}

@Composable
internal fun LibrarySortChoices(sort: LibrarySort, onSort: (LibrarySort) -> Unit, playlistOrder: Boolean) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(sort == LibrarySort.DEFAULT, { onSort(LibrarySort.DEFAULT) },
            label = { Text(if (playlistOrder) "Playlist order" else "Latest") },
            leadingIcon = if (sort == LibrarySort.DEFAULT) ({ SungIcon(SungSymbol.Check) }) else null,
            modifier = Modifier.heightIn(min = 48.dp))
        FilterChip(sort == LibrarySort.TITLE, { onSort(LibrarySort.TITLE) }, label = { Text("A–Z") },
            leadingIcon = if (sort == LibrarySort.TITLE) ({ SungIcon(SungSymbol.Check) }) else null,
            modifier = Modifier.heightIn(min = 48.dp))
    }
}
