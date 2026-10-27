package com.karthi.sungmobile.feature.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.designsystem.artwork.ArtworkMask
import com.karthi.sungmobile.core.designsystem.artwork.SungArtwork
import com.karthi.sungmobile.core.designsystem.icon.SungIcon
import com.karthi.sungmobile.core.designsystem.icon.SungSymbol
import com.karthi.sungmobile.core.model.CatalogKind

@Composable
internal fun CollectionHeader(state: CollectionState, hasPlayback: Boolean, onBack: () -> Unit,
    onRefresh: () -> Unit, onPlayer: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack) { SungIcon(SungSymbol.Back, "Back") }
            Spacer(Modifier.weight(1f))
            IconButton(onRefresh, enabled = !state.loading) { SungIcon(SungSymbol.Refresh, "Refresh collection") }
            FilledTonalIconButton(onPlayer, enabled = hasPlayback) {
                SungIcon(SungSymbol.Equalizer, "Now playing")
            }
        }
        CollectionHero(state)
    }
}

@Composable
private fun CollectionHero(state: CollectionState) {
    val kind = state.displayKind()
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainer) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
            SungArtwork(state.coverArtworkUrl(), Modifier.matchParentSize(),
                description = "${state.page?.title ?: state.route?.title.orEmpty()} artwork",
                mask = ArtworkMask.SQUARE)
            Surface(Modifier.align(Alignment.TopStart).padding(16.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color.Black.copy(alpha = .58f), contentColor = Color.White) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SungIcon(kind.icon, null, Modifier.size(18.dp))
                    Text(kind.label.uppercase(), style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold)
                }
            }
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 64.dp, topEnd = 20.dp))
                .background(Brush.verticalGradient(listOf(
                    Color(0xCC15151A), Color(0xF515151A))))
                .padding(start = 22.dp, end = 20.dp, top = 26.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(state.page?.title ?: state.route?.title.orEmpty(),
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                    color = Color.White, maxLines = 3, overflow = TextOverflow.Ellipsis)
                state.page?.details?.creator?.takeIf(String::isNotBlank)?.let { creator ->
                    Text(creator, style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = .82f),
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                val count = state.entries.size
                if (count > 0) {
                    val total = state.page?.details?.totalCount
                    val unit = if (kind == CatalogKind.ARTIST) "videos" else "songs"
                    Text(if (total != null && total > count) "$count of $total $unit" else "$count $unit",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = .78f))
                }
            }
        }
    }
}

private val CatalogKind.label: String get() = when (this) {
    CatalogKind.SONG -> "Song"
    CatalogKind.ALBUM -> "Album"
    CatalogKind.ARTIST -> "Artist"
    CatalogKind.PLAYLIST -> "Playlist"
}

private val CatalogKind.icon: SungSymbol get() = when (this) {
    CatalogKind.ARTIST -> SungSymbol.Artist
    CatalogKind.PLAYLIST -> SungSymbol.QueueMusic
    else -> SungSymbol.Disc
}
