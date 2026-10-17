package com.karthi.sungmobile.core.model.catalog

import com.karthi.sungmobile.core.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionBatchLoaderTest {
    private val song = CatalogItem("a", CatalogKind.SONG, "A", "Artist", "https://youtu.be/dQw4w9WgXcQ")
    private val token = PageToken("next")
    private val first = CatalogPage("Mix", listOf(song), token)
    private fun source(next: suspend (PageToken) -> CatalogPage) = object : CatalogSource {
        override suspend fun search(query: String, kind: CatalogKind) = error("Unused")
        override suspend fun open(url: String) = error("Unused")
        override suspend fun nextPage(token: PageToken) = next(token)
    }
    @Test fun preservesIntentionalDuplicatesAndSourceOrder() = runTest {
        val result = CollectionBatchLoader(source { CatalogPage("Mix", listOf(song.copy(id = "b"), song)) }).load(first) {}
        assertEquals(BatchStop.COMPLETE, result.stop)
        assertEquals(listOf("a", "b", "a"), result.page.items.map { it.id })
    }
    @Test fun failureRetainsLoadedSubsetAndCanResumeWithoutRepeatingFirstPage() = runTest {
        var fail = true
        val source = source { if (fail) throw SourceException(SourceFailure.NETWORK) else CatalogPage("Mix", listOf(song)) }
        val partial = CollectionBatchLoader(source).load(first) {}
        assertEquals(BatchStop.FAILED, partial.stop); assertEquals(first, partial.page)
        fail = false
        assertEquals(2, CollectionBatchLoader(source).load(partial.page) {}.page.items.size)
    }
    @Test fun limitsEntryCountAndPageRequests() = runTest {
        val many = source { CatalogPage("Mix", List(20) { song }) }
        val limited = CollectionBatchLoader(many, maxEntries = 3).load(first) {}
        assertEquals(BatchStop.LIMIT, limited.stop); assertEquals(3, limited.page.items.size)
        val pages = source { CatalogPage("Mix", listOf(song), PageToken("more")) }
        assertEquals(BatchStop.LIMIT, CollectionBatchLoader(pages, maxPages = 1).load(first) {}.stop)
    }
    @Test fun cyclicAndEmptyContinuationsStopWithoutUnboundedRequests() = runTest {
        assertEquals(BatchStop.PAGE_LOOP, CollectionBatchLoader(source { first }).load(first) {}.stop)
        assertEquals(BatchStop.PAGE_LOOP, CollectionBatchLoader(source { first.copy(items = emptyList()) }).load(first) {}.stop)
    }
    @Test fun timeoutReturnsSubsetButCallerCancellationPropagates() = runTest {
        val never = source { awaitCancellation() }
        val result = CollectionBatchLoader(never, timeoutMs = 100).load(first) {}
        assertEquals(BatchStop.TIMEOUT, result.stop); assertEquals(first, result.page)
        var returned = false
        val task = launch { CollectionBatchLoader(never).load(first) {}; returned = true }
        runCurrent(); task.cancelAndJoin(); assertFalse(returned)
    }
    @Test fun sourcePartialFlagRequiresAnExplicitSubsetDecision() = runTest {
        val result = CollectionBatchLoader(source { error("No paging") }).load(first.copy(next = null, partial = true)) {}
        assertEquals(BatchStop.PARTIAL, result.stop)
    }
}
