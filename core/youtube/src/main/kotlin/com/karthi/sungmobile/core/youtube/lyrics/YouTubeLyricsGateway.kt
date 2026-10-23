package com.karthi.sungmobile.core.youtube.lyrics

import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.lyrics.LyricsDocument
import com.karthi.sungmobile.core.model.lyrics.LyricsGateway
import com.karthi.sungmobile.core.model.lyrics.lyricsVideoId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Only lyric metadata is fetched here; audio resolution stays in StreamExtractor. */
class YouTubeLyricsGateway(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS).readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS).followRedirects(false).build(),
) : LyricsGateway {
    override suspend fun youtube(track: CatalogItem): LyricsDocument? {
        val videoId = track.lyricsVideoId() ?: return null
        val context = """{"client":{"clientName":"WEB_REMIX","clientVersion":"1.20261001.01.00","hl":"en","gl":"IN"}}"""
        val next = post("https://music.youtube.com/youtubei/v1/next?prettyPrint=false",
            """{"context":$context,"videoId":"$videoId","playlistId":"RDAMVM$videoId","enablePersistentPlaylistPanel":true,"isAudioOnly":true}""")
        val browseId = runCatching { parseYouTubeNext(next) }.getOrNull() ?: return null
        val mobileContext = """{"client":{"clientName":"ANDROID_MUSIC","clientVersion":"7.21.50","hl":"en","gl":"IN"}}"""
        val mobile = runCatching { post("https://music.youtube.com/youtubei/v1/browse?prettyPrint=false",
            """{"context":$mobileContext,"browseId":"$browseId"}""") }.getOrNull()
        val timed = mobile?.let { runCatching { parseYouTubeLyrics(it) }.getOrNull() }
        if (timed?.synchronized == true) return timed
        val browse = try {
            post("https://music.youtube.com/youtubei/v1/browse?prettyPrint=false",
                """{"context":$context,"browseId":"$browseId"}""")
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { return timed }
        return runCatching { parseYouTubeLyrics(browse) }.getOrNull() ?: timed
    }

    override suspend fun lrclib(track: CatalogItem): LyricsDocument? {
        if (track.lyricsVideoId() == null) return null
        if (System.currentTimeMillis() < lrclibRetryAfterMillis) throw IOException("LRCLIB rate limited")
        val artist = track.subtitle.substringBefore(" • ").trim()
        val duration = track.durationMs ?: return null
        if (track.title.isBlank() || artist.isBlank() || duration <= 0) return null
        val url = HttpUrl.Builder().scheme("https").host("lrclib.net")
            .addPathSegments("api/get")
            .addQueryParameter("track_name", track.title)
            .addQueryParameter("artist_name", artist)
            .addQueryParameter("duration", (duration / 1_000).toString()).build()
        val response = request(Request.Builder().url(url).get().header("User-Agent", "SungMobile/0.8.0").build())
            ?: return null
        return runCatching { parseLrclib(response, track.title, artist, duration) }.getOrNull()
    }

    private suspend fun post(url: String, json: String): String = request(Request.Builder().url(url)
        .header("Origin", "https://music.youtube.com")
        .header("User-Agent", "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/131.0 Mobile Safari/537.36")
        .post(json.toRequestBody("application/json".toMediaType())).build()) ?: ""

    private suspend fun request(request: Request): String? = suspendCancellableCoroutine { continuation ->
        val call = client.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, error: IOException) {
                if (continuation.isActive) continuation.resumeWithException(error)
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!continuation.isActive) return
                    if (response.code == 404) { continuation.resume(null); return }
                    if (response.code == 429 && request.url.host == "lrclib.net")
                        lrclibRetryAfterMillis = System.currentTimeMillis() + 60_000
                    if (!response.isSuccessful) {
                        continuation.resumeWithException(IOException("Lyrics HTTP ${response.code}")); return
                    }
                    try {
                        val body = response.body ?: throw IOException("Empty lyrics response")
                        if (body.contentLength() > MAX_BYTES) throw IOException("Lyrics response too large")
                        val source = body.source()
                        val buffer = Buffer()
                        while (buffer.size <= MAX_BYTES) {
                            val count = source.read(buffer, minOf(8_192L, MAX_BYTES + 1 - buffer.size))
                            if (count < 0) break
                        }
                        if (buffer.size > MAX_BYTES) throw IOException("Lyrics response too large")
                        val bytes = buffer.readByteArray()
                        continuation.resume(bytes.toString(Charsets.UTF_8))
                    } catch (error: Exception) {
                        if (continuation.isActive) continuation.resumeWithException(error)
                    }
                }
            }
        })
    }

    private companion object {
        const val MAX_BYTES = 1_048_576L
        @Volatile var lrclibRetryAfterMillis = 0L
    }
}
