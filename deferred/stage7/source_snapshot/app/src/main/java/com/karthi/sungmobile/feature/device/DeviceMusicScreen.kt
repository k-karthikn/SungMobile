package com.karthi.sungmobile.feature.device

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.playback.PlaybackControl
import com.karthi.sungmobile.feature.library.LocalLibraryUi

@Composable
fun DeviceMusicScreen(model: DeviceMusicViewModel, player: PlaybackControl, back: () -> Unit,
    modifier: Modifier = Modifier) {
    val state by model.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val library = LocalLibraryUi.current
    val lifecycle = LocalLifecycleOwner.current
    var filter by remember { mutableStateOf("") }
    var permission by remember { mutableStateOf(model.hasPermission()) }
    var pickerMode by remember { mutableStateOf("INCLUDE") }
    var pickerError by remember { mutableStateOf<String?>(null) }
    val permissionRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permission = model.hasPermission()
        if (permission) model.scan()
    }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            pickerError = null
            if (pickerMode == "DESTINATION" && uri.authority != "com.android.externalstorage.documents") {
                pickerError = "Choose a folder on this device or SD card for downloads."
            } else {
                val flags = if (pickerMode == "DESTINATION")
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                else Intent.FLAG_GRANT_READ_URI_PERMISSION
                val granted = runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }.isSuccess
                if (!granted) pickerError = "Folder access was not granted. Choose it again."
                else when (pickerMode) {
                    "INCLUDE" -> model.addFolder(uri, false)
                    "EXCLUDE" -> model.addFolder(uri, true)
                    "DESTINATION" -> model.update { it.copy(downloadTreeUri = uri.toString()) }
                }
            }
        }
    }
    LaunchedEffect(Unit) { permission = model.hasPermission(); model.autoScan() }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permission = model.hasPermission()
                model.autoScan()
            }
        }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }
    val visible = state.songs.filter { filter.isBlank() || it.track.title.contains(filter, true) ||
        it.track.subtitle.contains(filter, true) }
    val playable = visible.filter { it.available }.map { it.track }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            TextButton(back) { Text("Back") }
            Text("Device music", style = MaterialTheme.typography.headlineMedium)
            Text("Music/ and chosen folders · files remain yours", style = MaterialTheme.typography.bodyMedium)
            if (!permission && state.settings.scanMusic) {
                Text("Allow audio access to show songs in Music/. You can still choose a folder directly.")
                Button({ permissionRequest.launch(if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
                    else Manifest.permission.READ_EXTERNAL_STORAGE) }) { Text("Allow audio access") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Include Music/ by default")
                Switch(state.settings.scanMusic, { model.update { old -> old.copy(scanMusic = it) } })
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Automatic scanning")
                Switch(state.settings.autoScan, { model.update { old -> old.copy(autoScan = it) } })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton({ model.scan() }, enabled = !state.scanning) { Text("Scan now") }
                TextButton({ pickerMode = "INCLUDE"; folderPicker.launch(null) }) { Text("Add folder") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton({ pickerMode = "EXCLUDE"; folderPicker.launch(null) }) { Text("Exclude folder") }
                TextButton({ pickerMode = "DESTINATION"; folderPicker.launch(null) }) { Text("Change download folder") }
            }
            if (state.settings.downloadTreeUri == null) Text(if (Build.VERSION.SDK_INT >= 29)
                "New downloads: Music/SungMobile/" else "Choose a download folder for this Android version")
            else Text("New downloads: ${folderName(state.settings.downloadTreeUri!!)}")
            if (state.settings.downloadTreeUri != null)
                TextButton({ model.update { it.copy(downloadTreeUri = null) } }) { Text("Use Music/SungMobile/ for new downloads") }
            state.folders.forEach { folder ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${if (folder.exclude) "Exclude" else "Include"}: ${folder.name}", Modifier.weight(1f))
                    TextButton({ model.removeFolder(folder.uri) }) { Text("Remove rule") }
                }
            }
            if (state.scanning) LinearProgressIndicator(Modifier.fillMaxWidth())
            pickerError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            state.lastScanCount?.let { Text("Last scan: $it audio files found") }
            OutlinedTextField(filter, { filter = it }, Modifier.fillMaxWidth(), label = { Text("Filter device music") }, singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton({ player.select(playable, 0) }, enabled = playable.isNotEmpty()) { Text("Play all") }
                Text("${visible.size} shown", Modifier.padding(top = 12.dp))
            }
            if (visible.isEmpty()) Text("No device audio found in the included folders.")
        }
        items(visible, key = { it.track.url }) { row ->
            var menu by remember { mutableStateOf(false) }
            Surface(onClick = { if (row.available) {
                val index = playable.indexOfFirst { it.url == row.track.url }
                if (index >= 0) player.select(playable, index)
            } }, color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(row.track.title, style = MaterialTheme.typography.titleMedium)
                        Text(row.track.subtitle.ifBlank { if (row.available) "Device audio" else "File unavailable · scan or restore folder access" },
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Box {
                        TextButton({ menu = true }) { Text("More") }
                        DropdownMenu(menu, { menu = false }) {
                            DropdownMenuItem(text = { Text("Like / unlike") }, onClick = { library.toggle(row.track); menu = false })
                            DropdownMenuItem(text = { Text("Add to playlist") }, onClick = { library.add(listOf(row.track)); menu = false })
                            DropdownMenuItem(text = { Text("Play next") }, onClick = { player.enqueue(row.track, true); menu = false }, enabled = row.available)
                            DropdownMenuItem(text = { Text("Add to queue") }, onClick = { player.enqueue(row.track, false); menu = false }, enabled = row.available)
                        }
                    }
                }
            }
        }
    }
}

private fun folderName(value: String): String = runCatching {
    DocumentsContract.getTreeDocumentId(android.net.Uri.parse(value)).substringAfter(':')
}.getOrDefault("Selected folder")
