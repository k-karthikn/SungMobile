package com.karthi.sungmobile.core.model

import com.karthi.sungmobile.core.model.library.*
import org.junit.Assert.*
import org.junit.Test

class AdvancedLibraryTest {
    private fun song(id: String) = CatalogItem(id, CatalogKind.SONG, id, "Artist", "https://www.youtube.com/watch?v=$id")

    @Test fun rulesAreDeterministicAndPrivateModeHidesHistoryOnly() {
        val a = song("RB-RcX5DS5A")
        val b = song("dvgZkm1xWPE")
        val candidates = listOf(SmartCandidate(a, true, 2, 1000), SmartCandidate(b, true, 1, 2000))
        val rule = SmartRule(requireLiked = true, minPlays = 2, sort = SmartSort.MOST_PLAYED)
        assertEquals(listOf(a), evaluateSmartRule(rule, candidates, 3000, false))
        assertTrue(evaluateSmartRule(rule, candidates, 3000, true).isEmpty())
        assertEquals(listOf(b, a), evaluateSmartRule(SmartRule(), candidates, 3000, true))
    }

    @Test fun duplicateSelectionKeepsFirstOccurrence() {
        val a = song("RB-RcX5DS5A")
        val rows = listOf(LibrarySong("first", a, false, 0), LibrarySong("other", song("dvgZkm1xWPE"), false, 1),
            LibrarySong("later", a.copy(url = "https://youtu.be/RB-RcX5DS5A"), false, 2))
        assertEquals(setOf("later"), duplicateEntryIds(rows))
        assertEquals(setOf("first", "later"), duplicateOccurrenceIds(rows))
    }

    @Test fun periodsSplitAfterThirtyMinuteGap() {
        val track = song("RB-RcX5DS5A")
        val events = listOf(ListeningEvent("a", track, 0, 30_000, 30_000),
            ListeningEvent("b", track, 35_000, 70_000, 35_000),
            ListeningEvent("c", track, 2_000_000, 2_030_000, 30_000))
        assertEquals(listOf(1, 2), listeningPeriods(events).map { it.events.size })
    }
}
