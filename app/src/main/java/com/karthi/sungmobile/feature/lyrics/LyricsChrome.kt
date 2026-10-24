package com.karthi.sungmobile.feature.lyrics

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.R
import com.karthi.sungmobile.core.designsystem.artwork.SungArtwork
import com.karthi.sungmobile.core.designsystem.icon.SungIcon
import com.karthi.sungmobile.core.designsystem.icon.SungSymbol
import com.karthi.sungmobile.core.model.CatalogItem

@Composable
internal fun LyricsTopBar(synchronized: Boolean?, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IconButton(onBack, Modifier.size(48.dp)) {
            SungIcon(SungSymbol.Back, stringResource(R.string.back))
        }
        Text(stringResource(R.string.lyrics), Modifier.weight(1f),
            style = MaterialTheme.typography.headlineSmall)
        if (synchronized != null) {
            Surface(shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.secondaryContainer) {
                Text(if (synchronized) "Synced" else "Plain text",
                    Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
    }
}

/** Real track art and provider credit stay visible above the scrolling lyric text. */
@Composable
internal fun LyricsTrackCard(track: CatalogItem?, attribution: String?) {
    Surface(shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            SungArtwork(track?.artworkUrl, Modifier.size(64.dp), description = null)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(track?.title.orEmpty(), style = MaterialTheme.typography.titleLarge,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(lyricsSourceLabel(attribution, track?.subtitle.orEmpty()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

internal fun lyricsSourceLabel(attribution: String?, fallback: String): String {
    val source = attribution?.replace(Regex("^(?:source\\s*:\\s*)+", RegexOption.IGNORE_CASE), "")?.trim()
    return source?.takeIf { it.isNotEmpty() }?.let { "Source: $it" } ?: fallback
}

@Composable
internal fun LyricsTimingCard(offsetMs: Int, follow: Boolean, onOffset: (Int) -> Unit,
    onFollow: () -> Unit) {
    Surface(shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Timing offset", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${offsetMs / 1_000f} s", style = MaterialTheme.typography.titleMedium)
                }
                TimingStepButton("−", "Decrease timing offset by 0.25 seconds") {
                    onOffset(offsetMs - 250)
                }
                TimingStepButton("+", "Increase timing offset by 0.25 seconds") {
                    onOffset(offsetMs + 250)
                }
            }
            if (!follow) {
                FilledTonalButton(onFollow, Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = MaterialTheme.shapes.large) { Text("Return to current line") }
            }
        }
    }
}

@Composable
private fun TimingStepButton(symbol: String, label: String, onClick: () -> Unit) {
    FilledTonalButton(onClick, Modifier.size(48.dp).semantics { contentDescription = label },
        shape = MaterialTheme.shapes.medium, contentPadding = PaddingValues(0.dp)) {
        Text(symbol, style = MaterialTheme.typography.headlineSmall)
    }
}
