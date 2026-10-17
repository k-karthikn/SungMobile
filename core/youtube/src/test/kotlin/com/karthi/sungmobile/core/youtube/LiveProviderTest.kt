package com.karthi.sungmobile.core.youtube

import com.karthi.sungmobile.core.model.CatalogKind
import com.karthi.sungmobile.core.model.CatalogNotice
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

/** Explicit network probe: ./gradlew :core:youtube:test -Plive=true --tests '*LiveProviderTest' */
class LiveProviderTest {
    @Test fun songsPageAndTwoPlayableStreams() = runBlocking {
        assumeTrue(System.getProperty("sung.live") == "true")
        YouTubeSource().use { source ->
            val first = source.search("Coldplay", CatalogKind.SONG)
            assertTrue(first.items.isNotEmpty())
            val next = requireNotNull(first.next)
            val second = source.nextPage(next)
            assertTrue(second.items.any { item -> first.items.none { it.id == item.id } })
            val client = OkHttpClient.Builder().callTimeout(30, TimeUnit.SECONDS).build()
            first.items.take(2).forEach { track ->
                val resolved = source.resolve(track)
                assertTrue(resolved.isFresh(System.currentTimeMillis()))
                val request = Request.Builder().url(resolved.url).header("Range", "bytes=0-1023")
                resolved.headers.forEach { (k, v) -> request.header(k, v) }
                client.newCall(request.build()).execute().use { response ->
                    assertTrue("Audio HTTP status ${response.code}", response.isSuccessful)
                    assertTrue(requireNotNull(response.body).source().request(32))
                }
                println("PASS stream: ${resolved.track.title}; ${resolved.mimeType}; ${resolved.bitrate} kbps")
            }
        }
    }

    @Test fun albumsAndPlaylists() = runBlocking {
        assumeTrue(System.getProperty("sung.live") == "true")
        YouTubeSource().use { source ->
            listOf(CatalogKind.ALBUM, CatalogKind.PLAYLIST).forEach { kind ->
                val results = source.search("Coldplay", kind)
                assertTrue("No $kind results", results.items.isNotEmpty())
                val detail = source.open(results.items.first().url)
                assertTrue("No $kind detail tracks", detail.items.isNotEmpty())
                assertNotNull(detail.details)
                println("PASS $kind: ${detail.title}; ${detail.items.size} entries; total=${detail.details?.totalCount}; partial=${detail.partial}")
                detail.next?.let { token ->
                    val next = source.nextPage(token)
                    assertTrue("Empty continuation for $kind", next.items.isNotEmpty())
                    println("PASS $kind collection paging: ${next.items.size} entries")
                }
            }
        }
    }

    @Test fun longPlaylistHasWorkingContinuation() = runBlocking {
        assumeTrue(System.getProperty("sung.live") == "true")
        YouTubeSource().use { source ->
            val results = source.search("NoCopyrightSounds all songs", CatalogKind.PLAYLIST)
            var verified = false
            for (playlist in results.items.take(5)) {
                val detail = source.open(playlist.url)
                val token = detail.next ?: continue
                val next = source.nextPage(token)
                assertTrue("Empty collection continuation", next.items.isNotEmpty())
                assertNotEquals("Repeated continuation", token, next.next)
                assertEquals(detail.details, next.details)
                println("PASS long playlist: ${detail.title}; first=${detail.items.size}; next=${next.items.size}; total=${detail.details?.totalCount}")
                verified = true
                break
            }
            assertTrue("No paginated playlist found in the bounded live sample", verified)
        }
    }

    @Test fun artistCapabilityProbe() = runBlocking {
        assumeTrue(System.getProperty("sung.live") == "true")
        YouTubeSource().use { source ->
            val results = source.search("Coldplay", CatalogKind.ARTIST)
            assertTrue(results.items.isNotEmpty())
            val detail = source.open(results.items.first().url)
            assertTrue(detail.items.isNotEmpty() || detail.notice is CatalogNotice.ArtistBrowseUnavailable)
            println("ARTIST: ${detail.title}; entries=${detail.items.size}; notice=${detail.notice}")
        }
    }

    @Test fun reportedArtistsOfferWorkingSongSearch() = runBlocking {
        assumeTrue(System.getProperty("sung.live") == "true")
        YouTubeSource().use { source ->
            val results = source.search("John Michel", CatalogKind.ARTIST)
            assertTrue(results.items.isNotEmpty())
            results.items.take(2).forEach { artist ->
                val detail = source.open(artist.url)
                val notice = detail.notice
                if (notice is CatalogNotice.ArtistBrowseUnavailable) {
                    assertTrue(source.search(notice.artistName, CatalogKind.SONG).items.isNotEmpty())
                    println("PASS artist fallback search: ${notice.artistName}")
                } else {
                    assertTrue(detail.items.isNotEmpty())
                }
            }
        }
    }
}
