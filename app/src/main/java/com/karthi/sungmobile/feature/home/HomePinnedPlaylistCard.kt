package com.karthi.sungmobile.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.R
import com.karthi.sungmobile.core.designsystem.icon.SungIcon
import com.karthi.sungmobile.core.designsystem.icon.SungSymbol
import com.karthi.sungmobile.core.model.library.LocalPlaylist

/** A pinned playlist has no stored cover; show its real name/count with a clear collection icon. */
@Composable
internal fun HomePinnedPlaylistCard(playlist: LocalPlaylist, open: () -> Unit, modifier: Modifier = Modifier) {
    Surface(onClick = open, modifier = modifier.width(176.dp).testTag("home_pinned_${playlist.id}"),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(Modifier.fillMaxWidth().aspectRatio(1.25f), shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer) {
                Box(contentAlignment = Alignment.Center) {
                    SungIcon(SungSymbol.Library, null, Modifier.size(48.dp))
                }
            }
            Column(Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
                Text(playlist.name, style = MaterialTheme.typography.titleSmall, maxLines = 2,
                    overflow = TextOverflow.Ellipsis)
                Text(pluralStringResource(R.plurals.home_song_count, playlist.songCount, playlist.songCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
