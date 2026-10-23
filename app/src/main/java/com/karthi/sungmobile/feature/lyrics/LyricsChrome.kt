package com.karthi.sungmobile.feature.lyrics

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.designsystem.artwork.SungArtwork
import com.karthi.sungmobile.core.model.CatalogItem

/** Real track art and provider credit stay visible above the scrolling lyric text. */
@Composable
internal fun LyricsTrackCard(track: CatalogItem?, attribution: String?) {
    Surface(shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            SungArtwork(track?.artworkUrl, Modifier.size(64.dp), description = null)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(track?.title.orEmpty(), style = MaterialTheme.typography.titleMedium,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(attribution?.let { "Source: $it" } ?: track?.subtitle.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
internal fun LyricsTimingCard(offsetMs: Int, follow: Boolean, onOffset: (Int) -> Unit,
    onFollow: () -> Unit) {
    Surface(shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Timing offset", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton({ onOffset(offsetMs - 250) }, Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp)) { Text("− 0.25 s") }
                Text("${offsetMs / 1_000f} s", style = MaterialTheme.typography.titleMedium)
                FilledTonalButton({ onOffset(offsetMs + 250) }, Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp)) { Text("+ 0.25 s") }
            }
            if (!follow) {
                HorizontalDivider()
                FilledTonalButton(onFollow, Modifier.fillMaxWidth()) { Text("Return to current line") }
            }
        }
    }
}
