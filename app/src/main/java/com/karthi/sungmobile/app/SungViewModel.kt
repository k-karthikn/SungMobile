package com.karthi.sungmobile.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karthi.sungmobile.core.model.*
import com.karthi.sungmobile.core.playback.PlaybackControl
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SungViewModel(
    private val catalog: CatalogSource,
    val player: PlaybackControl,
    private val closeConnection: () -> Unit = {},
) : ViewModel() {
    private val mutableCatalog = MutableStateFlow(CatalogState())
    val catalogState = mutableCatalog.asStateFlow()
    private var catalogJob: Job? = null
    private var generation = 0
    private val history = ArrayDeque<CatalogPage?>()
    var onSearchSubmitted: (String, CatalogKind) -> Unit = { _, _ -> }
    private var retryAction: (() -> Unit)? = null

    fun editQuery(query: String) { mutableCatalog.update { it.copy(query = query.take(500)) } }

    fun chooseKind(kind: CatalogKind) {
        mutableCatalog.update { it.copy(kind = kind) }
        if (catalogState.value.query.isNotBlank()) search()
    }

    fun search() = search(recordHistory = true)

    fun recall(query: String, kind: CatalogKind) {
        mutableCatalog.update { it.copy(query = query, kind = kind) }; search()
    }

    private fun search(recordHistory: Boolean) {
        val current = catalogState.value
        val query = current.query.trim()
        if (query.isEmpty()) return
        if (recordHistory) onSearchSubmitted(query, current.kind)
        val isLink = query.contains("://") || query.startsWith("www.")
        history.clear()
        request(clear = true, inCollection = isLink, retry = { search(recordHistory = true) }) {
            if (isLink) catalog.open(query) else catalog.search(query, current.kind)
        }
    }

    fun open(item: CatalogItem) {
        if (item.kind == CatalogKind.SONG) {
            val songs = catalogState.value.page?.items.orEmpty().filter { it.kind == CatalogKind.SONG }
            val index = songs.indexOfFirst { it.id == item.id }
            if (index >= 0) player.select(songs, index) else player.select(listOf(item), 0)
        } else {
            if (history.size >= 20) history.removeFirst()
            history.addLast(catalogState.value.page)
            openCollection(item)
        }
    }

    private fun openCollection(item: CatalogItem) {
        request(clear = true, inCollection = true, retry = { openCollection(item) }) { catalog.open(item.url) }
    }

    fun searchArtistSongs(artistName: String) {
        mutableCatalog.update { it.copy(query = artistName.take(500), kind = CatalogKind.SONG) }
        search()
    }

    fun loadMore() {
        val current = catalogState.value
        val previous = current.page ?: return
        val token = previous.next ?: return
        if (current.loading) return
        request(clear = false, inCollection = current.inCollection, retry = ::loadMore) {
            val next = catalog.nextPage(token)
            previous.copy(items = (previous.items + next.items).distinctBy { it.id },
                next = next.next, partial = previous.partial || next.partial)
        }
    }

    fun back() {
        generation++
        catalogJob?.cancel()
        retryAction = null
        val previous = history.removeLastOrNull()
        mutableCatalog.update { it.copy(page = previous, inCollection = history.isNotEmpty(), loading = false, error = null) }
    }

    fun retry() { retryAction?.invoke() }

    private fun request(clear: Boolean, inCollection: Boolean, retry: () -> Unit, block: suspend () -> CatalogPage) {
        val request = ++generation
        catalogJob?.cancel()
        retryAction = retry
        mutableCatalog.update { it.copy(loading = true, error = null, inCollection = inCollection,
            page = if (clear) null else it.page) }
        catalogJob = viewModelScope.launch {
            try {
                val page = block()
                if (request == generation) mutableCatalog.update { it.copy(page = page, loading = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: SourceException) {
                if (request == generation) mutableCatalog.update { it.copy(loading = false, error = error.failure) }
            }
        }
    }

    override fun onCleared() {
        closeConnection()
    }
}
