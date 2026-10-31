package com.karthi.sungmobile.feature.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.designsystem.artwork.ArtworkMask
import com.karthi.sungmobile.core.designsystem.artwork.MaterialArtworkFade
import com.karthi.sungmobile.core.designsystem.artwork.SungArtwork
import com.karthi.sungmobile.core.designsystem.icon.SungIcon
import com.karthi.sungmobile.core.designsystem.icon.SungSymbol
import com.karthi.sungmobile.core.model.CatalogKind

@Composable
internal fun CollectionHeader(state: CollectionState, hasPlayback: Boolean, onBack: () -> Unit,
    onRefresh: () -> Unit, onPlayer: () -> Unit) {
    val kind = state.displayKind()
    val onSurface = MaterialTheme.colorScheme.onSurface
    Box(Modifier.fillMaxWidth().height(480.dp)) {
        SungArtwork(state.coverArtworkUrl(), Modifier.matchParentSize(),
            description = "${state.page?.title ?: state.route?.title.orEmpty()} artwork",
            mask = ArtworkMask.SQUARE)
        MaterialArtworkFade(Modifier.matchParentSize())
        CollectionHeaderTopFade(Modifier.matchParentSize())
        Row(Modifier.align(Alignment.TopCenter).fillMaxWidth().statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(onBack) { SungIcon(SungSymbol.Back, "Back") }
            Spacer(Modifier.weight(1f))
            FilledTonalIconButton(onRefresh, enabled = !state.loading) { SungIcon(SungSymbol.Refresh, "Refresh collection") }
            FilledTonalIconButton(onPlayer, enabled = hasPlayback) {
                SungIcon(SungSymbol.Equalizer, "Now playing")
            }
        }
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(kind.label.uppercase(), style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold, color = onSurface.copy(alpha = .86f))
            Text(state.page?.title ?: state.route?.title.orEmpty(),
                style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold,
                color = onSurface, maxLines = 3, overflow = TextOverflow.Ellipsis)
            state.page?.details?.creator?.takeIf(String::isNotBlank)?.let { creator ->
                Text(creator, style = MaterialTheme.typography.bodyLarge,
                    color = onSurface.copy(alpha = .84f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            val count = state.entries.size
            if (count > 0) {
                val total = state.page?.details?.totalCount
                val unit = if (kind == CatalogKind.ARTIST) "videos" else "songs"
                Text(if (total != null && total > count) "$count of $total $unit" else "$count $unit",
                    style = MaterialTheme.typography.labelLarge, color = onSurface.copy(alpha = .78f))
            }
        }
    }
}

@Composable
private fun CollectionHeaderTopFade(modifier: Modifier = Modifier) {
    val surface = MaterialTheme.colorScheme.surface
    Box(modifier.background(Brush.verticalGradient(
        0f to surface.copy(alpha = .78f),
        .14f to surface.copy(alpha = .52f),
        .30f to surface.copy(alpha = .16f),
        .44f to surface.copy(alpha = .02f),
        .52f to surface.copy(alpha = 0f),
    )))
}

private val CatalogKind.label: String get() = when (this) {
    CatalogKind.SONG -> "Song"
    CatalogKind.ALBUM -> "Album"
    CatalogKind.ARTIST -> "Artist"
    CatalogKind.PLAYLIST -> "Playlist"
}
