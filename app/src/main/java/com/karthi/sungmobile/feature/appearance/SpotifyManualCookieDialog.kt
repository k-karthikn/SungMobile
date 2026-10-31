package com.karthi.sungmobile.feature.appearance

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.data.spotify.extractSpotifySessionCookie

@Composable
internal fun SpotifyManualCookieDialog(onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    val context = LocalContext.current
    var value by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter Spotify session cookie") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Use your own Spotify sp_dc cookie. It is stored encrypted on this device.")
                OutlinedTextField(value, { value = it; error = false },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text("sp_dc cookie") }, isError = error)
                if (error) Text("Paste the sp_dc value or a cookie header containing sp_dc.")
                TextButton(onClick = { value = clipboardText(context); error = false }) {
                    Text("Paste from clipboard")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val cookie = normalizeCookie(value)
                if (cookie == null) error = true else onSubmit(cookie)
            }) { Text("Connect") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun normalizeCookie(input: String): String? {
    val trimmed = input.trim()
    return extractSpotifySessionCookie(trimmed)
        ?: trimmed.takeIf { it.length in 30..8_192 && ';' !in it && !it.any(Char::isWhitespace) }
}

private fun clipboardText(context: Context): String = runCatching {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
}.getOrDefault("")
