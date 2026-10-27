package com.karthi.sungmobile.core.youtube.home

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test

class MusicBrowseGatewayTest {
    @Test fun `continuation sends anonymous visitor id and preserves provider order`() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody(response("First row", "next-token", "testVisitor")))
            server.enqueue(MockResponse().setBody("""{
              "continuationContents":{"sectionListContinuation":{"contents":[${row("Second row")}]} }
            }"""))
            val shelves = MusicBrowseGateway(endpoint = server.url("/youtubei/v1/browse")).homeShelves()
            assertEquals(listOf("First row", "Second row"), shelves.map { it.title })
            val first = server.takeRequest()
            val second = server.takeRequest()
            assertTrue(first.body.readUtf8().contains("FEmusic_home"))
            assertNull(first.getHeader("X-Goog-Visitor-Id"))
            assertEquals("testVisitor", second.getHeader("X-Goog-Visitor-Id"))
            assertEquals("next-token", second.requestUrl?.queryParameter("continuation"))
        }
    }

    @Test fun `failed continuation retains first usable shelf`() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody(response("First row", "next-token", "testVisitor")))
            server.enqueue(MockResponse().setResponseCode(500))
            val shelves = MusicBrowseGateway(endpoint = server.url("/youtubei/v1/browse")).homeShelves()
            assertEquals(listOf("First row"), shelves.map { it.title })
        }
    }

    @Test fun `extra songs use only the general Home continuation`() = runBlocking {
        MockWebServer().use { server ->
            val rows = (1..5).joinToString(",") { row("Row $it") }
            server.enqueue(MockResponse().setBody("""{
              "responseContext":{"visitorData":"testVisitor"},
              "contents":{"singleColumnBrowseResultsRenderer":{"tabs":[{"tabRenderer":{"content":{
                "sectionListRenderer":{"contents":[$rows],"continuations":[{"nextContinuationData":{"continuation":"more-token"}}]}
              }}}]}}
            }"""))
            server.enqueue(MockResponse().setBody("""{
              "continuationContents":{"sectionListContinuation":{"contents":[${row("Extra row", "BBBBBBBBBBB")}]}}
            }"""))
            val gateway = MusicBrowseGateway(endpoint = server.url("/youtubei/v1/browse"))
            assertEquals(5, gateway.homeShelves().size)
            assertEquals(1, gateway.extraHomeSongs().size)
            server.takeRequest()
            val extra = server.takeRequest()
            assertEquals("more-token", extra.requestUrl?.queryParameter("continuation"))
            assertEquals("testVisitor", extra.getHeader("X-Goog-Visitor-Id"))
            assertFalse(extra.body.readUtf8().contains("BBBBBBBBBBB"))
        }
    }

    private fun response(title: String, token: String? = null, visitor: String? = null): String {
        val continuation = token?.let { ",\"continuations\":[{\"nextContinuationData\":{\"continuation\":\"$it\"}}]" }.orEmpty()
        val context = visitor?.let { "\"responseContext\":{\"visitorData\":\"$it\"}," }.orEmpty()
        return """{$context"contents":{"singleColumnBrowseResultsRenderer":{"tabs":[{"tabRenderer":{"content":{
          "sectionListRenderer":{"contents":[${row(title)}]$continuation}
        }}}]}}}"""
    }

    private fun row(title: String, videoId: String = "AAAAAAAAAAA") = """{"musicCarouselShelfRenderer":{
      "header":{"musicCarouselShelfBasicHeaderRenderer":{"title":{"runs":[{"text":"$title"}]}}},
      "contents":[{"musicTwoRowItemRenderer":{"title":{"runs":[{"text":"A song",
        "navigationEndpoint":{"watchEndpoint":{"videoId":"$videoId"}}}]}}}]
    }}"""
}
