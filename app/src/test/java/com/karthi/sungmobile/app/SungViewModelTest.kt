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

    @Test fun unavailableArtistSearchesTheSelectedArtistAndClearsCollection() = runTest(dispatcher) {
        val requests = mutableListOf<Pair<String, CatalogKind>>()
        val source = object : EmptySource() {
            override suspend fun search(query: String, kind: CatalogKind): CatalogPage {
                requests += query to kind
                return CatalogPage(query, emptyList())
            }
            override suspend fun open(url: String) = CatalogPage("John Michael Howell", emptyList(),
                notice = CatalogNotice.ArtistBrowseUnavailable("John Michael Howell"))
        }
        val model = model(source)
        model.open(CatalogItem("artist", CatalogKind.ARTIST, "John Michael Howell", "", "https://example.test"))
        runCurrent()
        assertTrue(model.catalogState.value.inCollection)
        model.searchArtistSongs("John Michael Howell"); runCurrent()
        assertEquals(listOf("John Michael Howell" to CatalogKind.SONG), requests)
        assertFalse(model.catalogState.value.inCollection)
        assertNull(model.catalogState.value.error)
    }

    @Test fun collectionRetryDoesNotDuplicateBackHistory() = runTest(dispatcher) {
        var attempts = 0
        val source = object : EmptySource() {
            override suspend fun open(url: String): CatalogPage {
                if (++attempts == 1) throw SourceException(SourceFailure.NETWORK)
                return CatalogPage("Album", emptyList())
            }
        }
        val model = model(source)
        model.editQuery("Original search"); model.search(); runCurrent()
        model.open(CatalogItem("album", CatalogKind.ALBUM, "Album", "", "https://example.test")); runCurrent()
        model.retry(); runCurrent()
        model.back(); runCurrent()
        assertEquals("Original search", model.catalogState.value.page?.title)
        assertFalse(model.catalogState.value.inCollection)
        assertNull(model.catalogState.value.error)
    }

    private fun model(source: CatalogSource): SungViewModel {
        val resolver = object : StreamResolver {
            override suspend fun resolve(track: CatalogItem): ResolvedStream = error("Not used")
        }
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val player = PlaybackCoordinator(resolver, NoopEngine(), scope)
        return SungViewModel(source, player) { player.release(); scope.cancel() }.also { store.put("model", it) }
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
