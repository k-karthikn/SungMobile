package com.karthi.sungmobile.feature.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.designsystem.component.SungLoading
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.discovery.HomeShelf
import com.karthi.sungmobile.core.model.discovery.HomeChip
import com.karthi.sungmobile.core.model.discovery.HomeData
import com.karthi.sungmobile.core.model.discovery.RecommendationFeedback

private val moods = listOf("Feel good" to "feel good songs", "Focus" to "instrumental focus",
    "Unwind" to "chill evening", "Energize" to "workout energy")

/** Home is discovery; saved music and local mixes live in Library. */
@Composable
fun DiscoveryHomeScreen(state: DiscoveryFeedState, refresh: () -> Unit, ensureFresh: () -> Unit,
    search: () -> Unit, appearance: () -> Unit, openLibrary: () -> Unit,
    openStatistics: () -> Unit,
    searchCatalog: (String) -> Unit, openItem: (HomeShelf, CatalogItem) -> Unit,
    playbackReady: Boolean, modifier: Modifier = Modifier,
    onRecommendationFeedback: (CatalogItem, RecommendationFeedback?) -> Unit = { _, _ -> },
    onHomeChipSelected: (HomeChip?) -> Unit = {}, artistSignals: HomeData = HomeData(),
    onArtistSelected: (String) -> Unit = {}) {
    LaunchedEffect(Unit) { ensureFresh() }
    val visibleShelves = if (state.selectedHomeChip != null) state.chipShelves else state.shelves
    val artists = remember(artistSignals) { artistSuggestions(artistSignals) }
    LazyColumn(modifier.fillMaxSize().testTag("discovery_home"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)) {
        item("header", "header") {
            HomeHeader(search, appearance, refresh, state.initialLoading || state.refreshing)
        }
        if (state.homeChips.isNotEmpty()) item("provider_chips", "provider_chips") {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = state.selectedHomeChip == null,
                    onClick = { onHomeChipSelected(null) }, label = { Text("All music") })
                state.homeChips.forEach { chip ->
                    FilterChip(selected = state.selectedHomeChip?.key == chip.key,
                        onClick = { onHomeChipSelected(chip) }, label = { Text(chip.title) },
                        modifier = Modifier.testTag("home_chip_${chip.title.lowercase().replace(' ', '_')}"))
                }
            }
        }
        item("statistics", "statistics") { HomeStatisticsEntry(openStatistics) }
        if (state.homeChips.isEmpty()) item("moods", "moods") {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                moods.forEach { (label, query) ->
                    AssistChip(
                        onClick = { searchCatalog(query) },
                        label = { Text(label) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("mood_${label.lowercase().replace(' ', '_')}"),
                    )
                }
            }
        }
        if (artists.isNotEmpty()) item("artist_spotlight", "artist_spotlight") {
            ArtistSpotlight(artists.first(), onArtistSelected)
        }
        if (artists.size > 1) item("artists_for_you", "artists_for_you") {
            ArtistsForYou(artists.drop(1), onArtistSelected)
        }
        item("genres", "genres") {
            GenreDiscoverySection { genre -> searchCatalog("$genre music") }
        }
        if (state.initialLoading || state.chipLoading) item("loading", "loading") {
            SungLoading(label = "Loading music suggestions…")
        }
        if (state.error != null) item("error", "error") {
            HomeFeedNotice(if (visibleShelves.isEmpty()) "Could not load music suggestions."
                else "Could not refresh suggestions.", refresh,
                if (visibleShelves.isEmpty()) openLibrary else null)
        }
        if (!state.initialLoading && !state.chipLoading && state.error == null && visibleShelves.isEmpty()) item("empty", "empty") {
            if (state.selectedHomeChip == null) {
                HomeFeedNotice("No music suggestions are available right now.", refresh, openLibrary)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("No suggestions in ${state.selectedHomeChip.title} right now.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton({ onHomeChipSelected(null) }) { Text("Show all music") }
                }
            }
        }
        if (state.selectedHomeChip == null) state.personalizedShelf?.let { shelf ->
            item("personalized_for_you", "personalized_shelf") {
                HomeFeedShelf(shelf, playbackReady, state.recommendationFeedback,
                    onRecommendationFeedback) { item -> openItem(shelf, item) }
            }
        }
        items(visibleShelves, key = HomeShelf::key, contentType = { "provider_shelf" }) { shelf ->
            HomeFeedShelf(shelf, playbackReady) { item -> openItem(shelf, item) }
        }
    }
}

@Composable
private fun HomeFeedNotice(message: String, retry: () -> Unit, library: (() -> Unit)?) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(retry) { Text("Retry") }
            if (library != null) TextButton(library) { Text("Open Library") }
        }
    }
}
