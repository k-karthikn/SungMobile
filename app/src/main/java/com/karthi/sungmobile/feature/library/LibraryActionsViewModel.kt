package com.karthi.sungmobile.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karthi.sungmobile.core.model.*
import com.karthi.sungmobile.core.model.library.*
import com.karthi.sungmobile.core.playback.PlaybackControl
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

/** Serial commands keep rapid taps and multi-row edits in their requested order. */
class LibraryActionsViewModel(private val repository: LibraryRepository, private val player: PlaybackControl) : ViewModel() {
    val liked = repository.likedKeys().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())
    private val commands = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    private val mutableMessage = MutableStateFlow<String?>(null)
    val message = mutableMessage.asStateFlow()
    init { viewModelScope.launch {
        for (command in commands) try { command() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { mutableMessage.value = error.message?.takeIf { error is IllegalArgumentException } ?: "Could not save this change. Please retry." }
    } }
    private fun run(command: suspend () -> Unit) { commands.trySend(command) }
    fun dismissMessage() { mutableMessage.value = null }
    fun like(tracks: List<CatalogItem>, value: Boolean) = run { repository.setLiked(tracks, value) }
    fun toggle(track: CatalogItem) = run {
        val keys = repository.likedKeys().first()
        repository.setLiked(listOf(track), track.libraryKey() !in keys)
    }
    fun create(name: String, tracks: List<CatalogItem> = emptyList(), duplicates: Boolean = true) = run {
        repository.create(name, tracks, duplicates); mutableMessage.value = "Playlist created"
    }
    fun rename(id: String, name: String) = run { repository.rename(id, name) }
    fun delete(id: String) = run { repository.deletePlaylist(id) }
    fun pin(playlist: LocalPlaylist) = run { repository.pin(playlist.id, !playlist.pinned) }
    fun add(id: String, tracks: List<CatalogItem>, duplicates: Boolean) = run {
        val result = repository.add(id, tracks, duplicates)
        mutableMessage.value = "Added ${result.added}; skipped ${result.skipped} duplicates"
    }
    fun remove(id: String, entries: Set<String>) = run { repository.removeEntries(id, entries) }
    fun move(id: String, entry: String, offset: Int) = run { repository.moveEntry(id, entry, offset) }
    fun play(collection: LibraryCollection, shuffle: Boolean) = run {
        val tracks = repository.snapshot(collection)
        if (tracks.isNotEmpty()) player.playCollection(tracks, shuffle)
    }
    fun playFrom(collection: LibraryCollection, rowId: String) = run {
        val rows = repository.songs(collection, limit = Int.MAX_VALUE)
        val index = rows.indexOfFirst { it.rowId == rowId }
        if (index >= 0) player.select(rows.map { it.track }, index)
    }
    fun enqueue(tracks: List<CatalogItem>, next: Boolean) { player.enqueueAll(tracks, next) }
}
