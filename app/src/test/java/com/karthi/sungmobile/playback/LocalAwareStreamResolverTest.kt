package com.karthi.sungmobile.playback

import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.CatalogKind
import com.karthi.sungmobile.core.model.ResolvedStream
import com.karthi.sungmobile.core.model.SourceException
import com.karthi.sungmobile.core.model.StreamResolver
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LocalAwareStreamResolverTest {
    @Test fun contentUriBypassesOnlineResolver() = runTest {
        var onlineCalled = false
        val resolver = LocalAwareStreamResolver(object : StreamResolver {
            override suspend fun resolve(track: CatalogItem): ResolvedStream {
                onlineCalled = true
                return ResolvedStream(track, "https://example.com/audio", "audio/mp4", emptyMap(), 0, null)
            }
        })
        val uri = "content://com.android.providers.media.documents/document/audio%3A123"
        val local = CatalogItem("local:$uri", CatalogKind.SONG, "Local", "Artist", uri)
        val stream = resolver.resolve(local)
        assertEquals(uri, stream.url)
        assertFalse(onlineCalled)
        val online = CatalogItem("id", CatalogKind.SONG, "Online", "Artist", "https://example.com")
        assertSame(online, resolver.resolve(online).track)
    }

    @Test(expected = SourceException::class)
    fun invalidLocalUriCannotReachOnlineResolver() = runTest {
        LocalAwareStreamResolver(object : StreamResolver {
            override suspend fun resolve(track: CatalogItem): ResolvedStream =
                error("Online resolver must not run")
        })
            .resolve(CatalogItem("local:file:///tmp/audio", CatalogKind.SONG,
                "Invalid", "Artist", "file:///tmp/audio"))
    }
}
