package com.karthi.sungmobile.app

import androidx.lifecycle.*
import com.karthi.sungmobile.core.model.*
import com.karthi.sungmobile.core.model.catalog.MusicLinks
import com.karthi.sungmobile.core.playback.PlaybackControl
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/** Search only. Collection routes and paging have a separate ViewModel. */
class SungViewModel(private val catalog: CatalogSource, val player: PlaybackControl,
    private val saved: SavedStateHandle, private val closeConnection: () -> Unit = {}) : ViewModel() {
    private val mutableCatalog = MutableStateFlow(CatalogState(query = saved["query"] ?: "",
        kind = saved.get<String>("kind")?.let(CatalogKind::valueOf) ?: CatalogKind.SONG))
    val catalogState = mutableCatalog.asStateFlow()
    private var job: Job? = null
    private var generation = 0
    private var active = false
    private var committed = false
    private val visited = mutableSetOf<PageToken>()
    private var retryAction: (() -> Unit)? = null
    var onSearchSubmitted: (String, CatalogKind) -> Unit = { _, _ -> }
    var onOpenCollection: (CatalogItem) -> Unit = {}
    var onOpenLink: (String) -> Unit = {}

    fun editQuery(query: String) {
        invalidate(); committed = false
        saved["query"] = query.take(500)
        mutableCatalog.update { it.copy(query = query.take(500), page = null, error = null, loading = false) }
        schedule()
    }
    fun chooseKind(kind: CatalogKind) {
        invalidate(); committed = false; saved["kind"] = kind.name
        mutableCatalog.update { it.copy(kind = kind, page = null, error = null, loading = false) }
        if (catalogState.value.query.isNotBlank()) search()
    }
    fun setActive(enabled: Boolean) {
        active = enabled
        if (!enabled) { invalidate(); mutableCatalog.update { it.copy(loading = false) } }
        else if (!catalogState.value.loading && catalogState.value.page == null && catalogState.value.error == null) schedule()
    }
    private fun invalidate() { generation++; job?.cancel() }
    private fun schedule() {
        val current = catalogState.value
        if (!active || current.query.trim().length < 2 || MusicLinks.looksLikeLink(current.query)) return
        val request = generation
        job = viewModelScope.launch { delay(350); if (request == generation) search(record = false) }
    }
    fun search() = search(record = true)
    private fun search(record: Boolean) {
        val current = catalogState.value
        if (current.query.isBlank()) return
        if (MusicLinks.looksLikeLink(current.query)) { invalidate(); onOpenLink(current.query); return }
        if (record) commit(force = true)
        visited.clear()
        request(clear = true, retry = ::search) { catalog.search(current.query.trim(), current.kind) }
    }
    private fun commit(force: Boolean = false) {
        if (!committed || force) {
            val current = catalogState.value
            onSearchSubmitted(current.query.trim(), current.kind); committed = true
        }
    }
    fun recall(query: String, kind: CatalogKind) {
        invalidate(); committed = false; saved["query"] = query; saved["kind"] = kind.name
        mutableCatalog.update { it.copy(query = query, kind = kind) }; search()
    }
    fun open(item: CatalogItem) {
        commit()
        if (item.kind == CatalogKind.SONG) {
            val songs = catalogState.value.page?.items.orEmpty().filter { it.kind == CatalogKind.SONG }
            val index = songs.indexOfFirst { it.id == item.id }
            if (index >= 0) player.select(songs, index) else player.select(listOf(item), 0)
        } else { setActive(false); onOpenCollection(item) }
    }
    fun searchArtistSongs(name: String) = recall(name, CatalogKind.SONG)
    fun loadMore() {
        val current = catalogState.value
        val page = current.page ?: return
        val token = page.next ?: return
        if (current.loading) return
        if (token in visited) { mutableCatalog.update { it.copy(page = page.copy(next = null, partial = true)) }; return }
        val request = generation + 1
        request(clear = false, retry = ::loadMore) {
            val next = catalog.nextPage(token)
            if (request != generation) throw CancellationException()
            visited += token
            val items = (page.items + next.items).distinctBy { it.id }
            val loop = next.next in visited || items.size == page.items.size && next.next != null
            page.copy(items = items, next = if (loop) null else next.next, partial = page.partial || next.partial || loop)
        }
    }
    fun retry() { if (catalogState.value.error == SourceFailure.EXPIRED_PAGE) search() else retryAction?.invoke() }
    private fun request(clear: Boolean, retry: () -> Unit, fetch: suspend () -> CatalogPage) {
        invalidate(); val request = generation; retryAction = retry
        mutableCatalog.update { it.copy(loading = true, error = null, page = if (clear) null else it.page) }
        job = viewModelScope.launch {
            try {
                val page = fetch()
                if (request == generation) mutableCatalog.update { it.copy(page = page, loading = false) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                if (request == generation) mutableCatalog.update { it.copy(loading = false, error = (error as? SourceException)?.failure ?: SourceFailure.EXTRACTION) }
            }
        }
    }
    override fun onCleared() { closeConnection() }
}
