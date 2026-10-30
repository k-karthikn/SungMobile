package com.karthi.sungmobile.core.data.local

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LocalArtworkHelpersTest {
    @Test fun downloadedMediaUriMatchesLibraryScanAlias() {
        val downloaded = Uri.parse("content://media/external_primary/audio/media/42")
        val scanned = Uri.parse("content://media/external/audio/media/42")
        assertEquals(artworkCacheIdentity(downloaded), artworkCacheIdentity(scanned))
        assertEquals("media:external:42", artworkCacheIdentity(scanned))
    }

    @Test fun sidecarPrecedenceMatchesSung() {
        val specific = Uri.parse("content://documents/specific")
        val shared = Uri.parse("content://documents/shared")
        val images = mapOf("song.jpg" to specific, "cover.png" to shared)
        assertEquals(specific, localCover("Song.mp3", images))
        assertEquals(shared, localCover("Other.flac", images))
        assertEquals(null, localCover("Other.flac", emptyMap()))
    }
}
