package com.karthi.sungmobile.feature.lyrics

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.data.settings.LyricsSettings
import com.karthi.sungmobile.core.designsystem.component.SungConfirmDialog
import com.karthi.sungmobile.core.designsystem.component.SungSettingsGroup
import com.karthi.sungmobile.core.designsystem.component.SungSettingsToggle

@Composable
fun LyricsSettingsSection(value: LyricsSettings, onChange: (LyricsSettings) -> Unit,
    cacheBytes: Long, onClearCache: () -> Unit) {
    var confirmClear by remember { mutableStateOf(false) }
    SungSettingsGroup("Lyrics") {
            SungSettingsToggle("Find lyrics online", value.lrclibEnabled,
                { onChange(value.copy(lrclibEnabled = it)) },
                "Shares song details with LRCLIB")
            HorizontalDivider()
            SungSettingsToggle("Save lyrics offline", value.cacheEnabled,
                { onChange(value.copy(cacheEnabled = it)) },
                "Turning off clears saved lyrics")
            HorizontalDivider()
            Text("Text size · ${value.textSizeSp} sp", style = MaterialTheme.typography.titleSmall)
            Slider(value.textSizeSp.toFloat(), { onChange(value.copy(textSizeSp = it.toInt())) },
                valueRange = 18f..34f, steps = 15)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Cache · ${cacheBytes / 1024} KiB", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton({ confirmClear = true }) { Text("Clear cache") }
            }
    }
    if (confirmClear) SungConfirmDialog("Clear lyrics cache?",
        "This removes cached lyric text. Likes, playlists, queue and saved timing offsets remain.", "Clear",
        { onClearCache(); confirmClear = false }, { confirmClear = false })
}
