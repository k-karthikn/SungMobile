package com.karthi.sungmobile.core.youtube.lyrics

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.lyrics.LyricsDocument
import com.karthi.sungmobile.core.model.lyrics.LyricsOrigin
import com.karthi.sungmobile.core.model.lyrics.lyricsVideoId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer
import java.io.IOException
import kotlin.math.max

/** Word-timed Apple TTML from LastWave's BetterLyrics provider path; no credentials are sent. */
internal class BetterLyricsGateway(private val client: OkHttpClient) {
    suspend fun fetch(track: CatalogItem): LyricsDocument? = withContext(Dispatchers.IO) {
        if (track.lyricsVideoId() == null || track.title.isBlank() || track.subtitle.isBlank()) return@withContext null
        val artist = track.subtitle.substringBefore(" • ").trim()
        val durationSeconds = track.durationMs?.takeIf { it > 0 }?.div(1_000)?.toInt()
        val cleanedTitle = cleanTitle(track.title)
        val cleanedArtist = artist.removeSuffix(" - Topic").trim().ifBlank { artist }
        val candidates = listOf(track.title.trim() to artist,
            cleanedTitle to cleanedArtist).distinct()
        val endpoints = listOf("getLyrics", "ttml/getLyrics", "qq/getLyrics")
        for ((title, queryArtist) in candidates) {
            for (endpoint in endpoints) {
                val url = "https://lyrics-api.boidu.dev/$endpoint".toHttpUrlOrNull()?.newBuilder()
                    ?.addQueryParameter("s", title)
                    ?.addQueryParameter("a", queryArtist)
                    ?.apply { durationSeconds?.let { addQueryParameter("d", it.toString()) } }
                    ?.build() ?: continue
                try {
                    val request = Request.Builder().url(url)
                        .header("User-Agent", USER_AGENT)
                        .header("Accept", "application/json, text/xml, */*")
                        .get().build()
                    val body = client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) null else {
                            val responseBody = response.body
                            if (responseBody == null || responseBody.contentLength() > MAX_RESPONSE_BYTES) null else {
                                val buffer = Buffer()
                                val source = responseBody.source()
                                while (buffer.size <= MAX_RESPONSE_BYTES) {
                                    val count = source.read(buffer, minOf(8_192L, MAX_RESPONSE_BYTES + 1 - buffer.size))
                                    if (count < 0) break
                                }
                                if (buffer.size > MAX_RESPONSE_BYTES) null else buffer.readUtf8()
                            }
                        }
                    } ?: continue
                    val ttml = findTtml(body) ?: continue
                    val lines = TtmlLyricsParser.parse(ttml)
                    if (lines.any { it.syllables.isNotEmpty() } && plausibleDuration(lines, durationSeconds)) {
                        return@withContext LyricsDocument(LyricsOrigin.BETTER_LYRICS,
                            "BetterLyrics · Apple Music timing", lines = lines)
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: IOException) {
                    // Try the provider's alternate TTML route.
                } catch (_: Exception) {
                    // Unavailable provider responses fall through to existing lyric sources.
                }
            }
        }
        null
    }

    private fun plausibleDuration(lines: List<com.karthi.sungmobile.core.model.lyrics.LyricLine>, durationSeconds: Int?): Boolean {
        if (durationSeconds == null || durationSeconds <= 0 || lines.size < 3) return true
        val expectedMs = durationSeconds * 1_000L
        if (expectedMs < 60_000L) return true
        val endMs = lines.maxOfOrNull { line ->
            val fragmentEnd = line.syllables.maxOfOrNull { it.startMs + it.durationMs } ?: 0L
            max(line.startMs, fragmentEnd)
        } ?: return true
        return endMs <= expectedMs + 45_000L &&
            !(endMs < expectedMs * 0.5 && expectedMs - endMs > 90_000L)
    }

    private fun cleanTitle(raw: String): String {
        var title = raw
        TITLE_CREDITS.forEach { title = it.replace(title, " ") }
        return title.replace(WHITESPACE, " ").trim().trimEnd(',', '-', '–', '—').trim()
            .ifBlank { raw.trim() }
    }

    private fun findTtml(body: String): String? {
        val value = body.trim().removePrefix("\uFEFF")
        if (value.startsWith("<")) return value.takeIf { HAS_PARAGRAPH.containsMatchIn(it) }
        val root = runCatching { JsonParser.parseString(value) }.getOrNull() ?: return null
        return findTtmlValue(root)
    }

    private fun findTtmlValue(element: JsonElement): String? = when {
        element.isJsonPrimitive && element.asJsonPrimitive.isString -> element.asString
            .takeIf { HAS_PARAGRAPH.containsMatchIn(it) }
        element.isJsonArray -> element.asJsonArray.asSequence().mapNotNull(::findTtmlValue).firstOrNull()
        element.isJsonObject -> {
            val obj = element.asJsonObject
            listOf("ttml", "ttmlContent", "lyrics", "content", "text", "data", "result")
                .firstNotNullOfOrNull { obj.get(it)?.let(::findTtmlValue) }
                ?: obj.entrySet().asSequence().mapNotNull { findTtmlValue(it.value) }.firstOrNull()
        }
        else -> null
    }

    private companion object {
        private const val MAX_RESPONSE_BYTES = 1_048_576L
        private const val USER_AGENT = "SungMobile/0.8.0 (lyrics lookup)"
        private val HAS_PARAGRAPH = Regex("<(?:[A-Za-z0-9_-]+:)?p\\b", RegexOption.IGNORE_CASE)
        private val WHITESPACE = Regex("\\s+")
        private val TITLE_CREDITS = listOf(
            Regex("\\s*[(\\[]\\s*(feat|ft|featuring|with)\\b[^)\\]]*[)\\]]", RegexOption.IGNORE_CASE),
            Regex("\\s+(feat|ft|featuring)\\.?\\s+.*$", RegexOption.IGNORE_CASE),
            Regex("\\s*[(\\[]\\s*(official\\s*)?(music\\s*)?(video|audio|visuali[sz]er|lyrics?\\s*video|lyrics?|m/?v|hd|hq|4k|full\\s*song)\\s*[)\\]]", RegexOption.IGNORE_CASE),
            Regex("\\s*[(\\[]\\s*official\\s*[)\\]]", RegexOption.IGNORE_CASE),
        )
    }
}
