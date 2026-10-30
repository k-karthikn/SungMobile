package com.karthi.sungmobile.feature.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.designsystem.artwork.ArtworkMask
import com.karthi.sungmobile.core.designsystem.artwork.SungArtwork
import com.karthi.sungmobile.core.designsystem.theme.LocalSungMotionEnabled
import com.karthi.sungmobile.core.designsystem.icon.SungIcon
import com.karthi.sungmobile.core.designsystem.icon.SungSymbol
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.CatalogKind
import com.karthi.sungmobile.core.model.discovery.HomeShelf
import com.karthi.sungmobile.core.model.discovery.RecommendationFeedback
import com.karthi.sungmobile.core.model.library.libraryKeyOrNull

@Composable
internal fun HomeFeedShelf(shelf: HomeShelf, playbackReady: Boolean,
    feedback: Map<String, RecommendationFeedback> = emptyMap(),
    onFeedback: ((CatalogItem, RecommendationFeedback?) -> Unit)? = null,
    open: (CatalogItem) -> Unit) {
    val motion = LocalSungMotionEnabled.current
    val largeFont = LocalDensity.current.fontScale > 1.3f
    val cardWidth = if (largeFont) 216.dp else 204.dp
    if (shelf.items.isEmpty()) return
    val state = rememberCarouselState { shelf.items.size }
    val rowState = rememberLazyListState()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(shelf.title, style = MaterialTheme.typography.titleLarge,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (motion && shelf.items.size > 1) {
            HorizontalMultiBrowseCarousel(state = state, preferredItemWidth = cardWidth,
                modifier = Modifier.fillMaxWidth().height(cardWidth), itemSpacing = 10.dp) { index ->
                val item = shelf.items[index]
                HomeFeedArtwork(item, playbackReady, { open(item) },
                    Modifier.fillMaxSize().maskClip(RoundedCornerShape(24.dp))
                        .testTag("home_card_${shelf.key}_$index"))
            }
            HomeFeedCarouselCaption(shelf.items, state, feedback, onFeedback)
        } else {
            LazyRow(state = rowState, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(shelf.items, key = { _, item -> "${item.kind}:${item.id}" }) { index, item ->
                    HomeFeedArtwork(item, playbackReady, { open(item) },
                        Modifier.size(cardWidth).testTag("home_card_${shelf.key}_$index"))
                }
            }
            val selected = shelf.items.getOrNull(rowState.firstVisibleItemIndex) ?: shelf.items.first()
            HomeFeedCaption(selected, selected.libraryKeyOrNull()?.let(feedback::get), onFeedback)
        }
    }
}

@Composable
private fun HomeFeedCarouselCaption(items: List<CatalogItem>, state: androidx.compose.material3.carousel.CarouselState,
    feedback: Map<String, RecommendationFeedback>, onFeedback: ((CatalogItem, RecommendationFeedback?) -> Unit)?) {
    var selectedIndex by remember(state) { mutableIntStateOf(0) }
    LaunchedEffect(state) {
        snapshotFlow { state.currentItem }.collect { selectedIndex = it }
    }
    val selected = items.getOrNull(selectedIndex) ?: items.firstOrNull() ?: return
    Crossfade(selected, animationSpec = tween(160), label = "Home caption") {
        HomeFeedCaption(it, it.libraryKeyOrNull()?.let(feedback::get), onFeedback)
    }
}

@Composable
private fun HomeFeedArtwork(item: CatalogItem, playbackReady: Boolean, open: () -> Unit,
    modifier: Modifier) {
    val enabled = item.kind != CatalogKind.SONG || playbackReady
    val label = when (item.kind) {
        CatalogKind.SONG -> "Play ${item.title}${item.subtitle.takeIf(String::isNotBlank)?.let { " by $it" }.orEmpty()}"
        CatalogKind.ALBUM -> "Open album ${item.title}"
        CatalogKind.PLAYLIST -> "Open playlist ${item.title}"
        CatalogKind.ARTIST -> "Open artist ${item.title}"
    }
    Box(modifier.clickable(enabled = enabled, onClickLabel = label, onClick = open)) {
        SungArtwork(item.artworkUrl, Modifier.fillMaxSize(), mask = ArtworkMask.TILE)
        if (item.kind == CatalogKind.ALBUM || item.kind == CatalogKind.PLAYLIST) {
            Surface(Modifier.align(Alignment.BottomStart).padding(10.dp),
                shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = .92f)) {
                Text(if (item.kind == CatalogKind.ALBUM) "Album" else "Playlist",
                    Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun HomeFeedCaption(item: CatalogItem, feedback: RecommendationFeedback?,
    onFeedback: ((CatalogItem, RecommendationFeedback?) -> Unit)?) {
    Row(Modifier.fillMaxWidth().heightIn(min = 57.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 2,
                overflow = TextOverflow.Ellipsis)
            val subtitle = item.subtitle.ifBlank { when (item.kind) {
                CatalogKind.SONG -> "Song"
                CatalogKind.ALBUM -> "Album"
                CatalogKind.PLAYLIST -> "Playlist"
                CatalogKind.ARTIST -> "Artist"
            } }
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
        }
        if (onFeedback != null) {
            var menuOpen by remember(item.id) { mutableStateOf(false) }
            Box {
                IconButton({ menuOpen = true }) { SungIcon(SungSymbol.More, "Recommendation options for ${item.title}") }
                DropdownMenu(menuOpen, { menuOpen = false }) {
                    DropdownMenuItem(text = { Text(if (feedback == RecommendationFeedback.MORE)
                        "Remove more like this" else "More from this song") }, onClick = {
                        menuOpen = false
                        onFeedback(item, if (feedback == RecommendationFeedback.MORE) null else RecommendationFeedback.MORE)
                    })
                    DropdownMenuItem(text = { Text("Hide from For you") }, onClick = {
                        menuOpen = false
                        onFeedback(item, RecommendationFeedback.HIDE)
                    })
                }
            }
        }
    }
}
