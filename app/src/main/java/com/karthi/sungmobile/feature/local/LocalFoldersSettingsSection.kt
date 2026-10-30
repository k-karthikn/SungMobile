package com.karthi.sungmobile.feature.local

import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.designsystem.component.SungSettingsGroup
import com.karthi.sungmobile.core.data.local.LocalAudioRepository

@Composable
fun LocalFoldersSettingsSection(repository: LocalAudioRepository) {
    var folders by remember(repository) { mutableStateOf(repository.addedFolders()) }
    var error by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching { repository.addFolder(uri) }
                .onSuccess { folders = repository.addedFolders(); error = false }
                .onFailure { error = true }
        }
    }
    SungSettingsGroup("Local songs") {
        Text("Music/SungMobile is included", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        folders.forEach { uri -> FolderRow(uri) {
            repository.removeFolder(uri)
            folders = repository.addedFolders()
        } }
        if (error) Text("Could not keep access to that folder. Select it again.",
            color = MaterialTheme.colorScheme.error)
        OutlinedButton({ picker.launch(null) }, modifier = Modifier.heightIn(min = 48.dp)) {
            Text("Add folder")
        }
    }
}

@Composable
private fun FolderRow(uri: Uri, remove: () -> Unit) {
    val name = remember(uri) { runCatching {
        DocumentsContract.getTreeDocumentId(uri).substringAfterLast('/')
    }.getOrDefault("Selected folder") }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(name, Modifier.weight(1f), maxLines = 1)
        TextButton(remove) { Text("Remove access") }
    }
}
