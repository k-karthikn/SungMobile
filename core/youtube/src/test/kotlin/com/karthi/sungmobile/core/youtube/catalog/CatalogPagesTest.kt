package com.karthi.sungmobile.core.youtube.catalog

import com.karthi.sungmobile.core.model.*
import com.karthi.sungmobile.core.model.catalog.CatalogDetails
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.stream.*
import org.junit.Assert.*
import org.junit.Test

class CatalogPagesTest {
    private fun song() = StreamInfoItem(0, "https://youtu.be/dQw4w9WgXcQ", "Song", StreamType.VIDEO_STREAM)
    private val details = CatalogDetails("https://youtube.com/playlist?list=PL1234567890", CatalogKind.PLAYLIST)
    @Test fun collectionMappingRetainsOccurrencesWhileSearchDeduplicates() {
        val pages = CatalogPages()
        val source = InfoItemsPage(listOf(song(), song()), null, emptyList())
        val search = pages.page("Search", source, CatalogKind.SONG) { error("No paging") }
        val collection = pages.page("Mix", source, CatalogKind.SONG, details) { error("No paging") }
        assertEquals(1, search.items.size); assertEquals(2, collection.items.size)
        assertEquals("", collection.items.first().subtitle); assertNull(collection.items.first().durationMs)
    }
    @Test fun restrictedRowsRetainExposedInformationAndMarkPartialContent() {
        val restricted = song().apply { contentAvailability = ContentAvailability.MEMBERSHIP }
        val source = InfoItemsPage(listOf(song(), restricted), null, emptyList())
        val result = CatalogPages().page("Mix", source, CatalogKind.SONG, details) { error("No paging") }
        assertEquals(1, result.items.size); assertTrue(result.partial)
        assertEquals("Membership required", result.unavailable.single().reason)
        assertEquals("Song", result.unavailable.single().title)
    }
    @Test fun repeatedContinuationHasStableIdentityAndClearExpiresIt() {
        val pages = CatalogPages()
        val source = InfoItemsPage(listOf(song()), Page("https://youtube.com/next", "same"), emptyList())
        val first = pages.page("Mix", source, CatalogKind.SONG, details) { source }
        val token = requireNotNull(first.next)
        assertEquals(token, pages.next(token).next)
        pages.clear()
        assertEquals(SourceFailure.EXPIRED_PAGE, assertThrows(SourceException::class.java) { pages.next(token) }.failure)
    }
    @Test fun continuationStorageIsBoundedAndPartialErrorsRetainUsableRows() {
        val pages = CatalogPages()
        var oldest: PageToken? = null
        repeat(129) {
            val source = InfoItemsPage(listOf(song()), Page("https://youtube.com/next/$it"), listOf(Exception("hidden row")))
            val result = pages.page("Mix", source, CatalogKind.SONG, details) { source }
            assertTrue(result.partial)
            if (it == 0) oldest = result.next
        }
        assertEquals(SourceFailure.EXPIRED_PAGE, assertThrows(SourceException::class.java) { pages.next(requireNotNull(oldest)) }.failure)
    }
}
