package com.karthi.sungmobile.feature.library.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.designsystem.component.SungFilterField
import com.karthi.sungmobile.core.model.library.LibrarySort

@Composable
internal fun LibraryPlaylistControls(filter: String, onFilter: (String) -> Unit,
    sort: LibrarySort, onSort: (LibrarySort) -> Unit,
    filtersExpanded: Boolean, onToggleFilters: () -> Unit, onNewPlaylist: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth()) {
            FilledTonalButton(onNewPlaylist, Modifier.heightIn(min = 48.dp)) { Text("New playlist") }
            Spacer(Modifier.weight(1f))
            LibraryFilterToggle(filtersExpanded, filter.isNotBlank() || sort != LibrarySort.DEFAULT, onToggleFilters)
        }
        AnimatedVisibility(filtersExpanded, enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SungFilterField(filter, onFilter, "Filter playlists")
                LibrarySortChoices(sort, onSort, false)
            }
        }
    }
}
