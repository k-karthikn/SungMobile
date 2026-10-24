package com.karthi.sungmobile.feature.download

import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.CatalogKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DownloadModelTest {
    @Test fun requestSurvivesJobExtrasWithoutResolvedStream() {
        val track = CatalogItem("video-id", CatalogKind.SONG, "A song", "The artist",
            "https://music.youtube.com/watch?v=RB-RcX5DS5A")
        val restored = DownloadRequest.from(101, DownloadRequest(101, track).extras())
        assertEquals(track, restored?.track)
        assertEquals(101, restored?.id)
    }

    @Test fun filenameIsSafeAndAllowsAnIntentionalRepeat() {
        val first = safeAudioFilename("  A/song: with?bad*chars  ", "m4a")
        val second = safeAudioFilename("  A/song: with?bad*chars  ", "m4a")
        assertTrue(first.startsWith("A_song_ with_bad_chars-"))
        assertTrue(first.endsWith(".m4a"))
        assertFalse(first.contains('/'))
        assertFalse(first.contains(':'))
        assertFalse(first == second)
    }
}
