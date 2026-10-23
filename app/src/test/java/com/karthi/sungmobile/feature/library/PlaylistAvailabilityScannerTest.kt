package com.karthi.sungmobile.feature.library

import com.karthi.sungmobile.core.model.*
import com.karthi.sungmobile.core.model.library.LibrarySong
import com.karthi.sungmobile.testing.TestCatalog
import com.karthi.sungmobile.testing.testSong
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistAvailabilityScannerTest {
    private fun rows(count: Int) = List(count) { LibrarySong("entry-$it", testSong.copy(url = "url-$it"), false, it) }

    @Test fun removingEarlierEntryDoesNotSkipTheNextBatchAndRetryOnlyChecksUnknown() = runTest {
        val rows = rows(102)
        val calls = mutableListOf<String>()
        var offline = true
        val source = object : TestCatalog() {
            override suspend fun open(url: String): CatalogPage {
                calls += url
                if (url == "url-1" && offline) throw SourceException(SourceFailure.NETWORK)
                if (url == "url-2") throw SourceException(SourceFailure.UNAVAILABLE)
                return CatalogPage("ok", emptyList())
            }
        }
        var results: Map<String, AvailabilityOutcome> = emptyMap()
        val scanner = PlaylistAvailabilityScanner(backgroundScope, source) { values, _ -> results = values }
        scanner.retain(rows.map { it.rowId }.toSet())
        scanner.scan(rows); runCurrent()
        assertEquals(100, calls.size)
        assertEquals(AvailabilityOutcome.UNKNOWN, results["entry-1"])
        val remaining = rows.drop(1)
        scanner.retain(remaining.map { it.rowId }.toSet())
        scanner.scan(remaining); runCurrent()
        assertEquals(listOf("url-100", "url-101"), calls.takeLast(2))
        offline = false
        scanner.retryUnknown(remaining); runCurrent()
        assertEquals(103, calls.size)
        assertEquals("url-1", calls.last())
        assertEquals(AvailabilityOutcome.AVAILABLE, results["entry-1"])
        assertEquals(AvailabilityOutcome.UNAVAILABLE, results["entry-2"])
    }

    @Test fun cancelledUncooperativeProbeCannotAffectANewScan() = runTest {
        val old = CompletableDeferred<CatalogPage>()
        var count = 0
        var results: Map<String, AvailabilityOutcome> = emptyMap()
        var checking = false
        val source = object : TestCatalog() {
            override suspend fun open(url: String): CatalogPage {
                count++
                if (count == 1) return withContext(NonCancellable) { old.await() }
                throw SourceException(SourceFailure.UNAVAILABLE)
            }
        }
        val scanner = PlaylistAvailabilityScanner(backgroundScope, source) { values, active -> results = values; checking = active }
        val first = rows(1)
        scanner.retain(setOf("entry-0")); scanner.scan(first); runCurrent()
        scanner.reset()
        val second = listOf(first.single().copy(rowId = "new"))
        scanner.retain(setOf("new")); scanner.scan(second); runCurrent()
        old.complete(CatalogPage("late", emptyList())); runCurrent()
        assertEquals(mapOf("new" to AvailabilityOutcome.UNAVAILABLE), results)
        assertFalse(checking)
    }
}
