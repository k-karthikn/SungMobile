package com.karthi.sungmobile.feature.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.data.backup.BackupRepository
import com.karthi.sungmobile.core.designsystem.component.SungSettingsGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupSettingsSection(repository: BackupRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var hasError by remember { mutableStateOf(false) }

    val exportPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            busy = true; status = null
            runCatching {
                val bytes = repository.export()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes) }
                        ?: error("Could not write the selected file.")
                }
            }.onSuccess {
                hasError = false; status = "Backup saved. Keep this file somewhere private."
            }.onFailure {
                hasError = true; status = it.message ?: "Could not export the backup."
            }
            busy = false
        }
    }
    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true; status = null
            runCatching {
                val bytes = withContext(Dispatchers.IO) { readBackup(context.contentResolver, uri) }
                repository.restore(bytes)
            }.onSuccess { result ->
                hasError = false
                status = "Backup applied. File includes ${result.likedSongs} likes, ${result.playlists} playlists and ${result.qualifiedPlays} listening records. Existing items were kept."
            }.onFailure {
                hasError = true; status = it.message ?: "Could not import this backup."
            }
            busy = false
        }
    }

    SungSettingsGroup("Import & export") {
        Text("Move preferences, likes, playlists and listening statistics to another device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton({ importPicker.launch(arrayOf("*/*")) }, Modifier.weight(1f), enabled = !busy) {
                Text("Import")
            }
            Button({ exportPicker.launch("SungMobile-${backupDate()}.json") }, Modifier.weight(1f), enabled = !busy) {
                Text("Export")
            }
        }
        status?.let {
            Text(it, style = MaterialTheme.typography.bodySmall,
                color = if (hasError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        }
    }
}

private fun backupDate() = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())

private fun readBackup(resolver: android.content.ContentResolver, uri: Uri): ByteArray {
    val limit = 25 * 1024 * 1024
    return resolver.openInputStream(uri)?.use { input ->
        val output = ByteArrayOutputStream()
        val chunk = ByteArray(8192)
        while (true) {
            val count = input.read(chunk)
            if (count < 0) break
            require(output.size() + count <= limit) { "Choose a backup under 25 MB." }
            output.write(chunk, 0, count)
        }
        output.toByteArray()
    } ?: error("Could not open the selected file.")
}
