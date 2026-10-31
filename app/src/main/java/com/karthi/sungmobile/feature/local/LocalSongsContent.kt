package com.karthi.sungmobile.feature.local

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.karthi.sungmobile.core.data.local.LocalAudioRepository
import com.karthi.sungmobile.core.data.local.LocalScan
import com.karthi.sungmobile.core.data.local.LocalSong
import com.karthi.sungmobile.core.designsystem.component.SungFilterField
import com.karthi.sungmobile.core.designsystem.component.SungSongListItem
import com.karthi.sungmobile.core.designsystem.icon.SungIcon
import com.karthi.sungmobile.core.designsystem.icon.SungSymbol
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.feature.library.components.LibraryFilterToggle
import kotlinx.coroutines.launch

/** A live view of device audio, not a second library or download inventory. */
@Composable
fun LocalSongsContent(repository: LocalAudioRepository, connected: Boolean,
    play: (List<CatalogItem>, Int) -> Unit, enqueue: (CatalogItem, Boolean) -> Unit,
    modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var result by remember(repository) { mutableStateOf(repository.snapshot() ?: LocalScan(emptyList(), false)) }
    var loading by remember(repository) { mutableStateOf(repository.snapshot() == null) }
    var refreshing by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var filter by rememberSaveable { mutableStateOf("") }
    var filterExpanded by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(repository) { repository.updates.collect { scan -> if (scan != null) result = scan } }
    fun refresh(force: Boolean = false) { scope.launch {
        refreshing = true
        runCatching { repository.scan(force) }.onSuccess { result = it; message = null }
            .onFailure { message = "Could not scan local audio." }
        loading = false
        refreshing = false
    } }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh(true) }
    LaunchedEffect(repository) { refresh() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh() }
    val shown = result.songs.filter { filter.isBlank() ||
        it.track.title.contains(filter, true) || it.track.subtitle.contains(filter, true) ||
        it.album?.contains(filter, true) == true }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (filter.isBlank()) "${result.songs.size} local songs" else "${shown.size} matching songs",
                Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            LibraryFilterToggle(filterExpanded, filter.isNotBlank(),
                { filterExpanded = !filterExpanded }, "local song filter")
            IconButton({ refresh(true) }, Modifier.size(48.dp).testTag("refresh_local_songs"), enabled = !refreshing) {
                if (refreshing) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                else SungIcon(SungSymbol.Refresh, "Refresh local songs")
            }
        }
        AnimatedVisibility(filterExpanded, enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()) {
            SungFilterField(filter, { filter = it }, "Find local songs")
        }
        if (result.needsAudioPermission) {
            Text("Allow music access to show audio added by other apps.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            FilledTonalButton({ permission.launch(repository.audioPermission()) }) { Text("Allow music access") }
        }
        if (result.unreadableFolders > 0) Text("Some previously selected folders could not be read.",
            color = MaterialTheme.colorScheme.error)
        message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        else if (shown.isEmpty()) Text(if (filter.isNotBlank()) "No matching songs."
            else if (result.needsAudioPermission) "Allow music access to show audio on this device."
            else "No audio found on this device.",
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
