package com.karthi.sungmobile.feature.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.model.library.*
import com.karthi.sungmobile.feature.library.ConfirmLibraryChange

@Composable
fun HistoryScreen(policy: HistoryPolicy?, model: HistoryViewModel, back: () -> Unit, modifier: Modifier = Modifier) {
    var clear by remember { mutableStateOf<String?>(null) }
    clear?.let { kind -> ConfirmLibraryChange("Clear $kind history?", "This cannot be undone. Likes and playlists will remain.",
        { clear = null }, { if (kind == "listening") model.clearListening() else model.clearSearches() }) }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(back) { Text("Back") }
        Text("History & privacy", style = MaterialTheme.typography.headlineMedium)
        if (policy == null) LinearProgressIndicator(Modifier.fillMaxWidth())
        else {
            PolicyToggle("Private mode", policy.privateMode) { model.setting(HistorySetting.PRIVATE_MODE, it) }
            Text("Private mode stays on until you turn it off. It pauses listening and search history. Likes and playlist edits still save. The current queue and playback position are still saved for restoration.")
            PolicyToggle("Save listening history", policy.listening) { model.setting(HistorySetting.LISTENING, it) }
            PolicyToggle("Save search history", policy.searches) { model.setting(HistorySetting.SEARCHES, it) }
            Text("A song appears in Recent after 30 seconds of actual listening, or half its duration if shorter. Pauses and seeking do not count.")
            OutlinedButton({ clear = "listening" }) { Text("Clear listening history") }
            OutlinedButton({ clear = "search" }) { Text("Clear search history") }
            Text("Your library is saved on this phone. Saved song details can be browsed offline; playback still needs internet.")
        }
    }
}

@Composable
private fun PolicyToggle(label: String, checked: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, Modifier.weight(1f).padding(top = 12.dp)); Switch(checked, change)
    }
}
