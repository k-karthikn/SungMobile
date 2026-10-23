package com.karthi.sungmobile.feature.library

import androidx.lifecycle.ViewModelStore
import com.karthi.sungmobile.core.model.library.*
import com.karthi.sungmobile.testing.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class AdvancedLibraryViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val repository = TestAdvancedRepository()
    private val library = TestLocalLibrary()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { store.clear(); Dispatchers.resetMain() }
    private fun model() = AdvancedLibraryViewModel(repository, library, TestCatalog(), RecordingPlayer()).also { store.put("library", it) }

    @Test fun confirmedRemovalStaysOnItsOriginalPlaylistAfterNavigation() = runTest(dispatcher) {
        val model = model()
        model.openCleanup("A"); runCurrent()
        assertEquals(setOf("second"), model.state.value.selected)
        model.toggleEntry("first")
        assertEquals(setOf("first", "second"), model.state.value.selected)
        model.removeEntries("A", setOf("first"))
        model.openCleanup("B"); runCurrent()
        assertEquals(listOf("A" to setOf("first")), library.removed)
        assertEquals("B", model.state.value.playlistId)
        assertNull(model.state.value.message)
    }
    @Test fun hiddenVersionPreviewCannotReappearAfterALateRead() = runTest(dispatcher) {
        val pending = CompletableDeferred<RevisionDetail?>()
        repository.readRevision = { withContext(NonCancellable) { pending.await() } }
        val model = model()
        model.openCleanup("A"); runCurrent()
        model.showRevision("old"); runCurrent()
        model.hideRevision()
        pending.complete(RevisionDetail(PlaylistRevision("old", "A", "Old", 1, 0), emptyList()))
        runCurrent()
        assertNull(model.state.value.revisionDetail)
    }
    @Test fun sameSizedHistoryRefreshRejectsAnOlderPaginationResult() = runTest(dispatcher) {
        fun events(prefix: String) = List(30) { ListeningEvent("$prefix-$it", testSong, 1, 2, 30_000) }
        var current = events("original")
        val pending = CompletableDeferred<ListeningStats>()
        repository.stats = { offset -> if (offset == 0) ListeningStats(recentEvents = current)
            else withContext(NonCancellable) { pending.await() } }
        val model = model()
        model.openStatistics(); runCurrent()
        model.moreEvents(); runCurrent()
        current = events("replacement")
        repository.changes.emit(Unit); runCurrent()
        pending.complete(ListeningStats(recentEvents = events("stale"))); runCurrent()
        assertEquals(current, model.state.value.stats.recentEvents)
        assertFalse(model.state.value.loadingMore)
    }
}
