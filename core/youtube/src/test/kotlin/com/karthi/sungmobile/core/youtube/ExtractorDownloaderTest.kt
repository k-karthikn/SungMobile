package com.karthi.sungmobile.core.youtube

import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.Assert.*
import org.junit.Test
import org.schabi.newpipe.extractor.downloader.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class ExtractorDownloaderTest {
    @Test fun suppliedHeadersOverrideDefaultsAndBodyIsPreserved() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("result"))
            val request = Request.newBuilder().post(server.url("/").toString(), "payload".toByteArray())
                .setHeader("User-Agent", "extractor-client").build()
            val result = ExtractorDownloader().execute(request)
            assertEquals("result", result.responseBody())
            val received = server.takeRequest()
            assertEquals(listOf("extractor-client"), received.headers.values("User-Agent"))
            assertEquals("payload", received.body.readUtf8())
        }
    }

    @Test fun rateLimitIsTypedAndOversizedResponseIsRejected() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(429))
            val request = Request.newBuilder().get(server.url("/").toString()).build()
            assertEquals(429, assertThrows(HttpStatusException::class.java) {
                ExtractorDownloader().execute(request)
            }.status)
            server.enqueue(MockResponse().setChunkedBody("larger than limit", 4))
            assertThrows(IOException::class.java) { ExtractorDownloader(maxBodyBytes = 8).execute(request) }
        }
    }

    @Test fun cancellationClosesInFlightHttpCall() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
            val client = OkHttpClient()
            val downloader = ExtractorDownloader(client)
            ExtractorRunner().use { runner ->
                val work = launch(Dispatchers.Default) {
                    runner.run { downloader.execute(Request.newBuilder().get(server.url("/").toString()).build()) }
                }
                assertNotNull(withContext(Dispatchers.IO) { server.takeRequest(3, TimeUnit.SECONDS) })
                work.cancelAndJoin()
                withTimeout(3_000) {
                    while (client.dispatcher.runningCallsCount() != 0) delay(10)
                }
                assertTrue(work.isCancelled)
            }
        }
    }
}
