package com.karthi.sungmobile.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.designsystem.icon.SungIcon
import com.karthi.sungmobile.core.designsystem.icon.SungSymbol
import com.karthi.sungmobile.core.model.library.PlaylistRevision
import com.karthi.sungmobile.core.model.library.RevisionDetail

@Composable
internal fun PlaylistVersionsHeader(count: Int) {
    var showHelp by remember { mutableStateOf(false) }
    if (showHelp) AlertDialog(onDismissRequest = { showHelp = false },
        title = { Text("Saved versions") },
        text = { Text("A local version is saved before each name, song or order edit. Sung keeps the latest 20 on this phone. Deleting the playlist also deletes its versions.") },
        confirmButton = { TextButton({ showHelp = false }) { Text("Got it") } })
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Saved versions ($count)", Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge)
            TextButton({ showHelp = true }) { Text("About") }
        }
        if (count == 0) Text("No saved versions yet",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun PlaylistVersionRow(revision: PlaylistRevision, onOpen: () -> Unit) {
    Surface(onClick = onOpen, shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SungIcon(SungSymbol.Clock, modifier = Modifier.size(24.dp))
            Column(Modifier.weight(1f)) {
                Text(revision.name, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall)
                Text("${timestamp(revision.createdAt)} · ${revision.songCount} songs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SungIcon(SungSymbol.Chevron, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
internal fun PlaylistVersionPreview(model: AdvancedLibraryViewModel, state: AdvancedLibraryState,
    detail: RevisionDetail) {
    val currentIds = state.entries.map { it.rowId }
    val versionIds = detail.entries.map { it.id }
    val currentSet = currentIds.toSet()
    val versionSet = versionIds.toSet()
    val currentPositions = currentIds.withIndex().associate { it.value to it.index }
    val added = versionIds.count { it !in currentSet }
    val removed = currentIds.count { it !in versionSet }
    val moved = versionIds.withIndex().count {
        currentPositions[it.value]?.let { position -> position != it.index } == true
    }
    var confirm by remember(detail.revision.id) { mutableStateOf(false) }
    var showSongs by remember(detail.revision.id) { mutableStateOf(false) }
    if (confirm) ConfirmLibraryChange("Restore this playlist version?",
        "Current name and ordered entries will be saved first. This version has ${detail.entries.size} entries, including duplicates.",
        { confirm = false }, { model.restoreRevision(detail.revision.id); confirm = false })

    Surface(shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.tertiaryContainer) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Version preview", style = MaterialTheme.typography.titleMedium)
            Text(detail.revision.name, style = MaterialTheme.typography.titleSmall)
            Text("${detail.entries.size} songs · ${timestamp(detail.revision.createdAt)}",
                style = MaterialTheme.typography.bodyMedium)
            Text("Add $added · Remove $removed · Reorder $moved" +
                if (detail.revision.name != state.playlistName) " · Rename" else "",
                style = MaterialTheme.typography.bodyMedium)
            TextButton({ showSongs = !showSongs }) {
                Text(if (showSongs) "Hide songs" else "Show songs")
            }
            if (showSongs) {
                detail.entries.take(25).forEachIndexed { index, entry ->
                    Text("${index + 1}. ${entry.title}", maxLines = 1,
                        overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                }
                if (detail.entries.size > 25) Text("And ${detail.entries.size - 25} more",
                    style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Button({ confirm = true }, enabled = !state.busy) { Text("Restore version") }
                TextButton(model::hideRevision) { Text("Close") }
            }
        }
    }
}
