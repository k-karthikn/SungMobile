package com.karthi.sungmobile.feature.lyrics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.data.settings.LyricsSettings
import com.karthi.sungmobile.core.designsystem.component.SungConfirmDialog
import com.karthi.sungmobile.core.designsystem.component.SungSettingsGroup
import com.karthi.sungmobile.core.designsystem.component.SungSettingsToggle

@Composable
fun LyricsSettingsSection(value: LyricsSettings, onChange: (LyricsSettings) -> Unit,
    cacheBytes: Long, onClearCache: () -> Unit) {
    var confirmClear by remember { mutableStateOf(false) }
    var legal by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    SungSettingsGroup("Lyrics") {
            SungSettingsToggle("Use LRCLIB for local, missing or untimed lyrics", value.lrclibEnabled,
                { onChange(value.copy(lrclibEnabled = it)) },
                "Sends the title, artist and duration to LRCLIB. Private mode skips this request.")
            HorizontalDivider()
            SungSettingsToggle("Save lyric text on this device", value.cacheEnabled,
                { onChange(value.copy(cacheEnabled = it)) },
                "Turning this off clears saved lyric text. Timing offsets stay saved.")
            Text("Text size · ${value.textSizeSp} sp", style = MaterialTheme.typography.titleSmall)
            Slider(value.textSizeSp.toFloat(), { onChange(value.copy(textSizeSp = it.toInt())) }, valueRange = 18f..34f, steps = 15)
            Text("Lyrics cache: ${cacheBytes / 1024} KiB", style = MaterialTheme.typography.bodyMedium)
            OutlinedButton({ confirmClear = true }, Modifier.fillMaxWidth()) { Text("Clear lyrics cache") }
    }
    SungSettingsGroup("About") {
        Text("SungMobile · YouTube music playback · GPL-3.0-or-later", style = MaterialTheme.typography.bodyMedium)
        TextButton({ legal = "LICENSE.txt" }) { Text("App license") }
        TextButton({ legal = "THIRD_PARTY_NOTICES.txt" }) { Text("Open-source notices") }
    }
    if (confirmClear) SungConfirmDialog("Clear lyrics cache?",
        "This removes cached lyric text. Likes, playlists, queue and saved timing offsets remain.", "Clear",
        { onClearCache(); confirmClear = false }, { confirmClear = false })
    legal?.let { file ->
        val body = remember(file) { runCatching { context.assets.open("legal/$file").bufferedReader().use { it.readText() } }.getOrDefault("Notice unavailable") }
        AlertDialog(onDismissRequest = { legal = null }, title = { Text(if (file == "LICENSE.txt") "App license" else "Open-source notices") },
            text = { androidx.compose.foundation.text.selection.SelectionContainer {
                Text(body, Modifier.heightIn(max = 430.dp).verticalScroll(rememberScrollState()), style = MaterialTheme.typography.bodySmall)
            } }, confirmButton = { TextButton({ legal = null }) { Text("Close") } })
    }
}
