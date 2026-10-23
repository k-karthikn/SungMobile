package com.karthi.sungmobile.core.youtube.lyrics

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.karthi.sungmobile.core.model.lyrics.*
import java.text.Normalizer
import kotlin.math.abs

internal fun JsonElement?.obj(): JsonObject? = if (this?.isJsonObject == true) this.asJsonObject else null
internal fun JsonElement?.str(): String? = if (this?.isJsonPrimitive == true) runCatching { this.asString }.getOrNull() else null
internal fun JsonElement?.long(): Long? = str()?.toLongOrNull()
internal fun JsonElement?.array() = if (this?.isJsonArray == true) asJsonArray.toList() else emptyList()
internal fun JsonObject?.at(vararg path: String): JsonElement? {
    var node: JsonElement? = this
    for (part in path) node = node.obj()?.get(part)
    return node
}

internal fun parseYouTubeNext(body: String): String? {
    val root = JsonParser.parseString(body).obj()
    val tabs = root.at("contents", "singleColumnMusicWatchNextResultsRenderer", "tabbedRenderer", "watchNextTabbedResultsRenderer", "tabs").array()
    return tabs.firstNotNullOfOrNull { tab ->
        val endpoint = tab.obj().at("tabRenderer", "endpoint", "browseEndpoint").obj()
        val pageType = endpoint.at("browseEndpointContextSupportedConfigs", "browseEndpointContextMusicConfig", "pageType").str()
        endpoint?.get("browseId").str()?.takeIf { it.startsWith("MPLYt") && (pageType == "MUSIC_PAGE_TYPE_TRACK_LYRICS" || tab.obj().at("tabRenderer", "title").str() == "Lyrics") }
    }
}

internal fun parseYouTubeLyrics(body: String): LyricsDocument? {
    val root = JsonParser.parseString(body).obj()
    val timedData = root.at("contents", "elementRenderer", "newElement", "type", "componentType", "model", "timedLyricsModel", "lyricsData").obj()
    val timed = timedData?.get("timedLyricsData").array().take(4_000)
    val rows = timed.mapNotNull { row ->
        val start = row.obj().at("cueRange", "startTimeMilliseconds").long()
        val text = row.obj()?.get("lyricLine").str()
        if (text.isNullOrBlank()) null else start to text
    }
    if (rows.isNotEmpty()) {
        val attribution = timedData?.get("sourceMessage").str() ?: "YouTube Music"
        val lines = rows.mapNotNull { (start, text) ->
            if (start != null && start >= 0) LyricLine(start, text) else null
        }
        if (lines.size == rows.size) return LyricsDocument(LyricsOrigin.YOUTUBE,
            attribution, lines = lines.sortedBy { it.startMs })
        val plain = rows.joinToString("\n") { it.second }
        if (plain.length <= 256_000) return LyricsDocument(LyricsOrigin.YOUTUBE,
            attribution, plainText = plain)
    }
    val sections = root.at("contents", "sectionListRenderer", "contents").array()
    for (section in sections) {
        val shelf = section.obj()?.get("musicDescriptionShelfRenderer").obj() ?: continue
        val text = shelf.at("description", "runs").array().joinToString("") { it.obj()?.get("text").str().orEmpty() }.trim()
        if (text.isNotEmpty() && text.length <= 256_000) {
            val footer = shelf.at("footer", "runs").array().joinToString("") { it.obj()?.get("text").str().orEmpty() }.ifBlank { "YouTube Music" }
            return LyricsDocument(LyricsOrigin.YOUTUBE, footer, plainText = text)
        }
    }
    return null
}

internal fun parseLrc(text: String): List<LyricLine> {
    if (text.length > 256_000) return emptyList()
    val stamp = Regex("\\[(\\d{1,3}):(\\d{2})(?:\\.(\\d{1,3}))?]")
    return text.lineSequence().flatMap { raw ->
        val lyric = raw.replace(stamp, "").trim()
        if (lyric.isBlank()) emptySequence() else stamp.findAll(raw).mapNotNull { match ->
            val minutes = match.groupValues[1].toLongOrNull() ?: return@mapNotNull null
            val seconds = match.groupValues[2].toLongOrNull() ?: return@mapNotNull null
            if (seconds > 59) return@mapNotNull null
            val fraction = match.groupValues[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0
            LyricLine((minutes * 60 + seconds) * 1_000 + fraction, lyric)
        }
    }.take(4_000).sortedBy { it.startMs }.toList()
}

private fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFKC)
    .lowercase().replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim().replace(Regex("\\s+"), " ")

internal fun parseLrclib(body: String, title: String, artist: String, durationMs: Long): LyricsDocument? {
    val data = JsonParser.parseString(body).obj() ?: return null
    if (normalize(data.get("trackName").str().orEmpty()) != normalize(title) ||
        normalize(data.get("artistName").str().orEmpty()) != normalize(artist)) return null
    val actualSeconds = data.get("duration").str()?.toDoubleOrNull() ?: return null
    if (durationMs <= 0 || abs(actualSeconds * 1_000 - durationMs) > 2_000) return null
    val synced = data.get("syncedLyrics").str()?.takeIf { it.length <= 256_000 }
    val lines = synced?.let(::parseLrc).orEmpty()
    if (lines.size >= 3 && lines.sumOf { it.text.length } >= 40)
        return LyricsDocument(LyricsOrigin.LRCLIB, "LRCLIB", lines = lines)
    val plain = data.get("plainLyrics").str()?.takeIf { it.length in 40..256_000 }
    return plain?.let { LyricsDocument(LyricsOrigin.LRCLIB, "LRCLIB", plainText = it) }
}
