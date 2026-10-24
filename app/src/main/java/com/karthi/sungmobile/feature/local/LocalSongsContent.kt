package com.karthi.sungmobile.feature.local

import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.karthi.sungmobile.core.designsystem.component.SungFilterField
import com.karthi.sungmobile.core.designsystem.component.SungSongListItem
import com.karthi.sungmobile.core.model.CatalogItem
import kotlinx.coroutines.launch

/** A live view of device audio, not a second library or download inventory. */
@Composable
fun LocalSongsContent(repository: LocalAudioRepository, connected: Boolean,
    play: (List<CatalogItem>, Int) -> Unit, enqueue: (CatalogItem, Boolean) -> Unit,
    modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var result by remember { mutableStateOf(LocalScan(emptyList(), false)) }
    var loading by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf<String?>(null) }
    var filter by remember { mutableStateOf("") }
    var folders by remember { mutableStateOf(repository.addedFolders()) }
    fun refresh() { scope.launch {
        loading = true
        runCatching { repository.scan() }.onSuccess { result = it; message = null }
            .onFailure { message = "Could not scan local audio." }
        loading = false
    } }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh() }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching { repository.addFolder(uri) }
                .onSuccess { folders = repository.addedFolders(); refresh() }
                .onFailure { message = "Could not keep access to that folder. Try selecting it again." }
        }
    }
    LaunchedEffect(repository) { refresh() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh() }
    val shown = result.songs.filter { filter.isBlank() ||
        it.track.title.contains(filter, true) || it.track.subtitle.contains(filter, true) ||
        it.album?.contains(filter, true) == true }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Local songs", style = MaterialTheme.typography.headlineSmall)
        Text("Music/SungMobile and your selected folders", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(::refresh) { Text("Refresh") }
            OutlinedButton({ picker.launch(null) }) { Text("Add folder") }
        }
        if (result.needsAudioPermission) {
            Text("Allow music access to show audio added by other apps.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            FilledTonalButton({ permission.launch(repository.audioPermission()) }) { Text("Allow music access") }
        }
        if (folders.isNotEmpty()) {
            Text("Extra folders", style = MaterialTheme.typography.titleMedium)
            folders.forEach { uri ->
                val name = runCatching { DocumentsContract.getTreeDocumentId(uri).substringAfterLast('/') }
                    .getOrDefault("Selected folder")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(name, Modifier.weight(1f).padding(top = 12.dp), maxLines = 1)
                    TextButton({ repository.removeFolder(uri); folders = repository.addedFolders(); refresh() }) {
                        Text("Remove access")
                    }
                }
            }
        }
        if (result.unreadableFolders > 0) Text("Some selected folders could not be read. Remove and add them again.",
            color = MaterialTheme.colorScheme.error)
        message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        SungFilterField(filter, { filter = it }, "Find local songs")
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        else if (shown.isEmpty()) Text(if (filter.isNotBlank()) "No matching songs."
            else if (result.needsAudioPermission) "Allow music access to show songs in Music/SungMobile."
            else "No audio found. Add files to Music/SungMobile or choose another folder.",
            Modifier.padding(vertical = 20.dp), style = MaterialTheme.typography.bodyLarge)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp)) {
            itemsIndexed(shown, key = { _, song -> song.identity }) { index, song ->
                var menu by remember(song.identity) { mutableStateOf(false) }
                SungSongListItem(song.track.title,
                    listOfNotNull(song.track.subtitle, song.album,
                        song.track.durationMs?.let(::formatLocalDuration)).joinToString(" • "),
                    song.track.artworkUrl, { play(shown.map(LocalSong::track), index) },
                    Modifier.testTag("local_song_${song.identity}"), enabled = connected) {
                    Box {
                        IconButton({ menu = true }) { Text("⋮") }
                        DropdownMenu(menu, { menu = false }) {
                            DropdownMenuItem(text = { Text("Play next") }, enabled = connected,
                                onClick = { menu = false; enqueue(song.track, true) })
                            DropdownMenuItem(text = { Text("Add to queue") }, enabled = connected,
                                onClick = { menu = false; enqueue(song.track, false) })
                        }
                    }
                }
            }
        }
    }
}

internal fun formatLocalDuration(milliseconds: Long): String {
    val seconds = milliseconds.coerceAtLeast(0) / 1000
    return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}
