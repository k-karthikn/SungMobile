package com.karthi.sungmobile.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karthi.sungmobile.core.model.library.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class LibraryQuery(val collection: LibraryCollection? = LibraryCollection.Likes,
    val filter: String = "", val sort: LibrarySort = LibrarySort.DEFAULT, val limit: Int = 50)
data class LibraryPage(val query: LibraryQuery = LibraryQuery(), val songs: List<LibrarySong> = emptyList(),
    val playlists: List<LocalPlaylist> = emptyList(), val title: String = "Liked songs", val loading: Boolean = true,
    val more: Boolean = false, val error: Boolean = false)

/** Owns only the current local list; mutation and playback lifetimes are independent. */
class LibraryBrowserViewModel(private val repository: LibraryRepository) : ViewModel() {
    private val query = MutableStateFlow(LibraryQuery())
    private val mutablePage = MutableStateFlow(LibraryPage())
    val page = mutablePage.asStateFlow()
    init {
        viewModelScope.launch {
            combine(query, repository.changes) { request, _ -> request }.collectLatest { request ->
                val previous = mutablePage.value
                mutablePage.value = if (previous.query.collection == request.collection &&
                    previous.query.filter == request.filter && previous.query.sort == request.sort) {
                    previous.copy(query = request, loading = true, error = false)
                } else LibraryPage(request)
                try {
                    val collection = request.collection
                    val songs = if (collection != null) repository.songs(collection, request.filter, request.sort, request.limit + 1) else emptyList()
                    val playlists = if (collection == null) repository.playlists(request.filter, request.sort, request.limit + 1) else emptyList()
                    val title = when (collection) {
                        null -> "Playlists"
                        LibraryCollection.Likes -> "Liked songs"
                        LibraryCollection.Recent -> "Recently played"
                        is LibraryCollection.Playlist -> repository.playlist(collection.id)?.name ?: "Playlist deleted"
                    }
                    mutablePage.value = LibraryPage(request, songs.take(request.limit), playlists.take(request.limit), title,
                        loading = false, more = songs.size > request.limit || playlists.size > request.limit)
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { mutablePage.value = LibraryPage(request, loading = false, error = true) }
            }
        }
    }
    fun open(collection: LibraryCollection?) { query.value = LibraryQuery(collection) }
    fun filter(value: String) { query.update { it.copy(filter = value.take(120), limit = 50) } }
    fun sort(value: LibrarySort) { query.update { it.copy(sort = value, limit = 50) } }
    fun more() { query.update { it.copy(limit = it.limit + 50) } }
    fun retry() { query.update { it.copy(limit = it.limit + 1) } }
}
