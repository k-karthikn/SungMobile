package com.karthi.sungmobile.core.youtube.home

import com.google.gson.JsonObject
import com.karthi.sungmobile.core.model.CatalogKind
import com.karthi.sungmobile.core.model.CatalogPage
import com.karthi.sungmobile.core.model.SourceException
import com.karthi.sungmobile.core.model.SourceFailure
import com.karthi.sungmobile.core.model.discovery.HomeFeedSource
import com.karthi.sungmobile.core.model.discovery.HomeCandidateSource
import com.karthi.sungmobile.core.model.discovery.HomeShelf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.Buffer
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Anonymous YouTube Music browse access; never sends the app's local history or likes. */
class MusicBrowseGateway(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS).readTimeout(12, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS).followRedirects(false).build(),
    private val endpoint: HttpUrl = HttpUrl.Builder().scheme("https").host("music.youtube.com")
        .addPathSegments("youtubei/v1/browse").build(),
) : HomeFeedSource, HomeCandidateSource {
    @Volatile private var extraPage: Pair<String, String?>? = null

    override suspend fun homeShelves(): List<HomeShelf> {
        extraPage = null
        val shelves = mutableListOf<HomeShelf>()
        var token: String? = null
        var visitorData: String? = null
        val visited = mutableSetOf<String>()
        repeat(3) { page ->
            val parsed = try { MusicBrowseParser.home(post("FEmusic_home", token, visitorData), page * 5) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                // A later page should not discard usable shelves from the first response.
                if (shelves.isNotEmpty()) return shelves
                throw if (error is SourceException) error else SourceException(SourceFailure.EXTRACTION, error)
            }
            shelves += parsed.shelves.take(5 - shelves.size)
            visitorData = parsed.visitorData ?: visitorData
            extraPage = parsed.continuation?.let { it to visitorData }
            if (shelves.size >= 5) return shelves
            val next = parsed.continuation
            if (next == null || !visited.add(next)) return shelves
            token = next
        }
        return shelves
    }

    override suspend fun extraHomeSongs(): List<com.karthi.sungmobile.core.model.CatalogItem> {
        val (token, visitorData) = extraPage ?: return emptyList()
        val parsed = MusicBrowseParser.home(post("FEmusic_home", token, visitorData), 100)
        return parsed.shelves.flatMap { it.items }.filter { it.kind == CatalogKind.SONG }
            .distinctBy { it.url }.take(30)
    }

    suspend fun openCollection(browseId: String, url: String, kind: CatalogKind): CatalogPage {
        val response = post(browseId)
        return try { MusicBrowseParser.collection(response, url, kind) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { throw SourceException(SourceFailure.EXTRACTION, error) }
    }

    private suspend fun post(browseId: String, continuation: String? = null,
        visitorData: String? = null): String {
        val body = JsonObject().apply {
            add("context", JsonObject().apply {
                add("client", JsonObject().apply {
                    addProperty("clientName", "WEB_REMIX")
                    addProperty("clientVersion", "1.20261001.01.00")
                    addProperty("hl", "en")
                    addProperty("gl", "IN")
                })
            })
            addProperty("browseId", browseId)
        }
        val url = endpoint.newBuilder().addQueryParameter("prettyPrint", "false")
            .apply { if (continuation != null) {
                addEncodedQueryParameter("ctoken", continuation)
                addEncodedQueryParameter("continuation", continuation)
            } }.build()
        val request = Request.Builder().url(url)
            .header("Origin", "https://music.youtube.com")
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/131.0 Mobile Safari/537.36")
            .apply { if (visitorData != null) header("X-Goog-Visitor-Id", visitorData) }
            .post(body.toString().toRequestBody("application/json".toMediaType())).build()
        return suspendCancellableCoroutine { continuationResult ->
            val call = client.newCall(request)
            continuationResult.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuationResult.isActive) continuationResult.resumeWithException(
                        SourceException(SourceFailure.NETWORK, e))
                }
                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!continuationResult.isActive) return
                        if (!response.isSuccessful) {
                            val failure = when (response.code) {
                                401, 403 -> SourceFailure.AUTHENTICATION_REQUIRED
                                429 -> SourceFailure.RATE_LIMITED
                                else -> SourceFailure.NETWORK
                            }
                            continuationResult.resumeWithException(SourceException(failure)); return
                        }
                        try {
                            val source = response.body?.source() ?: throw IOException("Empty browse response")
                            if (response.body!!.contentLength() > MAX_RESPONSE_BYTES) throw IOException("Browse response too large")
                            val buffer = Buffer()
                            while (buffer.size <= MAX_RESPONSE_BYTES) {
                                val count = source.read(buffer, minOf(8192L, MAX_RESPONSE_BYTES + 1 - buffer.size))
                                if (count < 0) break
                            }
                            if (buffer.size > MAX_RESPONSE_BYTES) throw IOException("Browse response too large")
                            continuationResult.resume(buffer.readUtf8())
                        } catch (error: Exception) {
                            if (continuationResult.isActive) continuationResult.resumeWithException(
                                SourceException(SourceFailure.NETWORK, error))
                        }
                    }
                }
            })
        }
    }

    private companion object { const val MAX_RESPONSE_BYTES = 2_097_152L }
}
