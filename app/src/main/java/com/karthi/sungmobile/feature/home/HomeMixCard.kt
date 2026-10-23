package com.karthi.sungmobile.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.R
import com.karthi.sungmobile.core.designsystem.artwork.SungArtwork
import com.karthi.sungmobile.core.designsystem.artwork.ArtworkMask
import com.karthi.sungmobile.core.designsystem.icon.*
import com.karthi.sungmobile.core.model.CatalogItem

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun HomeMixCard(title: String, songs: List<CatalogItem>, connected: Boolean,
    play: (List<CatalogItem>) -> Unit, save: (List<CatalogItem>) -> Unit, modifier: Modifier = Modifier) {
    var preview by remember { mutableStateOf<List<CatalogItem>?>(null) }
    // Privacy/history changes invalidate any captured preview, as they did before the UI update.
    LaunchedEffect(songs) { preview = null }
    Card(modifier = modifier, shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            MixArtwork(songs)
            Column(Modifier.padding(horizontal = 4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMediumEmphasized)
                Text(pluralStringResource(R.plurals.home_song_count, songs.size, songs.size), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = { preview = songs.toList() }, shapes = IconButtonDefaults.shapes(),
                    modifier = Modifier.size(48.dp)) {
                    SungIcon(SungSymbol.QueueMusic, stringResource(R.string.view_mix, title))
                }
                FilledIconButton(onClick = { play(songs.toList()) }, enabled = connected,
                    shapes = IconButtonDefaults.shapes(), modifier = Modifier.size(48.dp).testTag("play_mix_$title")) {
                    SungIcon(SungSymbol.Play, stringResource(R.string.play_mix, title), Modifier.size(28.dp))
                }
                IconButton(onClick = { save(songs.toList()) }, shapes = IconButtonDefaults.shapes(),
                    modifier = Modifier.size(48.dp)) {
                    SungIcon(SungSymbol.Bookmark, stringResource(R.string.save_mix, title))
                }
            }
        }
    }
    preview?.let { snapshot ->
        AlertDialog(onDismissRequest = { preview = null }, title = { Text(title) },
            text = {
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(snapshot, key = { it.id }, contentType = { "mix_preview" }) { song ->
                        Text("${song.title} · ${song.subtitle}", Modifier.padding(vertical = 8.dp))
                    }
                }
            }, confirmButton = {
                TextButton({ play(snapshot); preview = null }, enabled = connected) { Text(stringResource(R.string.play)) }
            }, dismissButton = { TextButton({ preview = null }) { Text(stringResource(R.string.close)) } })
    }
}

/** At most four constrained requests. No bitmap collage allocation, full-resolution load or animation. */
@Composable
private fun MixArtwork(songs: List<CatalogItem>) {
    val artwork = remember(songs) { songs.take(4).map { it.artworkUrl } }
    Column(Modifier.fillMaxWidth().aspectRatio(1.3f).clip(MaterialTheme.shapes.large),
        verticalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(2) { row ->
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(2) { column ->
                    // Two/three-song mixes repeat actual covers; no invented artwork.
                    SungArtwork(artwork.getOrNull((row * 2 + column) % artwork.size.coerceAtLeast(1)),
                        Modifier.weight(1f).fillMaxHeight(), mask = ArtworkMask.SQUARE)
                }
            }
        }
    }
}
