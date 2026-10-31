package com.karthi.sungmobile.feature.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val discoveryGenres = listOf(
    "Pop", "Hip-hop", "Rock", "R&B", "Electronic", "Indie", "Jazz", "Classical",
    "Metal", "Country", "K-pop", "Lo-fi",
)

@Composable
internal fun GenreDiscoverySection(onGenreSelected: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Explore by genre", style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold)
        Text("Find music across styles and scenes.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            discoveryGenres.forEach { genre ->
                AssistChip(
                    onClick = { onGenreSelected(genre) },
                    label = { Text(genre) },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("genre_${genre.lowercase().replace('-', '_')}")
                )
            }
        }
    }
}
