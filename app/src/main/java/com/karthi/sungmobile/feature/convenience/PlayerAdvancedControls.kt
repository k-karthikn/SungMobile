package com.karthi.sungmobile.feature.convenience

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.karthi.sungmobile.core.data.settings.AdvancedPlaybackSettings
import com.karthi.sungmobile.core.model.CatalogItem

@Composable
fun PlayerAdvancedControls(
    track: CatalogItem?,
    settings: AdvancedPlaybackSettings,
    privateMode: Boolean,
    onSmartShuffle: (Boolean) -> Unit,
    onRelatedAutoplay: (Boolean) -> Unit,
    onTrim: (String, Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Next songs", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Smart shuffle")
                Text("When Shuffle is on, spread repeated songs and artists apart when possible.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(settings.smartShuffle, onSmartShuffle, Modifier.semantics { contentDescription = "Smart shuffle" })
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Related autoplay")
                Text(if (privateMode) "Paused in Private mode." else
                    "When the queue ends, try a few related YouTube songs.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(settings.relatedAutoplay, onRelatedAutoplay, Modifier.semantics { contentDescription = "Related autoplay" })
        }
        TrackVolumeControl(track, settings, onTrim)
    }
}
