package com.karthi.sungmobile.feature.downloads

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.karthi.sungmobile.core.model.lyrics.lyricsVideoId
import com.karthi.sungmobile.core.playback.PlaybackState
import com.karthi.sungmobile.feature.device.DeviceMusicViewModel

@Composable
fun DownloadsScreen(model: DownloadViewModel, device: DeviceMusicViewModel, playback: PlaybackState,
    back: () -> Unit, modifier: Modifier = Modifier) {
    val inventory by model.state.collectAsStateWithLifecycle()
    val deviceState by device.state.collectAsStateWithLifecycle()
    var pendingRemoval by remember { mutableStateOf<String?>(null) }
    pendingRemoval?.let { id -> AlertDialog(onDismissRequest = { pendingRemoval = null },
        title = { Text("Delete downloaded file?") },
        text = { Text("A completed download's audio file will be deleted from device storage. Likes, playlists and history stay.") },
        confirmButton = { TextButton({ model.remove(id); pendingRemoval = null }) { Text("Delete file") } },
        dismissButton = { TextButton({ pendingRemoval = null }) { Text("Cancel") } }) }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            TextButton(back) { Text("Back") }
            Text("Downloads", style = MaterialTheme.typography.headlineMedium)
            Text("Completed audio is visible in Music/SungMobile/ or your chosen folder.")
            Text("Used: ${mb(inventory.usedBytes)} MiB · Budget: ${mb(deviceState.settings.budgetBytes)} MiB")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Wi-Fi only")
                Switch(deviceState.settings.wifiOnly, { value -> device.update { it.copy(wifiOnly = value) } })
            }
            var budgetMenu by remember { mutableStateOf(false) }
            Box {
                TextButton({ budgetMenu = true }) { Text("Storage budget") }
                DropdownMenu(budgetMenu, { budgetMenu = false }) {
                    listOf(512L, 1024L, 2048L, 4096L, 8192L).forEach { amount ->
                        DropdownMenuItem(text = { Text("$amount MiB") }, onClick = {
                            device.update { it.copy(budgetBytes = amount * 1024 * 1024) }; budgetMenu = false
                        })
                    }
                }
            }
            if (inventory.items.isEmpty()) Text("No downloads yet. Open a YouTube song and choose Download.")
        }
        items(inventory.items, key = { it.videoId }) { item ->
            val current = playback.track?.lyricsVideoId() == item.videoId && playback.engine.ready
            Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.medium) {
                Column(Modifier.padding(12.dp)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                    Text("${item.state.lowercase().replaceFirstChar(Char::uppercase)} · ${mb(item.bytes)} MiB")
                    item.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (item.state in setOf("QUEUED", "RUNNING", "PAUSED"))
                            TextButton({ model.cancel(item.videoId) }) { Text("Cancel") }
                        if (item.state in setOf("FAILED", "CANCELED", "MISSING"))
                            TextButton({ model.retry(item.videoId) }) { Text("Retry") }
                        if (item.state !in setOf("QUEUED", "RUNNING", "PAUSED"))
                            TextButton({ pendingRemoval = item.videoId }, enabled = !current) { Text("Remove") }
                    }
                    if (current) Text("Switch songs before deleting the playing file.")
                }
            }
        }
    }
}

private fun mb(bytes: Long): Long = bytes / (1024 * 1024)
