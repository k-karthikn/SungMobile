package com.karthi.sungmobile.core.model.catalog

import com.karthi.sungmobile.core.model.*
import java.net.URI
import java.net.URLDecoder

/** One validated destination. Shared links are previews until a user explicitly plays them. */
data class OpenMusicLink(val url: String, val label: String, val startMs: Long = 0)
object MusicLinks {
    private val urls = Regex("(?<![a-zA-Z0-9@./_-])(?:https?://[^\\s<>\\\"]+|(?:www\\.)?(?:music\\.)?youtube\\.com/[^\\s<>\\\"]+|youtu\\.be/[^\\s<>\\\"]+)", RegexOption.IGNORE_CASE)
    fun looksLikeLink(text: String) = text.contains("://") || text.startsWith("www.") ||
        text.startsWith("youtu.be/") || text.startsWith("youtube.com/") || text.startsWith("music.youtube.com/")

    fun extract(text: String): List<OpenMusicLink> {
        if (text.length > 16_384) throw SourceException(SourceFailure.INVALID_LINK)
        val found = urls.findAll(text).take(21).toList()
        if (found.size > 20) throw SourceException(SourceFailure.INVALID_LINK)
        val results = found.flatMap { match ->
            val raw = match.value.trimEnd('.', ',', ')', ']', ';')
            val normalized = if (raw.contains("://")) raw else "https://$raw"
            try { parse(normalized) } catch (_: SourceException) { emptyList() }
        }.distinct()
        if (results.isEmpty()) throw SourceException(SourceFailure.INVALID_LINK)
        return results
    }
    fun parse(url: String): List<OpenMusicLink> {
        val link = YouTubeLink.parse(url)
        val uri = URI(url)
        val query = uri.rawQuery.orEmpty().split('&').mapNotNull {
            val pair = it.split('=', limit = 2)
            if (pair.size == 2) pair[0] to URLDecoder.decode(pair[1], "UTF-8") else null
        }.toMap()
        val start = timestamp(query["t"] ?: query["start"] ?: uri.fragment?.removePrefix("t="))
        val primary = OpenMusicLink(link.url, when (link) {
            is YouTubeLink.Song -> "Open song"
            is YouTubeLink.Playlist -> "Open playlist"
            is YouTubeLink.Artist -> "Open artist / channel"
            is YouTubeLink.Browse -> "Open YouTube Music collection"
        }, if (link is YouTubeLink.Song) start else 0)
        val playlist = query["list"]?.takeIf { link is YouTubeLink.Song }?.let { id ->
            runCatching { YouTubeLink.parse("https://www.youtube.com/playlist?list=$id") }.getOrNull()
        }
        return listOfNotNull(primary, playlist?.let { OpenMusicLink(it.url, "Open linked playlist") })
    }
    internal fun timestamp(raw: String?): Long {
        if (raw == null) return 0
        val seconds = raw.toLongOrNull() ?: Regex("(?:(\\d{1,3})h)?(?:(\\d{1,3})m)?(?:(\\d{1,5})s)?")
            .matchEntire(raw)?.groupValues?.let { (it[1].toLongOrNull() ?: 0) * 3600 +
                (it[2].toLongOrNull() ?: 0) * 60 + (it[3].toLongOrNull() ?: 0) } ?: return 0
        return seconds.coerceIn(0, 86_400) * 1000
    }
}
