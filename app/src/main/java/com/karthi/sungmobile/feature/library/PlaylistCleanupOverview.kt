package com.karthi.sungmobile.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.designsystem.component.SungLoading

@Composable
internal fun PlaylistCleanupOverview(model: AdvancedLibraryViewModel, state: AdvancedLibraryState,
    duplicateCount: Int) {
    var removal by remember(state.playlistId) { mutableStateOf<Pair<Set<String>, Int>?>(null) }
    var showHelp by remember { mutableStateOf(false) }
    removal?.let { (ids, count) -> ConfirmLibraryChange("Remove ${ids.size} playlist entries?",
        "$count entries before; ${count - ids.size} after. Only selected occurrences are removed. You can restore an earlier local version later.",
        { removal = null }, { state.playlistId?.let { model.removeEntries(it, ids) }; removal = null }) }
    if (showHelp) AlertDialog(onDismissRequest = { showHelp = false },
        title = { Text("How cleanup works") },
        text = { Text("Duplicate cleanup keeps the first copy. Availability checks use YouTube in batches of 100; network and sign-in errors are never marked unavailable.") },
        confirmButton = { TextButton({ showHelp = false }) { Text("Got it") } })

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column {
            Text(state.playlistName, style = MaterialTheme.typography.headlineMedium)
            Text("Cleanup & versions", color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium)
        }
        if (state.entries.isEmpty()) {
            Surface(shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Text("Add songs to this playlist to use cleanup.", Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium)
            }
            return@Column
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CleanupCount("Duplicates", duplicateCount, Modifier.weight(1f))
            CleanupCount("Unavailable", state.confirmedUnavailable.size, Modifier.weight(1f))
        }
        if (duplicateCount > 0 || state.confirmedUnavailable.isNotEmpty() || state.selected.isNotEmpty()) {
            Surface(shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Review & remove", style = MaterialTheme.typography.titleMedium)
                    Text("${state.selected.size} selected · ${state.entries.size} songs in playlist",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (duplicateCount > 0) TextButton(model::selectDuplicates, enabled = !state.busy) {
                            Text("Select duplicates")
                        }
                        if (state.selected.isNotEmpty()) TextButton(model::clearSelection) { Text("Clear") }
                    }
                    if (state.selected.isNotEmpty()) Button({ removal = state.selected.toSet() to state.entries.size },
                        enabled = !state.busy) { Text("Review removal (${state.selected.size})") }
                }
            }
        }
        Surface(shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Check availability", style = MaterialTheme.typography.titleMedium)
                Text("${state.checkedCount}/${state.entries.size} checked" +
                    if (state.unknownChecks > 0) " · ${state.unknownChecks} unknown" else "",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium)
                if (state.checking) SungLoading(label = "Checking availability…")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.checking) OutlinedButton(model::cancelScan) { Text("Cancel") }
                    else OutlinedButton(model::scanAvailability,
                        enabled = state.checkedCount < state.entries.size) {
                        Text(if (state.checkedCount == 0) "Check songs" else "Check next 100")
                    }
                    if (state.unknownChecks > 0 && !state.checking)
                        TextButton(model::retryUnknown) { Text("Retry unknown") }
                }
                TextButton({ showHelp = true }) { Text("How cleanup works") }
            }
        }
    }
}

@Composable
private fun CleanupCount(label: String, count: Int, modifier: Modifier = Modifier) {
    Surface(modifier, shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer) {
        Column(Modifier.padding(14.dp)) {
            Text(count.toString(), style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer)
            Text(label, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}
