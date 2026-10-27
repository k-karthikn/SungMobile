package com.karthi.sungmobile.feature.collection

import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.CatalogKind
import com.karthi.sungmobile.core.model.CatalogPage
import com.karthi.sungmobile.core.model.catalog.CatalogDetails
import org.junit.Assert.assertEquals
import org.junit.Test

class CollectionArtworkTest {
    @Test fun searchThumbnailFillsMissingCollectionCover() {
        val route = CollectionRoute("https://www.youtube.com/playlist?list=PLtest", "Mix",
            CatalogKind.PLAYLIST, artworkUrl = "https://example.com/search.jpg")
        val page = CatalogPage("Mix", listOf(song("https://example.com/song.jpg")),
            details = CatalogDetails(route.url, CatalogKind.PLAYLIST))
        assertEquals("https://example.com/search.jpg", CollectionState(route, page).coverArtworkUrl())
    }

    @Test fun openedCoverWinsAndFirstSongIsLastFallback() {
        val route = CollectionRoute("url", "Album", CatalogKind.ALBUM)
        val song = song("https://example.com/song.jpg")
        val page = CatalogPage("Album", listOf(song),
            details = CatalogDetails("url", CatalogKind.PLAYLIST, artworkUrl = "https://example.com/album.jpg"))
        assertEquals("https://example.com/album.jpg", CollectionState(route, page).coverArtworkUrl())
        assertEquals("https://example.com/song.jpg",
            CollectionState(route, page.copy(details = page.details?.copy(artworkUrl = null))).coverArtworkUrl())
        assertEquals(CatalogKind.ALBUM, CollectionState(route, page).displayKind())
    }

    private fun song(artwork: String) = CatalogItem("song", CatalogKind.SONG, "Song", "Artist",
        "https://www.youtube.com/watch?v=RB-RcX5DS5A", artwork)
}
