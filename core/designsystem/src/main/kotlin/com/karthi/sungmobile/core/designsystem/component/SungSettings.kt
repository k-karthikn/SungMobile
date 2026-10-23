package com.karthi.sungmobile.core.designsystem.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Shared section surface for settings; callers own their switches and persistence. */
@Composable
fun SungSettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Surface(shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainer) {
            Column(Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}

@Composable
fun SungConfirmDialog(title: String, body: String, confirmLabel: String,
    onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, shape = MaterialTheme.shapes.extraLarge,
        title = { Text(title) }, text = { Text(body) },
        confirmButton = { Button(onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } })
}
