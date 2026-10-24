package com.karthi.sungmobile.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.CatalogSource
import com.karthi.sungmobile.core.model.library.*
import com.karthi.sungmobile.core.playback.PlaybackControl
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/** Requests carry a navigation/load generation so late work cannot change a different screen. */
class AdvancedLibraryViewModel(private val advanced: AdvancedLibraryRepository,
    private val library: LibraryRepository, private val catalog: CatalogSource,
    private val player: PlaybackControl) : ViewModel() {
    private val mutable = MutableStateFlow(AdvancedLibraryState())
    val state = mutable.asStateFlow()
    private val mutableSmartOverview = MutableStateFlow(SmartOverview())
    val smartOverview = mutableSmartOverview.asStateFlow()
    private var navigation = 0L
    private var loadGeneration = 0L
    private var detailGeneration = 0L
    private var loadJob: Job? = null
    private var detailJob: Job? = null
    private var moreJob: Job? = null
    private var commandJob: Job? = null
    private val scanner = PlaylistAvailabilityScanner(viewModelScope, catalog) { results, checking ->
        val ids = mutable.value.entries.mapTo(mutableSetOf()) { it.rowId }
        val current = results.filterKeys { it in ids }
        mutable.update { it.copy(confirmedUnavailable = current.filterValues { it == AvailabilityOutcome.UNAVAILABLE }.keys,
            unknownChecks = current.count { it.value == AvailabilityOutcome.UNKNOWN }, checkedCount = current.size,
            checking = checking) }
    }

    init { viewModelScope.launch {
        advanced.changes.onStart { emit(Unit) }.collectLatest {
            reload()
            try { mutableSmartOverview.value = SmartOverview(advanced.smartPlaylists(), loading = false) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutableSmartOverview.value = mutableSmartOverview.value.copy(loading = false, error = true) }
        }
    } }
    private fun navigate(next: AdvancedLibraryState) {
        navigation++
        loadJob?.cancel(); moreJob?.cancel(); cancelDetail()
        scanner.reset()
        mutable.value = next.copy(busy = commandJob?.isActive == true)
        reload()
    }
    fun openSmartList() = navigate(AdvancedLibraryState(page = AdvancedPage.SMART_LIST))
    fun openSmart(id: String) = navigate(AdvancedLibraryState(page = AdvancedPage.SMART_DETAIL, smartId = id))
    fun openCleanup(id: String) = navigate(AdvancedLibraryState(page = AdvancedPage.CLEANUP, playlistId = id))
    fun openStatistics() = navigate(AdvancedLibraryState(page = AdvancedPage.STATISTICS))
    fun close() = navigate(AdvancedLibraryState())
    fun back() { if (mutable.value.page == AdvancedPage.SMART_DETAIL) openSmartList() else close() }
    fun dismissMessage() { mutable.update { it.copy(message = null) } }

    private fun reload() {
        val request = ++loadGeneration
        val location = navigation
        val target = mutable.value
        loadJob?.cancel(); moreJob?.cancel()
        if (target.page == AdvancedPage.CLOSED) return
        loadJob = viewModelScope.launch {
            fun publish(change: (AdvancedLibraryState) -> AdvancedLibraryState) {
                if (location == navigation && request == loadGeneration) mutable.update(change)
            }
            try {
                when (target.page) {
                    AdvancedPage.SMART_LIST -> {
                        val lists = advanced.smartPlaylists()
                        publish { it.copy(smartPlaylists = lists) }
                    }
                    AdvancedPage.SMART_DETAIL -> {
                        val id = target.smartId ?: return@launch
                        val lists = advanced.smartPlaylists()
                        val songs = advanced.smartSongs(id)
                        publish { it.copy(smartPlaylists = lists, smartSongs = songs) }
                    }
                    AdvancedPage.CLEANUP -> {
                        val id = target.playlistId ?: return@launch
                        val playlist = library.playlist(id)
                        if (playlist == null) { if (location == navigation) close(); return@launch }
                        val rows = library.songs(LibraryCollection.Playlist(id), limit = Int.MAX_VALUE)
                        val revisions = advanced.revisions(id)
                        publish { old -> old.copy(playlistName = playlist.name, entries = rows, revisions = revisions,
                            cleanupLoaded = true, selected = if (!old.cleanupLoaded) duplicateEntryIds(rows)
                            else old.selected.intersect(rows.map { it.rowId }.toSet())) }
                        if (location == navigation && request == loadGeneration) scanner.retain(rows.map { it.rowId }.toSet())
                    }
                    AdvancedPage.STATISTICS -> {
                        val stats = advanced.statistics(30)
                        publish { it.copy(stats = stats, statsMore = stats.recentEvents.size == 30, loadingMore = false) }
                    }
                    AdvancedPage.CLOSED -> Unit
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { publish { it.copy(message = "Could not load this library view. Retry.", loadingMore = false) } }
        }
    }
    fun retry() = reload()

    /** Capture action targets at the tap; navigation does not redirect a confirmed database write. */
    private fun command(block: suspend (Long) -> Unit) {
        if (commandJob?.isActive == true) return
        val location = navigation
        mutable.update { it.copy(busy = true, message = null) }
        commandJob = viewModelScope.launch {
            try { block(location) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { if (location == navigation) mutable.update {
                it.copy(message = error.message?.takeIf { error is IllegalArgumentException } ?: "Could not save this change. Retry.")
            } }
            finally { mutable.update { it.copy(busy = false) }; reload() }
        }
    }
    private fun cancelDetail() { detailGeneration++; detailJob?.cancel() }
    private fun detail(block: suspend () -> (() -> Unit)) {
        cancelDetail()
        val request = detailGeneration
        val location = navigation
        detailJob = viewModelScope.launch {
            try {
                val publish = block()
                ensureActive()
                if (request == detailGeneration && location == navigation) publish()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { if (request == detailGeneration && location == navigation)
                mutable.update { it.copy(message = "Could not load the preview. Retry.") } }
        }
    }
    fun previewRule(rule: SmartRule, show: (List<CatalogItem>) -> Unit) = detail {
        val songs = advanced.previewSmart(rule)
        val publish: () -> Unit = { show(songs) }; publish
    }
    fun saveSmart(id: String?, name: String, rule: SmartRule) = command { location ->
        val saved = advanced.saveSmart(id, name, rule)
        if (location == navigation) openSmart(saved)
    }
    fun deleteSmart(id: String) = command { location ->
        advanced.deleteSmart(id)
        if (location == navigation) openSmartList()
    }
    fun playSmart(shuffle: Boolean) {
        val id = mutable.value.smartId ?: return
        command { location ->
            val songs = advanced.smartSongs(id)
            if (location == navigation && songs.isNotEmpty()) player.playCollection(songs, shuffle)
        }
    }
    fun saveOrdinary(name: String) {
        val id = mutable.value.smartId ?: return
        command { location ->
            val songs = advanced.smartSongs(id)
            library.create(name, songs, allowDuplicates = true)
            if (location == navigation) mutable.update { it.copy(message = "Ordinary playlist saved with ${songs.size} songs") }
        }
    }
    fun toggleEntry(id: String) {
        val current = mutable.value
        if (id !in duplicateOccurrenceIds(current.entries) + current.confirmedUnavailable) return
        mutable.update { it.copy(selected = if (id in it.selected) it.selected - id else it.selected + id) }
    }
    fun selectDuplicates() { mutable.update { it.copy(selected = duplicateEntryIds(it.entries)) } }
    fun clearSelection() { mutable.update { it.copy(selected = emptySet()) } }
    fun removeEntries(playlistId: String, ids: Set<String>) {
        val selected = ids.toSet()
        if (selected.isEmpty()) return
        command { location ->
            library.removeEntries(playlistId, selected)
            if (location == navigation) mutable.update { it.copy(selected = it.selected - selected,
                message = "Selected entries removed") }
        }
    }
    fun scanAvailability() = scanner.scan(mutable.value.entries)
    fun cancelScan() = scanner.cancel()
    fun retryUnknown() = scanner.retryUnknown(mutable.value.entries)
    fun showRevision(id: String) {
        val playlistId = mutable.value.playlistId ?: return
        detail {
            val result = advanced.revision(id)?.takeIf { it.revision.playlistId == playlistId }
            val publish: () -> Unit = { mutable.update { it.copy(revisionDetail = result) } }; publish
        }
    }
    fun hideRevision() { cancelDetail(); mutable.update { it.copy(revisionDetail = null) } }
    fun restoreRevision(id: String) {
        val playlistId = mutable.value.playlistId ?: return
        command { location ->
            advanced.restoreRevision(playlistId, id)
            if (location == navigation) { hideRevision(); mutable.update { it.copy(selected = emptySet(), message = "Playlist version restored") } }
        }
    }
    fun moreEvents() {
        val old = mutable.value.stats
        if (mutable.value.page != AdvancedPage.STATISTICS || old.privateMode || moreJob?.isActive == true) return
        val location = navigation
        val request = loadGeneration
        mutable.update { it.copy(loadingMore = true) }
        moreJob = viewModelScope.launch {
            try {
                val page = advanced.statistics(30, old.recentEvents.size)
                ensureActive()
                if (location == navigation && request == loadGeneration) mutable.update {
                    it.copy(stats = if (page.privateMode) page else page.copy(recentEvents =
                        (old.recentEvents + page.recentEvents).distinctBy { event -> event.id }), statsMore = page.recentEvents.size == 30)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { if (location == navigation && request == loadGeneration)
                mutable.update { it.copy(message = "Could not load more listening periods. Retry.") } }
            finally { if (location == navigation && request == loadGeneration) mutable.update { it.copy(loadingMore = false) } }
        }
    }
}
