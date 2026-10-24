package com.karthi.sungmobile.feature.convenience

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.data.settings.AdvancedPlaybackSettings
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.lyrics.lyricsVideoId

/** The existing per-song trim, shared by the full-player and mini-player menus. */
@Composable
fun TrackVolumeControl(track: CatalogItem?, settings: AdvancedPlaybackSettings,
    onTrim: (String, Int) -> Unit) {
    val id = track?.lyricsVideoId() ?: return
    val trim = settings.trimFor(id)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("This song's volume", style = MaterialTheme.typography.titleMedium)
        Text(if (trim == 0) "Original level" else "$trim dB", style = MaterialTheme.typography.bodyMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton({ onTrim(id, (trim - 1).coerceAtLeast(-12)) },
                enabled = trim > -12, modifier = Modifier.weight(1f)) { Text("Quieter") }
            OutlinedButton({ onTrim(id, (trim + 1).coerceAtMost(0)) },
                enabled = trim < 0, modifier = Modifier.weight(1f)) { Text("Louder") }
        }
        Text("Only reduces this song; the sleep timer fade still applies.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
