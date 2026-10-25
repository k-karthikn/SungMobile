package com.karthi.sungmobile.core.youtube.home

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.CatalogKind
import com.karthi.sungmobile.core.model.CatalogPage
import com.karthi.sungmobile.core.model.YouTubeLink
import com.karthi.sungmobile.core.model.catalog.CatalogDetails
import com.karthi.sungmobile.core.model.discovery.HomeShelf
import java.net.URI

/** A deliberately small, bounded parser for the public Home and collection renderers we use. */
internal object MusicBrowseParser {
    private const val MAX_ROWS = 5
    private const val MAX_ITEMS = 14
    private val videoId = Regex("[A-Za-z0-9_-]{11}")
    private val browseId = Regex("MPRE[A-Za-z0-9_-]{5,190}")
    private val playlistId = Regex("[A-Za-z0-9_-]{10,190}")

    data class Rows(val shelves: List<HomeShelf>, val continuation: String?, val visitorData: String?)

    fun home(json: String, startIndex: Int = 0): Rows {
        val root = JsonParser.parseString(json).obj() ?: return Rows(emptyList(), null, null)
        val section = root.at("contents", "singleColumnBrowseResultsRenderer", "tabs").arr()?.firstOrNull()
            ?.at("tabRenderer", "content", "sectionListRenderer")
            ?: root.at("continuationContents", "sectionListContinuation")
            ?: return Rows(emptyList(), null, null)
        val shelves = section.at("contents").arr()?.toList().orEmpty().take(MAX_ROWS).mapIndexedNotNull { index, row ->
            val renderer = row.at("musicCarouselShelfRenderer")
                ?: row.at("musicImmersiveCarouselShelfRenderer") ?: return@mapIndexedNotNull null
            val header = renderer.at("header", "musicCarouselShelfBasicHeaderRenderer")
            val title = (header.at("title", "runs").arr()?.firstOrNull()?.at("text").string()
                ?: header.at("strapline", "runs").arr()?.firstOrNull()?.at("text").string())
                ?.trim()?.take(100).orEmpty()
            if (title.isBlank()) return@mapIndexedNotNull null
            val items = renderer.at("contents").arr()?.toList().orEmpty().take(40).mapNotNull(::homeItem)
                .distinctBy { it.kind to it.id }.take(MAX_ITEMS)
            if (items.isEmpty()) null else HomeShelf("provider:${startIndex + index}:$title", title, items)
        }
        val token = section.at("continuations").arr()?.firstOrNull()
            ?.at("nextContinuationData", "continuation").string()?.takeIf { it.length in 1..4096 }
        val visitorData = root.at("responseContext", "visitorData").string()
            ?.takeIf { it.length in 1..200 && Regex("[A-Za-z0-9_%=-]+").matches(it) }
        return Rows(shelves, token, visitorData)
    }

    fun collection(json: String, url: String, kind: CatalogKind): CatalogPage {
        val root = JsonParser.parseString(json).obj() ?: error("Invalid collection response")
        val columns = root.at("contents", "twoColumnBrowseResultsRenderer")
        val header = columns.at("tabs").arr()?.firstOrNull()?.at("tabRenderer", "content",
            "sectionListRenderer", "contents").arr()?.firstOrNull()?.at("musicResponsiveHeaderRenderer")
            ?: root.at("header", "musicDetailHeaderRenderer")
        val title = header.at("title", "runs").arr()?.firstOrNull()?.at("text").string()?.take(150)
            ?: error("Collection title missing")
        val creator = runs(header.at("straplineTextOne", "runs").arr()).ifBlank {
            runs(header.at("subtitle", "runs").arr())
        }
        val artwork = artwork(header)
        val secondary = columns.at("secondaryContents", "sectionListRenderer", "contents").arr()?.toList().orEmpty()
        val shelf = secondary.firstNotNullOfOrNull {
            it.at("musicShelfRenderer") ?: it.at("musicPlaylistShelfRenderer")
        } ?: error("Collection track shelf missing")
        val songs = shelf.at("contents").arr()?.toList().orEmpty().take(200).mapNotNull { row ->
            responsiveSong(row.at("musicResponsiveListItemRenderer"), artwork)
        }.distinctBy { it.id }
        if (songs.isEmpty()) error("Collection has no playable tracks")
        return CatalogPage(title, songs, details = CatalogDetails(url, kind, creator, artwork))
    }

    private fun homeItem(row: JsonElement): CatalogItem? =
        row.at("musicTwoRowItemRenderer")?.let(::twoRowItem)
            ?: responsiveSong(row.at("musicResponsiveListItemRenderer"))

    private fun twoRowItem(renderer: JsonElement): CatalogItem? {
        val first = renderer.at("title", "runs").arr()?.firstOrNull()
        val title = first.at("text").string()?.trim()?.take(180)?.takeIf(String::isNotEmpty) ?: return null
        val endpoint = first.at("navigationEndpoint") ?: renderer.at("navigationEndpoint")
        val type = endpoint.at("browseEndpoint", "browseEndpointContextSupportedConfigs",
            "browseEndpointContextMusicConfig", "pageType").string()
        val id = endpoint.at("browseEndpoint", "browseId").string()
        val subtitle = runs(renderer.at("subtitle", "runs").arr()).take(150)
        val art = artwork(renderer)
        val songId = endpoint.at("watchEndpoint", "videoId").string()
        return when {
            songId != null && videoId.matches(songId) -> song(songId, title, songArtists(subtitle), art)
            type == "MUSIC_PAGE_TYPE_ALBUM" && id != null && browseId.matches(id) ->
                CatalogItem(id, CatalogKind.ALBUM, title, subtitle, YouTubeLink.Browse(id).url, art)
            type == "MUSIC_PAGE_TYPE_PLAYLIST" && id?.startsWith("VL") == true &&
                playlistId.matches(id.removePrefix("VL")) ->
                CatalogItem(id, CatalogKind.PLAYLIST, title, subtitle, "https://music.youtube.com/browse/$id", art)
            else -> null
        }
    }

    private fun responsiveSong(renderer: JsonElement?, fallbackArt: String? = null): CatalogItem? {
        val columns = renderer.at("flexColumns").arr() ?: return null
        val first = columns.firstOrNull()?.at("musicResponsiveListItemFlexColumnRenderer", "text", "runs")
            .arr()?.firstOrNull()
        val title = first.at("text").string()?.trim()?.take(180)?.takeIf(String::isNotEmpty) ?: return null
        val id = first.at("navigationEndpoint", "watchEndpoint", "videoId").string()
            ?: renderer.at("overlay", "musicItemThumbnailOverlayRenderer", "content", "musicPlayButtonRenderer",
                "playNavigationEndpoint", "watchEndpoint", "videoId").string()
        if (id == null || !videoId.matches(id)) return null
        val subtitle = songArtists(runs(columns.toList().getOrNull(1)?.at("musicResponsiveListItemFlexColumnRenderer", "text", "runs")
            .arr()))
        return song(id, title, subtitle, artwork(renderer) ?: fallbackArt)
    }

    private fun song(id: String, title: String, subtitle: String, art: String?) =
        CatalogItem(id, CatalogKind.SONG, title, subtitle, YouTubeLink.Song(id).url, art)

    private fun artwork(renderer: JsonElement?): String? {
        val thumbnails = renderer.at("thumbnailRenderer", "musicThumbnailRenderer", "thumbnail", "thumbnails").arr()
            ?: renderer.at("thumbnail", "musicThumbnailRenderer", "thumbnail", "thumbnails").arr()
            ?: renderer.at("thumbnail", "croppedSquareThumbnailRenderer", "thumbnail", "thumbnails").arr()
        val url = thumbnails?.lastOrNull()?.at("url").string()?.take(1000)?.takeIf {
            it.startsWith("https://")
        } ?: return null
        val host = runCatching { URI(url).host.orEmpty() }.getOrDefault("")
        return if ((host == "ggpht.com" || host.endsWith(".googleusercontent.com")) &&
            Regex("=w\\d+-h\\d+[^?]*$").containsMatchIn(url))
            url.replace(Regex("=w\\d+-h\\d+[^?]*$"), "=w544-h544-l90-rj")
        else url
    }

    private fun runs(items: JsonArray?): String = items?.toList().orEmpty().take(24)
        .mapNotNull { it.at("text").string() }.joinToString("")
        .replace(Regex("\\s+"), " ").trim().take(150)

    private fun songArtists(subtitle: String): String = subtitle.substringBefore('•').trim().trimEnd(',', '·').trim()

    private fun JsonElement?.obj(): JsonObject? = this?.takeIf { it.isJsonObject }?.asJsonObject
    private fun JsonElement?.arr(): JsonArray? = this?.takeIf { it.isJsonArray }?.asJsonArray
    private fun JsonElement?.at(vararg keys: String): JsonElement? = keys.fold(this) { node, key -> node.obj()?.get(key) }
    private fun JsonElement?.string(): String? = this?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }
        ?.asString
}
