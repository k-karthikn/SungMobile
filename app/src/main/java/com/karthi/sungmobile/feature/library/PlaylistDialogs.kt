package com.karthi.sungmobile.feature.library

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.designsystem.component.SungConfirmDialog

@Composable
fun PlaylistNameDialog(title: String, initial: String = "", help: String = "", dismiss: () -> Unit, save: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(onDismissRequest = dismiss, title = { Text(title) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (help.isNotEmpty()) Text(help)
            OutlinedTextField(name, { name = it.take(120) }, label = { Text("Playlist name") }, singleLine = true)
            Text("${name.trim().length}/120", style = MaterialTheme.typography.bodySmall)
        }
    }, confirmButton = { TextButton({ save(name.trim()); dismiss() }, enabled = name.trim().isNotEmpty()) { Text("Save") } },
        dismissButton = { TextButton(dismiss) { Text("Cancel") } })
}

@Composable
fun ConfirmLibraryChange(title: String, body: String, dismiss: () -> Unit, confirm: () -> Unit) {
    SungConfirmDialog(title, body, "Confirm", { confirm(); dismiss() }, dismiss)
}
