package com.karthi.sungmobile.feature.library

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.designsystem.artwork.SungArtwork
import com.karthi.sungmobile.core.designsystem.icon.SungIcon
import com.karthi.sungmobile.core.designsystem.icon.SungSymbol
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.library.*

internal fun androidx.compose.foundation.lazy.LazyListScope.smartList(model: AdvancedLibraryViewModel, state: AdvancedLibraryState) {
    item(key = "smart_header") {
        var create by remember { mutableStateOf(false) }
        if (create) SmartRuleDialog(null, { create = false }, model::saveSmart, model::previewRule)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Smart playlists", style = MaterialTheme.typography.headlineMedium)
                Text("Lists that update automatically from your library.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FilledTonalButton({ create = true }) {
                SungIcon(SungSymbol.Plus, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Create smart playlist")
            }
        }
    }
    if (state.smartPlaylists.isEmpty()) item(key = "smart_empty") {
        Surface(shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SungIcon(SungSymbol.QueueMusic, modifier = Modifier.size(32.dp))
                Text("Make a mix that keeps itself fresh", style = MaterialTheme.typography.titleMedium)
                Text("Choose Likes or listening rules and Sung will update the list for you.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    items(state.smartPlaylists, key = { "smart:${it.id}" }) { playlist ->
        SmartPlaylistCard(playlist) { model.openSmart(playlist.id) }
    }
}

internal fun androidx.compose.foundation.lazy.LazyListScope.smartDetail(model: AdvancedLibraryViewModel, state: AdvancedLibraryState) {
    val playlist = state.smartPlaylists.firstOrNull { it.id == state.smartId }
    item(key = "smart_detail_header") {
        var edit by remember(state.smartId) { mutableStateOf(false) }
        var saveOrdinary by remember { mutableStateOf(false) }
        var delete by remember { mutableStateOf(false) }
        var more by remember { mutableStateOf(false) }
        if (edit && playlist != null) SmartRuleDialog(playlist, { edit = false }, model::saveSmart, model::previewRule)
        if (saveOrdinary) PlaylistNameDialog("Save current songs as ordinary playlist", playlist?.name ?: "",
            "This makes an editable snapshot. Later changes to the smart list will not change it.",
            { saveOrdinary = false }, model::saveOrdinary)
        if (delete && playlist != null) ConfirmLibraryChange("Delete smart playlist?", "Its songs and Likes will remain.",
            { delete = false }, { model.deleteSmart(playlist.id) })
        if (playlist == null) {
            Text("Smart playlist unavailable", style = MaterialTheme.typography.titleLarge)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.tertiaryContainer) {
                    Row(Modifier.fillMaxWidth().padding(18.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = .72f)) {
                            Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                                SungIcon(SungSymbol.QueueMusic, modifier = Modifier.size(34.dp))
                            }
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("SMART PLAYLIST", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer)
                            Text(playlist.name, style = MaterialTheme.typography.headlineSmall,
                                maxLines = 2, overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onTertiaryContainer)
                            Text("${state.smartSongs.size} songs · ${ruleLabel(playlist.rule)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button({ model.playSmart(false) }, Modifier.weight(1f),
                        enabled = state.smartSongs.isNotEmpty()) {
                        SungIcon(SungSymbol.Play, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Play")
                    }
                    FilledTonalButton({ model.playSmart(true) }, Modifier.weight(1f),
                        enabled = state.smartSongs.isNotEmpty()) {
                        SungIcon(SungSymbol.Shuffle, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Shuffle")
                    }
                }

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton({ edit = true }, Modifier.weight(1f)) {
                        SungIcon(SungSymbol.Equalizer, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Edit rules", maxLines = 1)
                    }
                    TextButton({ saveOrdinary = true }, Modifier.weight(1f),
                        enabled = state.smartSongs.isNotEmpty()) {
                        SungIcon(SungSymbol.Plus, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save a copy", maxLines = 1)
                    }
                    Box {
                        IconButton({ more = true }) { SungIcon(SungSymbol.More, "More playlist options") }
                        DropdownMenu(expanded = more, onDismissRequest = { more = false }) {
                            DropdownMenuItem(text = { Text("Delete smart playlist") }, onClick = {
                                more = false
                                delete = true
                            })
                        }
                    }
                }

                if (state.smartSongs.isEmpty()) {
                    Surface(shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        Text("No songs match these rules yet. Try broadening the rules or check History & privacy.",
                            Modifier.fillMaxWidth().padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Text("Songs", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
    items(state.smartSongs, key = { "smart_song:${it.id}" }) { song -> SmartSongRow(song) }
}

@Composable
private fun SmartPlaylistCard(playlist: SmartPlaylist, open: () -> Unit) {
    Surface(onClick = open, color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.extraLarge) {
        Row(Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.secondaryContainer) {
                Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                    SungIcon(SungSymbol.Shuffle, modifier = Modifier.size(26.dp))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(playlist.name, style = MaterialTheme.typography.titleMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(ruleLabel(playlist.rule), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            SungIcon(SungSymbol.Chevron, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun SmartRuleDialog(existing: SmartPlaylist?, dismiss: () -> Unit,
    save: (String?, String, SmartRule) -> Unit,
    preview: (SmartRule, (List<CatalogItem>) -> Unit) -> Unit) {
    var name by remember(existing?.id) { mutableStateOf(existing?.name ?: "") }
    var liked by remember(existing?.id) { mutableStateOf(existing?.rule?.requireLiked ?: true) }
    var plays by remember(existing?.id) { mutableIntStateOf(existing?.rule?.minPlays ?: 0) }
    var days by remember(existing?.id) { mutableIntStateOf(existing?.rule?.recentDays ?: 0) }
    var sort by remember(existing?.id) { mutableStateOf(existing?.rule?.sort ?: SmartSort.TITLE) }
    var max by remember(existing?.id) { mutableIntStateOf(existing?.rule?.maxSongs ?: 50) }
    var previewSongs by remember(existing?.id) { mutableStateOf<List<CatalogItem>?>(null) }
    val valid = liked || plays > 0 || days > 0
    val rule = if (valid) SmartRule(liked, plays, days, sort, max) else null
    AlertDialog(onDismissRequest = dismiss, title = { Text(if (existing == null) "New smart playlist" else "Edit smart playlist") },
        text = { Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(name, { name = it.take(120) }, label = { Text("Name") }, singleLine = true)
            Row { Checkbox(liked, { liked = it; previewSongs = null }); Text("Liked songs", Modifier.padding(top = 12.dp)) }
            Text("At least this many qualified plays")
            RuleChoices(listOf(0, 1, 2, 5), plays, { plays = it; previewSongs = null }) {
                if (it == 0) "Any" else "$it+"
            }
            Text("Played within")
            RuleChoices(listOf(0, 7, 30, 90), days, { days = it; previewSongs = null }) { if (it == 0) "Any time" else "$it days" }
            Text("Order")
            RuleChoices(SmartSort.entries, sort, { sort = it; previewSongs = null }) {
                when (it) {
                    SmartSort.TITLE -> "Title"
                    SmartSort.RECENT -> "Recently played"
                    SmartSort.MOST_PLAYED -> "Most played"
                }
            }
            Text("Maximum songs")
            RuleChoices(listOf(25, 50, 100), max, { max = it; previewSongs = null }) { "$it" }
            if (!valid) Text("Choose at least one filter.", color = MaterialTheme.colorScheme.error)
            TextButton({ rule?.let { preview(it) { songs -> previewSongs = songs } } }, enabled = rule != null) { Text("Preview current songs") }
            previewSongs?.let { songs ->
                Text("${songs.size} current songs")
                songs.take(10).forEach { Text(it.title) }
                if (songs.size > 10) Text("…and ${songs.size - 10} more")
            }
        } },
        confirmButton = { TextButton({ rule?.let { save(existing?.id, name.trim(), it); dismiss() } },
            enabled = rule != null && name.trim().isNotEmpty()) { Text("Save") } },
        dismissButton = { TextButton(dismiss) { Text("Cancel") } })
}

@Composable
private fun <T> RuleChoices(options: List<T>, selected: T, choose: (T) -> Unit, label: (T) -> String) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { option -> FilterChip(selected == option, { choose(option) }, { Text(label(option)) }) }
    }
}

@Composable
private fun SmartSongRow(song: CatalogItem) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(Modifier.fillMaxWidth().padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            SungArtwork(song.artworkUrl, Modifier.size(52.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(song.title, style = MaterialTheme.typography.titleSmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

private fun ruleLabel(rule: SmartRule): String = buildList {
    if (rule.requireLiked) add("Liked")
    if (rule.minPlays > 0) add("Played ${rule.minPlays}+ times")
    if (rule.recentDays > 0) add("Within ${rule.recentDays} days")
    add("${rule.maxSongs} max")
}.joinToString(" · ")
