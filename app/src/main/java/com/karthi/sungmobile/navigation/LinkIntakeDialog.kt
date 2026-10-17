package com.karthi.sungmobile.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.model.catalog.OpenMusicLink

@Composable
fun LinkIntakeDialog(state: LinkIntakeState, dismiss: () -> Unit, open: (OpenMusicLink) -> Unit) {
    if (state.choices.isEmpty() && state.error == null) return
    AlertDialog(onDismissRequest = dismiss, title = { Text("Open YouTube link") }, text = {
        if (state.error != null) Text(state.error)
        else LazyColumn(Modifier.heightIn(max = 360.dp)) {
            itemsIndexed(state.choices) { _, link ->
                TextButton({ open(link); dismiss() }) {
                    Column {
                        Text(link.label + if (link.startMs > 0) " · starts at ${link.startMs / 1000}s when played" else "")
                        Text(link.url, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }, confirmButton = { TextButton(dismiss) { Text("Cancel") } })
}
