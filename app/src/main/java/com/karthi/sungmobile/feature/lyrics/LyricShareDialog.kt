package com.karthi.sungmobile.feature.lyrics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun LyricShareDialog(
    initialQuote: String,
    trackTitle: String,
    sharing: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onShare: (String) -> Unit,
) {
    var quote by remember(trackTitle, initialQuote) { mutableStateOf(initialQuote) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create lyric card") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Choose the excerpt to include. The card also shows the song title and artist.",
                    style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = quote,
                    onValueChange = { quote = it.take(LYRIC_SHARE_MAX_LENGTH) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                    label = { Text("Lyric excerpt") },
                    minLines = 3,
                    maxLines = 7,
                    supportingText = { Text("${quote.length}/$LYRIC_SHARE_MAX_LENGTH") },
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error,
                    maxLines = 2, overflow = TextOverflow.Ellipsis) }
            }
        },
        confirmButton = {
            TextButton(onClick = { onShare(quote.trim()) }, enabled = quote.isNotBlank() && !sharing) {
                Text(if (sharing) "Preparing…" else "Share image")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !sharing) { Text("Cancel") } },
    )
}
