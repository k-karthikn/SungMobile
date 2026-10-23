package com.karthi.sungmobile.feature.library.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun LibrarySelectionBar(count: Int, done: () -> Unit, selectAll: () -> Unit,
    like: (Boolean) -> Unit, add: () -> Unit, enqueue: (Boolean) -> Unit, remove: (() -> Unit)?) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(12.dp)) {
            Text("$count selected", style = MaterialTheme.typography.titleSmall)
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                TextButton(done) { Text("Done") }
                TextButton(selectAll) { Text("Select loaded") }
                if (count > 0) {
                    TextButton({ like(true) }) { Text("Like") }
                    TextButton({ like(false) }) { Text("Unlike") }
                    TextButton(add) { Text("Add to playlist") }
                    TextButton({ enqueue(true) }) { Text("Play next") }
                    TextButton({ enqueue(false) }) { Text("Add to queue") }
                    if (remove != null) TextButton(remove) { Text("Remove") }
                }
            }
        }
    }
}
