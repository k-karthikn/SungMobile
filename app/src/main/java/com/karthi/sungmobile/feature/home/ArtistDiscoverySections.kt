package com.karthi.sungmobile.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.designsystem.artwork.SungArtwork
import com.karthi.sungmobile.core.designsystem.icon.SungIcon
import com.karthi.sungmobile.core.designsystem.icon.SungSymbol
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.CatalogKind
import com.karthi.sungmobile.core.model.discovery.HomeData

internal data class ArtistSuggestion(val name: String, val artworkUrl: String?, val sampleTitle: String, val score: Double)

internal fun artistSuggestions(data: HomeData): List<ArtistSuggestion> {
    if (data.privateMode) return emptyList()
    val scores = linkedMapOf<String, ArtistSuggestion>()
    data.recent.forEachIndexed { index, item ->
        addArtistSignal(scores, item, (12.0 / (index + 1)).coerceAtLeast(1.0))
    }
    data.liked.forEach { item -> addArtistSignal(scores, item, 2.5) }
    return scores.values.sortedByDescending(ArtistSuggestion::score).take(6)
}

private fun addArtistSignal(scores: MutableMap<String, ArtistSuggestion>, item: CatalogItem, weight: Double) {
    if (item.kind != CatalogKind.SONG) return
    val rawArtist = item.subtitle.substringBefore(" • ").removeSuffix(" - Topic").trim()
    if (rawArtist.isBlank() || rawArtist.equals("Unknown artist", ignoreCase = true)) return
    rawArtist.split(ARTIST_SEPARATOR).map(String::trim).filter(String::isNotBlank).forEach { artist ->
        val key = artist.lowercase()
        val existing = scores[key]
        scores[key] = ArtistSuggestion(artist, item.artworkUrl ?: existing?.artworkUrl,
            existing?.sampleTitle ?: item.title, (existing?.score ?: 0.0) + weight)
    }
}

private val ARTIST_SEPARATOR = Regex("\\s*(?:,|&|\\bfeat\\.?\\b|\\bft\\.?\\b)\\s*", RegexOption.IGNORE_CASE)

@Composable
internal fun ArtistSpotlight(suggestion: ArtistSuggestion, onOpen: (String) -> Unit) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            Modifier.fillMaxWidth().clickable(onClickLabel = "Explore ${suggestion.name}") {
                onOpen(suggestion.name)
            }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SungArtwork(suggestion.artworkUrl, Modifier.size(78.dp).clip(RoundedCornerShape(20.dp)), description = null)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("ARTIST SPOTLIGHT", style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(suggestion.name, style = MaterialTheme.typography.titleLarge,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("From your listening · ${suggestion.sampleTitle}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = .75f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            SungIcon(SungSymbol.Chevron, "Explore ${suggestion.name}")
        }
    }
}

@Composable
internal fun ArtistsForYou(suggestions: List<ArtistSuggestion>, onOpen: (String) -> Unit) {
    if (suggestions.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Artists for you", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            suggestions.forEach { suggestion ->
                Column(
                    Modifier.width(92.dp).clickable(onClickLabel = "Search ${suggestion.name}") {
                        onOpen(suggestion.name)
                    }.testTag("artist_for_you_${suggestion.name.lowercase().replace(' ', '_')}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    SungArtwork(suggestion.artworkUrl, Modifier.size(74.dp).clip(CircleShape), description = null)
                    Text(suggestion.name, style = MaterialTheme.typography.labelLarge,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}
