package com.karthi.sungmobile.feature.library

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.R
import com.karthi.sungmobile.core.designsystem.component.SungLoading
import com.karthi.sungmobile.core.designsystem.icon.*
import com.karthi.sungmobile.feature.home.HomeMixCard
import com.karthi.sungmobile.feature.home.HomeState

/** The Mixes destination contains real local mixes and saved smart lists only. */
@Composable
internal fun LibraryMixesContent(home: HomeState, smart: SmartOverview, connected: Boolean,
    actions: LibraryActionsViewModel, openSmart: (String) -> Unit, openSmartList: () -> Unit,
    openStatistics: () -> Unit) {
    val save = LocalLibraryUi.current.add
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Your mixes", style = MaterialTheme.typography.headlineSmall)
        val liked = home.likedMix.takeIf { it.size >= 2 }
        val recent = home.recentMix.takeIf { !home.data.privateMode && it.size >= 2 }
        if (liked != null || recent != null) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (liked != null) HomeMixCard(stringResource(R.string.liked_shuffle), liked, connected,
                    actions::playTracks, save, Modifier.width(208.dp))
                if (recent != null) HomeMixCard(stringResource(R.string.recent_mix), recent, connected,
                    actions::playTracks, save, Modifier.width(208.dp))
            }
        } else if (home.loading) {
            SungLoading(label = "Loading mixes…")
        } else Text("Like or listen to more songs to fill your mixes.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        HorizontalDivider()
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Smart playlists", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            TextButton(openSmartList) { Text("Manage") }
        }
        if (smart.loading) SungLoading(label = "Loading smart playlists…")
        if (smart.error) Text("Could not load smart playlists. Open Manage to retry.",
            color = MaterialTheme.colorScheme.error)
        if (!smart.loading && !smart.error && smart.playlists.isEmpty()) Text("No smart playlists yet.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        smart.playlists.take(6).forEach { playlist ->
            Surface(onClick = { openSmart(playlist.id) }, shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SungIcon(SungSymbol.Shuffle, modifier = Modifier.size(24.dp))
                    Text(playlist.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    SungIcon(SungSymbol.Chevron)
                }
            }
        }
        TextButton(openStatistics) { Text("Listening statistics") }
    }
}
