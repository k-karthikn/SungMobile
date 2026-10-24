package com.karthi.sungmobile.feature.library

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.library.LibraryRepository
import com.karthi.sungmobile.core.model.library.libraryKeyOrNull
import com.karthi.sungmobile.core.playback.PlaybackState

/** Cross-screen actions and dialogs live here instead of being duplicated in feature screens. */
@Composable
fun LibraryHost(repository: LibraryRepository, actions: LibraryActionsViewModel, playback: PlaybackState, content: @Composable () -> Unit) {
    val liked by actions.liked.collectAsStateWithLifecycle()
    val message by actions.message.collectAsStateWithLifecycle()
    var adding by remember { mutableStateOf<List<CatalogItem>?>(null) }
    var savingQueue by remember { mutableStateOf<List<CatalogItem>?>(null) }
    val libraryUi = remember(liked, actions, playback.queue.entries) {
        LibraryUi(liked, actions::toggle, { adding = it },
            { savingQueue = playback.queue.entries.map { it.track }
                .filter { it.libraryKeyOrNull() != null } })
    }
    CompositionLocalProvider(LocalLibraryUi provides libraryUi) { content() }
    adding?.let { AddToPlaylistDialog(it, repository, actions) { adding = null } }
    savingQueue?.let { tracks -> PlaylistNameDialog("Save queue as playlist", help = "Copies ${tracks.size} online queue entries, including already played songs and duplicates, in their current order. Local files stay in the queue only.",
        dismiss = { savingQueue = null }, save = { actions.create(it, tracks, duplicates = true) }) }
    message?.let { text -> AlertDialog(onDismissRequest = actions::dismissMessage, text = { Text(text) },
        confirmButton = { TextButton(actions::dismissMessage) { Text("OK") } }) }
}
