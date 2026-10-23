package com.karthi.sungmobile.feature.convenience

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import com.karthi.sungmobile.core.designsystem.component.SungConfirmDialog
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.designsystem.icon.*
import com.karthi.sungmobile.core.model.SavedSession
import com.karthi.sungmobile.core.playback.PlaybackState

@Composable
fun SessionsScreen(
    state: ConvenienceUiState,
    playback: PlaybackState,
    onBack: () -> Unit,
    onSave: (String, PlaybackState) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onRestore: (String, (Boolean) -> Unit) -> Unit,
    onClearMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var saveName by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<SavedSession?>(null) }
    var rename by remember { mutableStateOf<SavedSession?>(null) }
    var delete by remember { mutableStateOf<SavedSession?>(null) }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack) { SungIcon(SungSymbol.Back, "Back") }
            Text("Saved sessions", style = MaterialTheme.typography.headlineMedium)
        }
        Text("Save this queue and return to it later. Restoring starts paused.",
            style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(saveName, { saveName = it.take(80) }, label = { Text("Session name") },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        FilledTonalButton({ onSave(saveName, playback); saveName = "" },
            enabled = saveName.isNotBlank() && playback.queue.entries.isNotEmpty() &&
                playback.queue.entries.size <= 500 && state.sessions.size < 20) { Text("Save current session") }
        if (playback.queue.entries.size > 500) Text("Queue is too large to save (500 entries maximum).",
            color = MaterialTheme.colorScheme.error)
        state.message?.let { message -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text(message, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClearMessage) { Text("Dismiss") }
        } }
        if (state.sessions.isEmpty()) Text("No saved sessions yet.")
        state.sessions.forEach { session ->
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(session.name, style = MaterialTheme.typography.titleMedium)
                    Text("${session.snapshot.queue.entries.size} songs • ${session.speed.multiplier}×",
                        style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilledTonalButton({ selected = session }) { Text("Open") }
                        TextButton({ rename = session }) { Text("Rename") }
                        TextButton({ delete = session }) { Text("Delete") }
                    }
                }
            }
        }
    }
    selected?.let { session ->
        AlertDialog(onDismissRequest = { selected = null }, title = { Text(session.name) },
            text = { Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                Text("Restore replaces the current queue and starts paused at the saved position.")
                Spacer(Modifier.height(8.dp))
                session.snapshot.queue.entries.forEachIndexed { index, entry ->
                    Text("${index + 1}. ${entry.track.title} — ${entry.track.subtitle}",
                        style = MaterialTheme.typography.bodySmall)
                }
            } },
            confirmButton = { TextButton({ onRestore(session.id) { success ->
                selected = null
                if (success) onBack()
            } }) { Text("Restore paused") } },
            dismissButton = { TextButton({ selected = null }) { Text("Cancel") } })
    }
    rename?.let { session ->
        var name by remember(session.id) { mutableStateOf(session.name) }
        AlertDialog(onDismissRequest = { rename = null }, title = { Text("Rename session") },
            text = { OutlinedTextField(name, { name = it.take(80) }, label = { Text("Name") }, singleLine = true) },
            confirmButton = { TextButton({ onRename(session.id, name); rename = null }, enabled = name.isNotBlank()) { Text("Save") } },
            dismissButton = { TextButton({ rename = null }) { Text("Cancel") } })
    }
    delete?.let { session ->
        SungConfirmDialog("Delete saved session?", "The current queue and your library will remain.", "Delete",
            { onDelete(session.id); delete = null }, { delete = null })
    }
}
