package com.karthi.sungmobile.feature.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.model.library.*

internal fun androidx.compose.foundation.lazy.LazyListScope.statistics(model: AdvancedLibraryViewModel, state: AdvancedLibraryState) {
    item {
        Text("Listening statistics", style = MaterialTheme.typography.headlineMedium)
        if (state.stats.privateMode) {
            Text("Statistics are hidden while Private mode is on. Turn it off in History & privacy to see saved listening records.")
        } else {
            Text("${state.stats.qualifiedPlays} qualified plays")
            Text("${duration(state.stats.listenedMs)} recorded listening time")
            Text("Only qualified listening counts. Clearing listening history resets these statistics.")
        }
    }
    if (!state.stats.privateMode) {
        item { Text("Top songs", style = MaterialTheme.typography.titleLarge) }
        items(state.stats.topSongs, key = { it.track.id }) { song ->
            Text("${song.plays} plays · ${song.track.title} · ${duration(song.listenedMs)}")
        }
        item {
            HorizontalDivider()
            Text("Recent listening periods", style = MaterialTheme.typography.titleLarge)
            Text("Derived from qualified passes; a gap over 30 minutes starts a new period.")
        }
        items(listeningPeriods(state.stats.recentEvents)) { period ->
            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text("${timestamp(period.startedAt)} · ${period.events.size} plays", style = MaterialTheme.typography.titleSmall)
                period.events.forEach { Text(it.track.title, style = MaterialTheme.typography.bodyMedium) }
            }
        }
        if (state.statsMore) item { OutlinedButton(model::moreEvents, enabled = !state.loadingMore) { Text("Load more periods") } }
    }
}


private fun duration(ms: Long): String { val minutes = ms / 60_000; return "${minutes / 60}h ${minutes % 60}m" }
