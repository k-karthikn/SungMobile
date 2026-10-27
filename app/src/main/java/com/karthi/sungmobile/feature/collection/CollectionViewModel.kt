package com.karthi.sungmobile.feature.collection

import androidx.lifecycle.*
import com.karthi.sungmobile.core.model.*
import com.karthi.sungmobile.core.model.catalog.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID

data class CollectionRoute(val url: String, val title: String, val kind: CatalogKind? = null,
    val startMs: Long = 0, val artworkUrl: String? = null, val id: String = UUID.randomUUID().toString())
data class CollectionState(val route: CollectionRoute? = null, val page: CatalogPage? = null,
    val entries: List<CatalogEntry> = emptyList(), val loading: Boolean = false, val error: SourceFailure? = null)

class CollectionViewModel(private val source: CatalogSource, private val saved: SavedStateHandle) : ViewModel() {
    private val mutable = MutableStateFlow(CollectionState())
    val state = mutable.asStateFlow()
    private var job: Job? = null
    private var generation = 0
    private var retryAction: (() -> Unit)? = null
    private val visited = mutableSetOf<PageToken>()
    init { saved.get<String>("url")?.let { url -> open(CollectionRoute(url, saved["title"] ?: "YouTube",
        saved.get<String>("kind")?.let(CatalogKind::valueOf), saved["start"] ?: 0L,
        saved["artwork"])) } }
    fun open(route: CollectionRoute) {
        saved["url"] = route.url; saved["title"] = route.title; saved["kind"] = route.kind?.name; saved["start"] = route.startMs
        if (route.artworkUrl != null) saved["artwork"] = route.artworkUrl else saved.remove<String>("artwork")
        mutable.value = CollectionState(route)
        refresh()
    }
    fun close() { generation++; job?.cancel(); saved.remove<String>("url"); mutable.value = CollectionState() }
    fun refresh() {
        val route = state.value.route ?: return
        visited.clear()
        request(::refresh) { source.open(route.url) to true }
    }
    fun more() {
        if (state.value.loading) return
        val page = state.value.page ?: return
        val token = page.next ?: return
        if (token in visited) { mutable.update { it.copy(page = page.copy(next = null, partial = true)) }; return }
        val request = generation + 1
        request(::more) {
            val next = source.nextPage(token)
            if (request != generation) throw CancellationException()
            visited += token
            val loop = next.next in visited || next.items.isEmpty() && next.next != null
            page.copy(items = page.items + next.items, next = if (loop) null else next.next,
                partial = page.partial || next.partial || loop, unavailable = page.unavailable + next.unavailable) to false
        }
    }
    fun retry() { if (state.value.error == SourceFailure.EXPIRED_PAGE) refresh() else retryAction?.invoke() }
    private fun request(retry: () -> Unit, fetch: suspend () -> Pair<CatalogPage, Boolean>) {
        retryAction = retry
        val request = ++generation
        job?.cancel()
        mutable.update { it.copy(loading = true, error = null) }
        job = viewModelScope.launch {
            try {
                val (page, replace) = fetch()
                if (request != generation) return@launch
                val existing = if (replace) emptyList() else state.value.entries
                mutable.update { it.copy(page = page, entries = existing + page.items.drop(existing.size).map { track -> CatalogEntry(track = track) }, loading = false) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                if (request == generation) mutable.update { it.copy(loading = false, error = (error as? SourceException)?.failure ?: SourceFailure.EXTRACTION) }
            }
        }
    }
}
