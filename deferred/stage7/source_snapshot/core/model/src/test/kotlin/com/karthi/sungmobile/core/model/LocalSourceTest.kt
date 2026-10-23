package com.karthi.sungmobile.core.model

import com.karthi.sungmobile.core.model.library.libraryKey
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalSourceTest {
    @Test fun deviceUriHasSeparateLibraryIdentityFromYouTube() {
        val local = CatalogItem("content://media/external/audio/media/42", CatalogKind.SONG,
            "Local", "Artist", "content://media/external/audio/media/42")
        val youtube = CatalogItem("https://www.youtube.com/watch?v=RB-RcX5DS5A", CatalogKind.SONG,
            "Online", "Artist", "https://www.youtube.com/watch?v=RB-RcX5DS5A")
        assertEquals(TrackSource.DEVICE, local.source)
        assertEquals("device:content://media/external/audio/media/42", local.libraryKey())
        assertEquals(TrackSource.YOUTUBE, youtube.source)
        assertEquals("youtube:RB-RcX5DS5A", youtube.libraryKey())
    }
}
