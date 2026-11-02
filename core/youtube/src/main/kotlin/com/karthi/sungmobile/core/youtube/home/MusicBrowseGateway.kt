package com.karthi.sungmobile.core.youtube.home

import com.google.gson.JsonObject
import com.karthi.sungmobile.core.model.CatalogKind
import com.karthi.sungmobile.core.model.CatalogPage
import com.karthi.sungmobile.core.model.SourceException
import com.karthi.sungmobile.core.model.SourceFailure
import com.karthi.sungmobile.core.model.discovery.HomeFeedSource
import com.karthi.sungmobile.core.model.discovery.HomeCandidateSource
import com.karthi.sungmobile.core.model.discovery.HomeChip
import com.karthi.sungmobile.core.model.discovery.HomeFeedPage
import com.karthi.sungmobile.core.model.discovery.HomeShelf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    @Volatile private var cachedSearchConfig: SearchConfig? = null
    private val searchConfigMutex = Mutex()

    override suspend fun homeShelves(): List<HomeShelf> = fetchHomeFeed(MAX_LEGACY_HOME_SHELVES).shelves

    override suspend fun homePage(): HomeFeedPage = fetchHomeFeed(MAX_HOME_SHELVES)

    override suspend fun homePage(onUpdate: (HomeFeedPage) -> Unit): HomeFeedPage =
        fetchHomeFeed(MAX_HOME_SHELVES, onUpdate)

    override suspend fun homeChip(chip: HomeChip): HomeFeedPage {
        if (chip.title.contains("podcast", ignoreCase = true)) {
            return try {
                val shelves = MusicBrowseParser.podcastShelves(post("FEmusic_podcasts"))
                if (shelves.isNotEmpty()) HomeFeedPage(shelves)
                else loadHomeChipFallback(chip)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                loadHomeChipFallback(chip)
            }
        }
        return loadHomeChipFallback(chip)
    }

    private suspend fun loadHomeChipFallback(chip: HomeChip): HomeFeedPage {
        val browseId = chip.browseId?.takeIf { it.length in 1..200 } ?: "FEmusic_home"
        val params = chip.params.takeIf { it.length in 1..4096 } ?: return HomeFeedPage()
        return try {
            MusicBrowseParser.homePage(post(browseId, params = params))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            HomeFeedPage()
        }
    }

    private suspend fun fetchHomeFeed(maxShelves: Int, onUpdate: (HomeFeedPage) -> Unit = {}): HomeFeedPage {
        extraPage = null
        val shelves = mutableListOf<HomeShelf>()
        var token: String? = null
        var visitorData: String? = null
        var chips = emptyList<HomeChip>()
        val visited = mutableSetOf<String>()
        repeat(3) { page ->
            val parsed = try { MusicBrowseParser.home(post("FEmusic_home", token, visitorData), page * 5) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                // A later page should not discard usable shelves from the first response.
                if (shelves.isNotEmpty()) return HomeFeedPage(shelves, chips)
                throw if (error is SourceException) error else SourceException(SourceFailure.EXTRACTION, error)
            }
            if (page == 0) chips = parsed.chips
            shelves += parsed.shelves.take(maxShelves - shelves.size)
            onUpdate(HomeFeedPage(shelves.toList(), chips))
            visitorData = parsed.visitorData ?: visitorData
            extraPage = parsed.continuation?.let { it to visitorData }
            if (shelves.size >= maxShelves) return HomeFeedPage(shelves, chips)
            val next = parsed.continuation
            if (next == null || !visited.add(next)) return HomeFeedPage(shelves, chips)
            token = next
        }
        return HomeFeedPage(shelves, chips)
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

    /** One direct YouTube Music search request, used by bulk library matching. */
    suspend fun searchSongsForMatching(query: String): CatalogPage {
        require(query.isNotBlank())
        val config = getSearchConfig()
        val body = JsonObject().apply {
            add("context", JsonObject().apply {
                add("client", JsonObject().apply {
                    addProperty("clientName", "WEB_REMIX")
                    addProperty("clientVersion", config.clientVersion)
                    addProperty("hl", "en")
                    addProperty("gl", "IN")
                    config.visitorData?.let { addProperty("visitorData", it) }
                })
            })
            addProperty("query", query.trim())
            addProperty("params", MUSIC_SONG_SEARCH_PARAMS)
        }
        val url = HttpUrl.Builder().scheme("https").host("music.youtube.com")
            .addPathSegments("youtubei/v1/search").addQueryParameter("key", config.apiKey)
            .addQueryParameter("prettyPrint", "false").build()
        val request = Request.Builder().url(url)
            .header("Origin", "https://music.youtube.com")
            .header("User-Agent", MUSIC_USER_AGENT)
            .header("X-YouTube-Client-Name", WEB_REMIX_CLIENT_ID)
            .header("X-YouTube-Client-Version", config.clientVersion)
            .header("Accept-Language", "en-IN,en;q=0.9")
            .post(body.toString().toRequestBody("application/json".toMediaType())).build()
        val response = execute(request)
        return try {
            MusicBrowseParser.searchSongs(response, query)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            throw SourceException(SourceFailure.EXTRACTION, error)
        }
    }

    private suspend fun getSearchConfig(): SearchConfig {
        cachedSearchConfig?.let { return it }
        return searchConfigMutex.withLock {
            cachedSearchConfig?.let { return@withLock it }
            val request = Request.Builder().url("https://music.youtube.com/")
                .header("User-Agent", MUSIC_USER_AGENT)
                .header("Accept-Language", "en-IN,en;q=0.9")
                .build()
            val html = execute(request)
            val apiKey = findConfig(html, "INNERTUBE_API_KEY")
                ?: throw SourceException(SourceFailure.EXTRACTION)
            val clientVersion = findConfig(html, "INNERTUBE_CONTEXT_CLIENT_VERSION")
                ?: throw SourceException(SourceFailure.EXTRACTION)
            SearchConfig(apiKey, clientVersion, findConfig(html, "VISITOR_DATA")).also {
                cachedSearchConfig = it
            }
        }
    }

    private fun findConfig(html: String, key: String): String? = Regex(
        "[\\\"']${Regex.escape(key)}[\\\"']\\s*:\\s*[\\\"']([^\\\"']{1,500})[\\\"']"
    ).find(html)?.groupValues?.getOrNull(1)

    private suspend fun post(browseId: String, continuation: String? = null,
        visitorData: String? = null, params: String? = null): String {
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
            params?.let { addProperty("params", it) }
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
        return execute(request)
    }

    private suspend fun execute(request: Request): String {
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

    private companion object {
        const val MAX_RESPONSE_BYTES = 2_097_152L
        const val MAX_HOME_SHELVES = 15
        const val MAX_LEGACY_HOME_SHELVES = 5
        const val WEB_REMIX_CLIENT_ID = "67"
        const val MUSIC_SONG_SEARCH_PARAMS = "EgWKAQIIAWoKEAkQBRAKEAMQBA=="
        const val MUSIC_USER_AGENT = "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/131.0 Mobile Safari/537.36"
    }

    private data class SearchConfig(val apiKey: String, val clientVersion: String, val visitorData: String?)
}
