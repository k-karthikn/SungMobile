package com.karthi.sungmobile.feature.library

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.library.*

internal fun androidx.compose.foundation.lazy.LazyListScope.smartList(model: AdvancedLibraryViewModel, state: AdvancedLibraryState) {
    item {
        Text("Smart playlists", style = MaterialTheme.typography.headlineMedium)
        Text("Live lists from your Likes and qualified listening history. Play and Shuffle use a snapshot of the current songs.")
        var create by remember { mutableStateOf(false) }
        if (create) SmartRuleDialog(null, { create = false }, model::saveSmart, model::previewRule)
        Button({ create = true }) { Text("New smart playlist") }
        TextButton(model::openStatistics) { Text("Listening statistics") }
    }
    items(state.smartPlaylists, key = { it.id }) { playlist ->
        Surface(onClick = { model.openSmart(playlist.id) }, color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.medium) {
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                Text(playlist.name, style = MaterialTheme.typography.titleMedium)
                Text(ruleLabel(playlist.rule), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
    if (state.smartPlaylists.isEmpty()) item { Text("No smart playlists yet.") }
}

internal fun androidx.compose.foundation.lazy.LazyListScope.smartDetail(model: AdvancedLibraryViewModel, state: AdvancedLibraryState) {
    val playlist = state.smartPlaylists.firstOrNull { it.id == state.smartId }
    item {
        var edit by remember(state.smartId) { mutableStateOf(false) }
        var saveOrdinary by remember { mutableStateOf(false) }
        var delete by remember { mutableStateOf(false) }
        if (edit && playlist != null) SmartRuleDialog(playlist, { edit = false }, model::saveSmart, model::previewRule)
        if (saveOrdinary) PlaylistNameDialog("Save current songs as ordinary playlist", playlist?.name ?: "",
            "This makes an editable snapshot. Later changes to the smart list will not change it.",
            { saveOrdinary = false }, model::saveOrdinary)
        if (delete && playlist != null) ConfirmLibraryChange("Delete smart playlist?", "Its songs and Likes will remain.",
            { delete = false }, { model.deleteSmart(playlist.id) })
        Text(playlist?.name ?: "Smart playlist", style = MaterialTheme.typography.headlineMedium)
        playlist?.let { Text(ruleLabel(it.rule)) }
        Text("${state.smartSongs.size} current songs")
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            Button({ model.playSmart(false) }, enabled = state.smartSongs.isNotEmpty()) { Text("Play") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton({ model.playSmart(true) }, enabled = state.smartSongs.isNotEmpty()) { Text("Shuffle") }
            TextButton({ edit = true }) { Text("Edit rules") }
            TextButton({ saveOrdinary = true }, enabled = state.smartSongs.isNotEmpty()) { Text("Save as ordinary") }
            TextButton({ delete = true }) { Text("Delete") }
        }
        if (state.smartSongs.isEmpty()) Text("No songs match right now. History-based results are hidden in Private mode.")
    }
    items(state.smartSongs, key = { it.id }) { song -> SongSummary(song) }
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
            RuleChoices(listOf(0, 1, 2, 5), plays, { plays = it; previewSongs = null }) { "$it" }
            Text("Played within")
            RuleChoices(listOf(0, 7, 30, 90), days, { days = it; previewSongs = null }) { if (it == 0) "Any time" else "$it days" }
            Text("Order")
            RuleChoices(SmartSort.entries, sort, { sort = it; previewSongs = null }) { it.name.lowercase().replace('_', ' ') }
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

@Composable private fun SongSummary(song: CatalogItem) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(song.title, style = MaterialTheme.typography.titleSmall)
        Text(song.subtitle, style = MaterialTheme.typography.bodySmall)
    }
}

private fun ruleLabel(rule: SmartRule): String = buildList {
    if (rule.requireLiked) add("Liked")
    if (rule.minPlays > 0) add("Played ${rule.minPlays}+ times")
    if (rule.recentDays > 0) add("Within ${rule.recentDays} days")
    add("${rule.maxSongs} max")
}.joinToString(" · ")
