package com.karthi.sungmobile.feature.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.designsystem.component.SungSettingsGroup
import com.karthi.sungmobile.core.designsystem.component.SungSettingsToggle
import com.karthi.sungmobile.core.designsystem.icon.SungIcon
import com.karthi.sungmobile.core.designsystem.icon.SungSymbol
import com.karthi.sungmobile.core.model.library.*
import com.karthi.sungmobile.core.designsystem.component.SungLoading
import com.karthi.sungmobile.feature.library.ConfirmLibraryChange

@Composable
fun HistoryScreen(policy: HistoryPolicy?, model: HistoryViewModel, back: () -> Unit, modifier: Modifier = Modifier) {
    var clear by remember { mutableStateOf<String?>(null) }
    clear?.let { kind -> ConfirmLibraryChange("Clear $kind history?", "This cannot be undone. Likes and playlists will remain.",
        { clear = null }, { if (kind == "listening") model.clearListening() else model.clearSearches() }) }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(back, Modifier.size(48.dp)) { SungIcon(SungSymbol.Back, "Back to settings") }
            Text("History & privacy", style = MaterialTheme.typography.headlineSmall)
        }
        if (policy == null) SungLoading(label = "Loading history settings…")
        else {
            SungSettingsGroup("Privacy") {
                SungSettingsToggle("Private mode", policy.privateMode,
                    { model.setting(HistorySetting.PRIVATE_MODE, it) },
                    "Pauses listening and search history until you turn it off. Likes, playlists and queue restoration still work.")
                HorizontalDivider()
                SungSettingsToggle("Save listening history", policy.listening,
                    { model.setting(HistorySetting.LISTENING, it) },
                    "A song appears in Recents after 30 seconds of actual listening, or half its duration if shorter. Pauses and seeking do not count.")
                HorizontalDivider()
                SungSettingsToggle("Save search history", policy.searches,
                    { model.setting(HistorySetting.SEARCHES, it) })
            }
            SungSettingsGroup("Clear history") {
                OutlinedButton({ clear = "listening" }, Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = MaterialTheme.shapes.large) { Text("Clear listening history") }
                OutlinedButton({ clear = "search" }, Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = MaterialTheme.shapes.large) { Text("Clear search history") }
                Text("Your library is saved on this phone. Saved song details can be browsed offline; playback still needs internet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
