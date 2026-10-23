package com.karthi.sungmobile.core.youtube.lyrics

import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.CatalogKind
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeTrue
import org.junit.Test

class LiveLyricsTest {
    @Test fun directYouTubeMusicLyrics() = runBlocking {
        assumeTrue(System.getProperty("sung.live") == "true")
        val track = CatalogItem("https://www.youtube.com/watch?v=dQw4w9WgXcQ", CatalogKind.SONG, "Never Gonna Give You Up",
            "Rick Astley", "https://music.youtube.com/watch?v=dQw4w9WgXcQ")
        assertNotNull(YouTubeLyricsGateway().youtube(track))
    }
}
