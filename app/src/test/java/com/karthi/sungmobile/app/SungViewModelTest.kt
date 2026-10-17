package com.karthi.sungmobile.app

import androidx.lifecycle.ViewModelStore
import com.karthi.sungmobile.core.model.*
import com.karthi.sungmobile.core.playback.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SungViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { store.clear(); Dispatchers.resetMain() }

    @Test fun staleSearchCannotReplaceLatestResults() = runTest(dispatcher) {
        val old = CompletableDeferred<CatalogPage>()
        val source = object : EmptySource() {
            override suspend fun search(query: String, kind: CatalogKind) = if (query == "old") {
                withContext(NonCancellable) { old.await() }
            } else CatalogPage("new", emptyList())
        }
        val model = model(source)
        model.editQuery("old"); model.search(); runCurrent()
        model.editQuery("new"); model.search(); runCurrent()
        old.complete(CatalogPage("old", emptyList())); runCurrent()
        assertEquals("new", model.catalogState.value.page?.title)
    }

    @Test fun paginationFailurePreservesItemsAndRetryDeduplicates() = runTest(dispatcher) {
        val song = CatalogItem("1", CatalogKind.SONG, "Song", "Artist", "https://example.test")
        var attempts = 0
        val source = object : EmptySource() {
            override suspend fun search(query: String, kind: CatalogKind) =
                CatalogPage("Songs", listOf(song), PageToken("next"))
            override suspend fun nextPage(token: PageToken): CatalogPage {
                if (++attempts == 1) throw SourceException(SourceFailure.NETWORK)
                return CatalogPage("Songs", listOf(song, song.copy(id = "2")))
            }
        }
        val model = model(source)
        model.editQuery("music"); model.search(); runCurrent()
        model.loadMore(); runCurrent()
        assertEquals(listOf(song), model.catalogState.value.page?.items)
        assertEquals(SourceFailure.NETWORK, model.catalogState.value.error)
        model.retry(); runCurrent()
        assertEquals(2, model.catalogState.value.page?.items?.size)
        assertNull(model.catalogState.value.page?.next)
        assertNull(model.catalogState.value.error)
    }

    @Test fun previewDebouncesWithoutHistoryAndOpeningCommitsOnce() = runTest(dispatcher) {
        var requests = 0
        val song = CatalogItem("1", CatalogKind.SONG, "Song", "Artist", "https://example.test")
        val source = object : EmptySource() {
            override suspend fun search(query: String, kind: CatalogKind): CatalogPage {
                requests++; return CatalogPage(query, listOf(song))
            }
        }
        val model = model(source); model.setActive(true)
        val history = mutableListOf<String>(); model.onSearchSubmitted = { text, _ -> history += text }
        model.editQuery("mu"); advanceTimeBy(200); model.editQuery("music")
        advanceTimeBy(349); runCurrent(); assertEquals(0, requests)
        advanceTimeBy(1); runCurrent(); assertEquals(1, requests); assertTrue(history.isEmpty())
        model.open(song); model.open(song)
        assertEquals(listOf("music"), history)
    }

    @Test fun editImmediatelyInvalidatesSlowResultBeforeNewDebounce() = runTest(dispatcher) {
        val old = CompletableDeferred<CatalogPage>()
        val model = model(object : EmptySource() {
            override suspend fun search(query: String, kind: CatalogKind) =
                withContext(NonCancellable) { old.await() }
        })
        model.setActive(true); model.editQuery("old"); model.search(); runCurrent()
        model.editQuery("new"); old.complete(CatalogPage("old", emptyList())); runCurrent()
        assertNull(model.catalogState.value.page)
        model.setActive(false)
    }

    @Test fun submitOverridesDebounceAndHiddenSearchCancelsPreview() = runTest(dispatcher) {
        val queries = mutableListOf<String>()
        val model = model(object : EmptySource() {
            override suspend fun search(query: String, kind: CatalogKind): CatalogPage {
                queries += query; return CatalogPage(query, emptyList())
            }
        })
        model.setActive(true); model.editQuery("one"); model.search(); advanceUntilIdle()
        assertEquals(listOf("one"), queries)
        model.editQuery("two"); model.setActive(false); advanceUntilIdle()
        assertEquals(listOf("one"), queries)
        model.setActive(true); advanceUntilIdle(); assertEquals(listOf("one", "two"), queries)
        model.editQuery(""); advanceUntilIdle(); assertNull(model.catalogState.value.page)
    }

    @Test fun openingCollectionRetainsSearchSnapshotAndCategory() = runTest(dispatcher) {
        val model = model(EmptySource())
        model.editQuery("Original"); model.chooseKind(CatalogKind.ALBUM); runCurrent()
        val before = model.catalogState.value
        var opened: CatalogItem? = null; model.onOpenCollection = { opened = it }
        val album = CatalogItem("album", CatalogKind.ALBUM, "Album", "", "https://example.test")
        model.open(album); runCurrent(); model.setActive(true); runCurrent()
        assertEquals(album, opened); assertEquals(before, model.catalogState.value)
    }

    @Test fun enteringSearchDoesNotDuplicateAnExplicitRequestAlreadyInProgress() = runTest(dispatcher) {
        var requests = 0
        val pending = CompletableDeferred<CatalogPage>()
        val model = model(object : EmptySource() {
            override suspend fun search(query: String, kind: CatalogKind): CatalogPage {
                requests++; return pending.await()
            }
        })
        model.recall("Artist", CatalogKind.SONG); runCurrent()
        model.setActive(true); advanceTimeBy(500); runCurrent()
        assertEquals(1, requests)
        pending.complete(CatalogPage("Artist", emptyList())); runCurrent()
    }

    private fun model(source: CatalogSource): SungViewModel {
        val resolver = object : StreamResolver {
            override suspend fun resolve(track: CatalogItem): ResolvedStream = error("Not used")
        }
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val player = PlaybackCoordinator(resolver, NoopEngine(), scope)
        return SungViewModel(source, player, androidx.lifecycle.SavedStateHandle()) { player.release(); scope.cancel() }.also { store.put("model", it) }
    }

    private open class EmptySource : CatalogSource {
        override suspend fun search(query: String, kind: CatalogKind) = CatalogPage(query, emptyList())
        override suspend fun nextPage(token: PageToken) = CatalogPage("", emptyList())
        override suspend fun open(url: String) = CatalogPage("", emptyList())
    }
    private class NoopEngine : PlayerEngine {
        override val state = MutableStateFlow(EngineSnapshot())
        override fun load(stream: ResolvedStream, positionMs: Long) = Unit
        override fun play() = Unit
        override fun pause() = Unit
        override fun seekTo(positionMs: Long) = Unit
        override fun stop() = Unit
        override fun release() = Unit
    }
}
