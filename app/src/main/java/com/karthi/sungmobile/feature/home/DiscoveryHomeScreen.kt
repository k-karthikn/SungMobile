package com.karthi.sungmobile.feature.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.designsystem.component.SungLoading
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.discovery.HomeShelf
import com.karthi.sungmobile.core.model.discovery.HomeChip
import com.karthi.sungmobile.core.model.discovery.HomeData
import com.karthi.sungmobile.core.model.discovery.RecommendationFeedback
import com.karthi.sungmobile.core.model.library.libraryKeyOrNull
import com.karthi.sungmobile.core.model.library.StreakProgress

private val moods = listOf("Feel good" to "feel good songs", "Focus" to "instrumental focus",
    "Unwind" to "chill evening", "Energize" to "workout energy")

/** Home is discovery; saved music and local mixes live in Library. */
@Composable
fun DiscoveryHomeScreen(state: DiscoveryFeedState, refresh: () -> Unit, ensureFresh: () -> Unit,
    search: () -> Unit, appearance: () -> Unit, openLibrary: () -> Unit,
    searchCatalog: (String) -> Unit, openItem: (HomeShelf, CatalogItem) -> Unit,
    playbackReady: Boolean, modifier: Modifier = Modifier,
    onRecommendationFeedback: (CatalogItem, RecommendationFeedback?) -> Unit = { _, _ -> },
    onHomeChipSelected: (HomeChip?) -> Unit = {}, onRetryHomeChip: () -> Unit = {},
    artistSignals: HomeData = HomeData(),
    onArtistSelected: (String) -> Unit = {}, onPlayRadio: (List<CatalogItem>) -> Unit = {},
    personalizationEnabled: Boolean = true,
    openStatistics: () -> Unit = {}, streak: StreakProgress = StreakProgress(),
    streakColor: Color = Color.Unspecified, openStreak: () -> Unit = {}) {
    LaunchedEffect(Unit) { ensureFresh() }
    val selectedChipKey = state.selectedHomeChip?.key

    PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = refresh,
        modifier = modifier.fillMaxSize().testTag("home_pull_refresh")) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wideLayout = maxWidth >= 720.dp
        val listState = rememberLazyListState()
        var previousChipKey by remember { mutableStateOf(selectedChipKey) }
        LaunchedEffect(selectedChipKey) {
            if (selectedChipKey != previousChipKey) {
                listState.animateScrollToItem(0)
                previousChipKey = selectedChipKey
            }
        }

        val selectedChip = state.selectedHomeChip
        val canPersonalize = personalizationEnabled && !artistSignals.privateMode
        val artists = remember(artistSignals, canPersonalize) {
            if (canPersonalize) artistSuggestions(artistSignals) else emptyList()
        }
        val radioItems = remember(state.personalizedShelf, state.shelves, artistSignals, personalizationEnabled) {
            val personal = if (canPersonalize) state.personalizedShelf?.items.orEmpty() else emptyList()
            val localSignals = if (canPersonalize) artistSignals.liked + artistSignals.recent else emptyList()
            (personal + localSignals + state.shelves.flatMap { it.items })
                .filter { it.kind == com.karthi.sungmobile.core.model.CatalogKind.SONG && it.libraryKeyOrNull() != null }
                .distinctBy { it.id }.take(20)
        }
        val hasPersonalSongs = canPersonalize && state.personalizedShelf?.items?.isNotEmpty() == true

        Column(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                HomeHeader(search, appearance, openStatistics, streak, streakColor, openStreak)
                if (state.homeChips.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().testTag("pinned_home_chips"),
                        contentPadding = PaddingValues(vertical = 1.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        item(key = "all_music") {
                            FilterChip(selected = selectedChip == null,
                                onClick = { onHomeChipSelected(null) },
                                label = { Text("All music", style = MaterialTheme.typography.labelMediumEmphasized) },
                                shape = CircleShape,
                                modifier = Modifier.heightIn(min = 36.dp))
                        }
                        items(state.homeChips, key = HomeChip::key) { chip ->
                            FilterChip(selected = selectedChip?.key == chip.key,
                                onClick = { onHomeChipSelected(chip) },
                                label = { Text(chip.title, style = MaterialTheme.typography.labelMediumEmphasized,
                                    maxLines = 1) },
                                shape = CircleShape,
                                modifier = Modifier.heightIn(min = 36.dp)
                                    .testTag("home_chip_${chip.title.lowercase().replace(' ', '_')}"))
                        }
                    }
                }
            }

            LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth()
                .testTag("discovery_home"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(if (wideLayout) 20.dp else 22.dp)) {

            if (selectedChip != null) {
                when {
                    state.chipLoading -> item("chip_loading", "chip_loading") {
                        SungLoading(label = "Loading ${selectedChip.title}…")
                    }
                    state.chipError -> item("chip_error", "chip_error") {
                        HomeFeedNotice("Couldn't load ${selectedChip.title}. Check your connection and retry.",
                            onRetryHomeChip, null)
                    }
                    state.chipShelves.isEmpty() -> item("chip_empty", "chip_empty") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("No suggestions in ${selectedChip.title} right now.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton({ onHomeChipSelected(null) }) { Text("Show all music") }
                        }
                    }
                    else -> items(state.chipShelves, key = HomeShelf::key,
                        contentType = { "provider_shelf" }) { shelf ->
                        HomeFeedShelf(shelf, playbackReady) { item -> openItem(shelf, item) }
                    }
                }
            } else {
                if (state.initialLoading && state.shelves.isEmpty()) item("loading", "loading") {
                    SungLoading(label = "Loading music suggestions…")
                }
                if (state.error != null && state.shelves.isEmpty()) item("error", "error") {
                    HomeFeedNotice("Could not load music suggestions.", refresh, openLibrary)
                }
                if (radioItems.isNotEmpty()) item("infinite_radio", "infinite_radio") {
                    InfiniteRadioHero(
                        badge = if (hasPersonalSongs) "Made for you" else "Your music, on repeat",
                        description = if (hasPersonalSongs) "An endless station shaped by your listening"
                            else "Start with familiar picks and keep discovering songs",
                        enabled = playbackReady,
                        onPlay = { onPlayRadio(radioItems) },
                    )
                }
                if (canPersonalize) state.personalizedShelf?.let { shelf ->
                    item("personalized_for_you", "personalized_shelf") {
                        HomeFeedShelf(shelf, playbackReady, state.recommendationFeedback,
                            onRecommendationFeedback) { item -> openItem(shelf, item) }
                    }
                }
                if (state.error != null && state.shelves.isNotEmpty()) item("refresh_error", "refresh_error") {
                    HomeFeedNotice("Could not refresh suggestions.", refresh, null)
                }
                items(state.shelves, key = HomeShelf::key, contentType = { "provider_shelf" }) { shelf ->
                    HomeFeedShelf(shelf, playbackReady) { item -> openItem(shelf, item) }
                }
                if (!state.initialLoading && state.error == null && state.shelves.isEmpty() &&
                    radioItems.isEmpty() && !hasPersonalSongs) item("empty", "empty") {
                    HomeFeedNotice("No music suggestions are available right now.", refresh, openLibrary)
                }

                if (wideLayout && artists.size > 1) item("artist_discovery", "artist_discovery") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(Modifier.weight(1f)) { ArtistSpotlight(artists.first(), onArtistSelected) }
                        Box(Modifier.weight(1f)) { ArtistsForYou(artists.drop(1), onArtistSelected) }
                    }
                } else {
                    if (artists.isNotEmpty()) item("artist_spotlight", "artist_spotlight") {
                        ArtistSpotlight(artists.first(), onArtistSelected)
                    }
                    if (artists.size > 1) item("artists_for_you", "artists_for_you") {
                        ArtistsForYou(artists.drop(1), onArtistSelected)
                    }
                }

                if (wideLayout && state.homeChips.isEmpty()) item("mood_genres", "mood_genres") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(Modifier.weight(1f)) { MoodDiscoverySection(searchCatalog) }
                        Box(Modifier.weight(1f)) {
                            GenreDiscoverySection { genre -> searchCatalog("$genre music") }
                        }
                    }
                } else {
                    if (state.homeChips.isEmpty()) item("moods", "moods") {
                        MoodDiscoverySection(searchCatalog)
                    }
                    item("genres", "genres") {
                        GenreDiscoverySection { genre -> searchCatalog("$genre music") }
                    }
                }
            }
        }
        }
    }
    }
}

@Composable
private fun MoodDiscoverySection(searchCatalog: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Browse by mood", style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            moods.forEach { (label, query) ->
                AssistChip(onClick = { searchCatalog(query) }, label = { Text(label) },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("mood_${label.lowercase().replace(' ', '_')}"))
            }
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
