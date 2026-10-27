package com.karthi.sungmobile.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.designsystem.artwork.SungArtwork
import com.karthi.sungmobile.core.model.library.LibrarySong
import com.karthi.sungmobile.core.model.library.duplicateEntryIds
import com.karthi.sungmobile.core.model.library.duplicateOccurrenceIds

internal fun LazyListScope.cleanup(model: AdvancedLibraryViewModel, state: AdvancedLibraryState) {
    val duplicateIds = duplicateOccurrenceIds(state.entries)
    val candidates = duplicateIds + state.confirmedUnavailable
    item("cleanup_overview") { PlaylistCleanupOverview(model, state, duplicateEntryIds(state.entries).size) }
    if (candidates.isNotEmpty()) item("review_header") {
        Text("Songs to review", style = MaterialTheme.typography.titleMedium)
    }
    items(state.entries.filter { it.rowId in candidates }, key = { "cleanup:${it.rowId}" }) { row ->
        CleanupCandidateRow(row, row.rowId in duplicateIds,
            row.rowId in state.confirmedUnavailable, row.rowId in state.selected,
            { model.toggleEntry(row.rowId) })
    }
    item("versions_header") { PlaylistVersionsHeader(state.revisions.size) }
    items(state.revisions, key = { "revision:${it.id}" }) { revision ->
        PlaylistVersionRow(revision, { model.showRevision(revision.id) })
    }
}

@Composable
private fun CleanupCandidateRow(row: LibrarySong, duplicate: Boolean, unavailable: Boolean,
    selected: Boolean, onSelected: () -> Unit) {
    val reason = listOfNotNull(if (duplicate) "Duplicate" else null,
        if (unavailable) "Unavailable" else null).joinToString(" · ")
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SungArtwork(row.track.artworkUrl, Modifier.size(48.dp))
            Column(Modifier.weight(1f)) {
                Text(row.track.title, style = MaterialTheme.typography.titleSmall,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(reason, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Checkbox(selected, { onSelected() })
        }
    }
}
