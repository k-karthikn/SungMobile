package com.karthi.sungmobile.feature.library

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.model.library.*
import com.karthi.sungmobile.core.designsystem.component.SungLoading

internal fun androidx.compose.foundation.lazy.LazyListScope.cleanup(model: AdvancedLibraryViewModel, state: AdvancedLibraryState) {
    val duplicates = duplicateEntryIds(state.entries)
    val candidates = duplicateOccurrenceIds(state.entries) + state.confirmedUnavailable
    item {
        var removal by remember(state.playlistId) { mutableStateOf<Pair<Set<String>, Int>?>(null) }
        removal?.let { (ids, count) -> ConfirmLibraryChange("Remove ${ids.size} playlist entries?",
            "$count entries before; ${count - ids.size} after. Only selected occurrences are removed. You can restore an earlier local version later.",
            { removal = null }, { state.playlistId?.let { model.removeEntries(it, ids) } }) }
        Text(state.playlistName, style = MaterialTheme.typography.headlineMedium)
        Text("Cleanup and versions", style = MaterialTheme.typography.titleLarge)
        Text("${duplicates.size} duplicates · ${state.confirmedUnavailable.size} confirmed unavailable")
        Text("The first occurrence of each duplicate is kept by default. Select only the entries you want to remove.")
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            TextButton(model::selectDuplicates) { Text("Select duplicates") }
            TextButton(model::clearSelection) { Text("Clear selection") }
            Button({ removal = state.selected.toSet() to state.entries.size }, enabled = state.selected.isNotEmpty() && !state.busy) { Text("Review removal (${state.selected.size})") }
        }
        Text("Before ${state.entries.size} → after ${state.entries.size - state.selected.size}")
        HorizontalDivider()
        Text("Availability check", style = MaterialTheme.typography.titleMedium)
        Text("Checks up to 100 songs per batch on YouTube. Only confirmed unavailable songs become removal candidates. Network, rate and sign-in errors remain unknown.")
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            OutlinedButton(model::scanAvailability, enabled = !state.checking && state.checkedCount < state.entries.size) {
                Text(if (state.checkedCount == 0) "Check availability" else "Check next 100")
            }
            if (state.checking) TextButton(model::cancelScan) { Text("Cancel") }
            if (state.unknownChecks > 0 && !state.checking) TextButton(model::retryUnknown) { Text("Retry unknown") }
        }
        if (state.checking) SungLoading(label = "Checking availability…")
        Text("Checked ${state.checkedCount}/${state.entries.size}; ${state.unknownChecks} unknown")
        HorizontalDivider()
        Text("Review entries", style = MaterialTheme.typography.titleMedium)
        if (candidates.isEmpty()) Text("No cleanup candidates found yet.")
    }
    items(state.entries.filter { it.rowId in candidates }, key = { it.rowId }) { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(row.track.title, style = MaterialTheme.typography.titleSmall)
                Text(when (row.rowId) {
                    in state.confirmedUnavailable -> "Confirmed unavailable"
                    in duplicates -> "Duplicate occurrence"
                    else -> "First occurrence · kept by default"
                })
            }
            Checkbox(row.rowId in state.selected, { model.toggleEntry(row.rowId) })
        }
    }
    item {
        HorizontalDivider()
        Text("Local versions", style = MaterialTheme.typography.titleLarge)
        Text("A version is saved before each name, song or order edit. The latest 20 are kept on this phone. Deleting the playlist also deletes its versions.")
        if (state.revisions.isEmpty()) Text("No earlier versions yet.")
    }
    items(state.revisions, key = { it.id }) { revision ->
        OutlinedButton({ model.showRevision(revision.id) }, Modifier.fillMaxWidth()) {
            Text("${timestamp(revision.createdAt)} · ${revision.name} · ${revision.songCount} songs")
        }
    }
    state.revisionDetail?.let { detail -> item {
        val currentIds = state.entries.map { it.rowId }
        val versionIds = detail.entries.map { it.id }
        val currentSet = currentIds.toSet()
        val versionSet = versionIds.toSet()
        val currentPositions = currentIds.withIndex().associate { it.value to it.index }
        val restored = versionIds.count { it !in currentSet }
        val removed = currentIds.count { it !in versionSet }
        val moved = versionIds.withIndex().count { currentPositions[it.value]?.let { position -> position != it.index } == true }
        var confirm by remember(detail.revision.id) { mutableStateOf(false) }
        if (confirm) ConfirmLibraryChange("Restore this playlist version?",
            "Current name and ordered entries will be saved first. This version has ${detail.entries.size} entries, including duplicates.",
            { confirm = false }, { model.restoreRevision(detail.revision.id) })
        Text("Version preview", style = MaterialTheme.typography.titleMedium)
        Text("${detail.revision.name} · ${detail.entries.size} songs (current ${state.entries.size})")
        Text("Restore would add $restored entries, remove $removed and change $moved positions" +
            if (detail.revision.name != state.playlistName) "; rename the playlist" else "")
        detail.entries.take(25).forEachIndexed { index, entry -> Text("${index + 1}. ${entry.title}") }
        if (detail.entries.size > 25) Text("…and ${detail.entries.size - 25} more")
        Row {
            Button({ confirm = true }) { Text("Restore this version") }
            TextButton(model::hideRevision) { Text("Close preview") }
        }
    } }
}

