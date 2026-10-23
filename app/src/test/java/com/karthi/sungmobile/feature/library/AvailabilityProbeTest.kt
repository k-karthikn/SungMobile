package com.karthi.sungmobile.feature.library

import com.karthi.sungmobile.core.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AvailabilityProbeTest {
    private val song = CatalogItem("RB-RcX5DS5A", CatalogKind.SONG, "Yellow", "Coldplay",
        "https://www.youtube.com/watch?v=RB-RcX5DS5A")
    private fun source(result: suspend () -> CatalogPage) = object : CatalogSource {
        override suspend fun search(query: String, kind: CatalogKind) = error("unused")
        override suspend fun nextPage(token: PageToken) = error("unused")
        override suspend fun open(url: String) = result()
    }
    @Test fun onlyConfirmedUnavailableBecomesRemovalCandidate() = runBlocking {
        assertEquals(AvailabilityOutcome.AVAILABLE,
            probeAvailability(source { CatalogPage("Yellow", listOf(song)) }, song))
        assertEquals(AvailabilityOutcome.UNAVAILABLE,
            probeAvailability(source { throw SourceException(SourceFailure.UNAVAILABLE) }, song))
        for (failure in listOf(SourceFailure.NETWORK, SourceFailure.RATE_LIMITED,
            SourceFailure.AUTHENTICATION_REQUIRED, SourceFailure.EXTRACTION)) {
            assertEquals(AvailabilityOutcome.UNKNOWN,
                probeAvailability(source { throw SourceException(failure) }, song))
        }
    }
    @Test fun cancellationStopsProbe() = runBlocking {
        try {
            probeAvailability(source { throw CancellationException() }, song)
            fail("Expected cancellation")
        } catch (_: CancellationException) { }
    }
}
