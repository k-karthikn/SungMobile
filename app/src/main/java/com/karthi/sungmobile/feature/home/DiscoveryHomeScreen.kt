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
import com.karthi.sungmobile.core.model.discovery.RecommendationFeedback

private val moods = listOf("Feel good" to "feel good songs", "Focus" to "instrumental focus",
    "Unwind" to "chill evening", "Energize" to "workout energy")

/** Home is discovery; saved music and local mixes live in Library. */
@Composable
fun DiscoveryHomeScreen(state: DiscoveryFeedState, refresh: () -> Unit, ensureFresh: () -> Unit,
    search: () -> Unit, appearance: () -> Unit, openLibrary: () -> Unit,
    openStatistics: () -> Unit,
    moodSearch: (String) -> Unit, openItem: (HomeShelf, CatalogItem) -> Unit,
    playbackReady: Boolean, modifier: Modifier = Modifier,
    onRecommendationFeedback: (CatalogItem, RecommendationFeedback?) -> Unit = { _, _ -> }) {
    LaunchedEffect(Unit) { ensureFresh() }
    LazyColumn(modifier.fillMaxSize().testTag("discovery_home"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)) {
        item("header", "header") {
            HomeHeader(search, appearance, refresh, state.initialLoading || state.refreshing)
        }
        item("statistics", "statistics") { HomeStatisticsEntry(openStatistics) }
        item("moods", "moods") {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                moods.forEach { (label, query) ->
                    AssistChip(
                        onClick = { moodSearch(query) },
                        label = { Text(label) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("mood_${label.lowercase().replace(' ', '_')}"),
                    )
                }
            }
        }
        if (state.initialLoading) item("loading", "loading") {
            SungLoading(label = "Loading music suggestions…")
        }
        if (state.error != null) item("error", "error") {
            HomeFeedNotice(if (state.shelves.isEmpty()) "Could not load music suggestions."
                else "Could not refresh suggestions.", refresh,
                if (state.shelves.isEmpty()) openLibrary else null)
        }
        if (!state.initialLoading && state.error == null && state.shelves.isEmpty()) item("empty", "empty") {
            HomeFeedNotice("No music suggestions are available right now.", refresh, openLibrary)
        }
        state.personalizedShelf?.let { shelf ->
            item("personalized_for_you", "personalized_shelf") {
                HomeFeedShelf(shelf, playbackReady, state.recommendationFeedback,
                    onRecommendationFeedback) { item -> openItem(shelf, item) }
            }
        }
        items(state.shelves, key = HomeShelf::key, contentType = { "provider_shelf" }) { shelf ->
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
