package com.karthi.sungmobile.feature.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karthi.sungmobile.core.model.*
import com.karthi.sungmobile.core.model.catalog.*
import com.karthi.sungmobile.core.playback.PlaybackControl
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

enum class CollectionAction(val label: String) { PLAY("Play all"), SHUFFLE("Shuffle all"), NEXT("Play next"), APPEND("Add to queue"), SAVE("Add to local playlist") }
data class CollectionOperation(val action: CollectionAction, val loading: Boolean = true, val count: Int = 0,
    val batch: CollectionBatch? = null, val message: String? = null)

class CollectionActionsViewModel(private val source: CatalogSource, private val player: PlaybackControl) : ViewModel() {
    private val mutable = MutableStateFlow<CollectionOperation?>(null)
    val state = mutable.asStateFlow()
    private var job: Job? = null
    private var generation = 0
    private var expectedQueue: List<String> = emptyList()
    private var route: CollectionRoute? = null
    private val mutableSave = MutableStateFlow<List<CatalogItem>?>(null)
    val save = mutableSave.asStateFlow()
    init { viewModelScope.launch {
        player.state.map { it.queue.entries.map { entry -> entry.id } }.distinctUntilChanged().collect { queue ->
            val pending = state.value
            if (pending != null && pending.action != CollectionAction.SAVE && queue != expectedQueue && pending.message == null) {
                generation++; job?.cancel()
                mutable.value = pending.copy(loading = false, batch = null, message = "The queue changed. Start this action again.")
            }
        }
    } }
    fun begin(details: CollectionState, action: CollectionAction) {
        val page = details.page ?: return
        cancel(); route = details.route
        expectedQueue = player.state.value.queue.entries.map { it.id }
        mutable.value = CollectionOperation(action)
        if (action != CollectionAction.SAVE && !player.state.value.connected) {
            mutable.value = CollectionOperation(action, loading = false, message = "The player is reconnecting. Retry when it is connected.")
            return
        }
        load(page, action)
    }
    private fun load(page: CatalogPage, action: CollectionAction) {
        val request = ++generation
        job = viewModelScope.launch {
            val result = CollectionBatchLoader(source).load(page) { count ->
                if (request == generation) mutable.value = CollectionOperation(action, count = count)
            }
            if (request != generation) return@launch
            mutable.value = CollectionOperation(action, loading = false, count = result.page.items.size, batch = result)
            if (result.stop == BatchStop.COMPLETE) commit()
        }
    }
    fun retry() {
        val pending = state.value ?: return
        val previous = pending.batch ?: return
        if (previous.failure == SourceFailure.EXPIRED_PAGE || previous.stop == BatchStop.PAGE_LOOP) {
            mutable.value = pending.copy(message = "Refresh the collection, then start this action again.")
        } else { job?.cancel(); mutable.value = pending.copy(loading = true); load(previous.page, pending.action) }
    }
    fun commit() {
        val pending = state.value ?: return
        val songs = pending.batch?.page?.items?.filter { it.kind == CatalogKind.SONG }.orEmpty()
        if (pending.loading) return
        if (songs.isEmpty()) {
            mutable.value = pending.copy(message = "No playable songs were returned.")
            return
        }
        if (pending.action != CollectionAction.SAVE && !player.state.value.connected) {
            mutable.value = pending.copy(message = "The player disconnected. Start this action again after reconnecting.")
            return
        }
        if (pending.action != CollectionAction.SAVE && player.state.value.queue.entries.map { it.id } != expectedQueue) { cancel(); return }
        mutable.value = null
        when (pending.action) {
            CollectionAction.PLAY -> if (songs.size == 1 && (route?.startMs ?: 0) > 0) player.playAt(songs.single(), requireNotNull(route).startMs)
                else player.playCollection(songs, false)
            CollectionAction.SHUFFLE -> player.playCollection(songs, true)
            CollectionAction.NEXT -> player.enqueueAll(songs, true)
            CollectionAction.APPEND -> player.enqueueAll(songs, false)
            CollectionAction.SAVE -> mutableSave.value = songs.toList()
        }
    }
    fun savedHandled() { mutableSave.value = null }
    fun cancel() { generation++; job?.cancel(); mutable.value = null }
}
