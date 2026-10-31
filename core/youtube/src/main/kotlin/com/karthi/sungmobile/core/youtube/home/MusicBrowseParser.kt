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
import com.karthi.sungmobile.core.model.discovery.HomeChip
import com.karthi.sungmobile.core.model.discovery.HomeFeedPage
import java.net.URI

/** A deliberately small, bounded parser for the public Home and collection renderers we use. */
internal object MusicBrowseParser {
    private const val MAX_ROWS = 5
    private const val MAX_ITEMS = 14
    private const val MAX_PODCAST_ITEMS = 18
    private val videoId = Regex("[A-Za-z0-9_-]{11}")
    private val browseId = Regex("MPRE[A-Za-z0-9_-]{5,190}")
    private val artistBrowseId = Regex("UC[A-Za-z0-9_-]{22}")
    private val podcastBrowseId = Regex("MPSPPL[A-Za-z0-9_-]{5,190}")
    private val playlistId = Regex("[A-Za-z0-9_-]{10,190}")

    data class Rows(val shelves: List<HomeShelf>, val continuation: String?, val visitorData: String?,
        val chips: List<HomeChip> = emptyList())

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
        return Rows(shelves, token, visitorData, parseChips(root))
    }

    fun homePage(json: String): HomeFeedPage {
        val parsed = home(json)
        return HomeFeedPage(parsed.shelves, parsed.chips)
    }

    fun podcastShelves(json: String): List<HomeShelf> {
        val root = JsonParser.parseString(json).obj() ?: return emptyList()
        val carousels = collectRenderers(root, "musicCarouselShelfRenderer")
        val shelves = carousels.mapIndexedNotNull { index, carousel ->
            val header = carousel.at("header", "musicCarouselShelfBasicHeaderRenderer")
            val title = (header.at("title", "runs").arr()?.firstOrNull()?.at("text").string()
                ?: header.at("strapline", "runs").arr()?.firstOrNull()?.at("text").string())
                ?.trim()?.take(100).orEmpty().ifBlank { "Podcasts" }
            val content = carousel.at("contents") ?: carousel
            val shows = collectRenderers(content, "musicTwoRowItemRenderer")
                .mapNotNull(::podcastShowItem)
            val episodes = collectRenderers(content, "musicMultiRowListItemRenderer")
                .mapNotNull(::podcastEpisodeItem) + collectRenderers(content, "musicResponsiveListItemRenderer")
                .mapNotNull(::podcastEpisodeItem)
            val items = (shows + episodes).distinctBy { it.kind to it.id }.take(MAX_PODCAST_ITEMS)
            if (items.isEmpty()) null else HomeShelf("podcast:$index:$title", title, items)
        }
        if (shelves.isNotEmpty()) return shelves
        val fallback = (collectRenderers(root, "musicTwoRowItemRenderer").mapNotNull(::podcastShowItem) +
            collectRenderers(root, "musicMultiRowListItemRenderer").mapNotNull(::podcastEpisodeItem) +
            collectRenderers(root, "musicResponsiveListItemRenderer").mapNotNull(::podcastEpisodeItem))
            .distinctBy { it.kind to it.id }.take(MAX_PODCAST_ITEMS)
        return if (fallback.isEmpty()) emptyList() else listOf(HomeShelf("podcasts", "Podcasts", fallback))
    }

    fun chips(json: String): List<HomeChip> {
        val root = JsonParser.parseString(json).obj() ?: return emptyList()
        return parseChips(root)
    }

    private fun parseChips(root: JsonElement): List<HomeChip> {
        val section = root.at("contents", "singleColumnBrowseResultsRenderer", "tabs")
            .arr()?.firstOrNull()?.at("tabRenderer", "content", "sectionListRenderer")
        val chipRows = section?.at("header", "chipCloudRenderer", "chips")?.arr()?.toList().orEmpty()
        return chipRows.mapNotNull { row ->
            val renderer = row.at("chipCloudChipRenderer") ?: return@mapNotNull null
            val title = (renderer.at("text", "runs")?.arr()?.firstOrNull()?.at("text").string()
                ?: renderer.at("text", "simpleText").string())?.trim()?.take(80)
                ?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            val browse = renderer.at("navigationEndpoint", "browseEndpoint")
            val params = browse.at("params").string()?.takeIf { it.length in 1..4096 } ?: return@mapNotNull null
            val browseId = browse.at("browseId").string()?.takeIf { it.length in 1..200 }
            HomeChip(title, params, browseId)
        }.distinctBy(HomeChip::key).take(24)
    }

    fun collection(json: String, url: String, kind: CatalogKind): CatalogPage {
        val root = JsonParser.parseString(json).obj() ?: error("Invalid collection response")
        val columns = root.at("contents", "twoColumnBrowseResultsRenderer")
        val header = columns.at("tabs").arr()?.firstOrNull()?.at("tabRenderer", "content",
            "sectionListRenderer", "contents").arr()?.firstOrNull()?.at("musicResponsiveHeaderRenderer")
            ?: root.at("header", "musicDetailHeaderRenderer")
            ?: root.at("header", "musicImmersiveHeaderRenderer")
            ?: root.at("header", "musicVisualHeaderRenderer")
            ?: root.at("header", "musicHeaderRenderer")
        val title = (header.at("title", "runs").arr()?.firstOrNull()?.at("text").string()
            ?: header.at("title", "simpleText").string())?.take(150)
            ?: error("Collection title missing")
        val creator = runs(header.at("straplineTextOne", "runs").arr()).ifBlank {
            runs(header.at("subtitle", "runs").arr())
        }
        val artwork = artwork(header)
        val secondary = columns.at("secondaryContents", "sectionListRenderer", "contents").arr()?.toList().orEmpty()
        val shelf = secondary.firstNotNullOfOrNull {
            it.at("musicShelfRenderer") ?: it.at("musicPlaylistShelfRenderer")
        } ?: collectRenderers(root, "musicShelfRenderer").firstOrNull()
            ?: collectRenderers(root, "musicPlaylistShelfRenderer").firstOrNull()
        val songs = shelf?.at("contents").arr()?.toList().orEmpty().take(200).mapNotNull { row ->
            responsiveSong(row.at("musicResponsiveListItemRenderer"), artwork)
        }.toMutableList()
        if (kind == CatalogKind.ARTIST) {
            val artistRows = collectRenderers(root, "musicShelfRenderer")
                .flatMap { it.at("contents").arr()?.toList().orEmpty() }
                .mapNotNull { homeItem(it) }
            songs += artistRows
            if (songs.isEmpty()) {
                songs += collectRenderers(root, "musicCarouselShelfRenderer")
                    .flatMap { it.at("contents").arr()?.toList().orEmpty() }
                    .mapNotNull(::homeItem)
            }
        }
        if (songs.isEmpty()) {
            val podcastRows = collectRenderers(root, "musicMultiRowListItemRenderer")
                .mapNotNull(::podcastEpisodeItem)
            songs += podcastRows
        }
        val uniqueSongs = songs.distinctBy { it.id }
        if (uniqueSongs.isEmpty()) error("Collection has no playable tracks")
        return CatalogPage(title, uniqueSongs, details = CatalogDetails(url, kind, creator, artwork))
    }

    private fun homeItem(row: JsonElement): CatalogItem? {
        val twoRow = row.at("musicTwoRowItemRenderer")
        if (twoRow != null && podcastShowItem(twoRow) != null) return null
        return twoRow?.let(::twoRowItem) ?: responsiveSong(row.at("musicResponsiveListItemRenderer"))
    }

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
            type?.contains("PODCAST_SHOW", ignoreCase = true) == true && id != null && podcastBrowseId.matches(id) ->
                CatalogItem(id, CatalogKind.PLAYLIST, title, subtitle, YouTubeLink.Browse(id).url, art)
            type == "MUSIC_PAGE_TYPE_ALBUM" && id != null && browseId.matches(id) ->
                CatalogItem(id, CatalogKind.ALBUM, title, subtitle, YouTubeLink.Browse(id).url, art)
            type == "MUSIC_PAGE_TYPE_ARTIST" && id != null && artistBrowseId.matches(id) ->
                CatalogItem(id, CatalogKind.ARTIST, title, subtitle, YouTubeLink.Browse(id).url, art)
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

    private fun podcastShowItem(renderer: JsonElement): CatalogItem? {
        val first = renderer.at("title", "runs").arr()?.firstOrNull()
        val title = (first.at("text").string() ?: renderer.at("title", "simpleText").string())
            ?.trim()?.take(180)?.takeIf(String::isNotEmpty) ?: return null
        val browse = collectStrings(renderer, "browseId").firstOrNull { podcastBrowseId.matches(it) } ?: return null
        val subtitle = runs(renderer.at("subtitle", "runs").arr()).take(150)
        return CatalogItem(browse, CatalogKind.PLAYLIST, title, subtitle,
            YouTubeLink.Browse(browse).url, artwork(renderer))
    }

    private fun podcastEpisodeItem(renderer: JsonElement): CatalogItem? {
        val title = (renderer.at("title", "runs").arr()?.firstOrNull()?.at("text").string()
            ?: renderer.at("title", "simpleText").string()
            ?: renderer.at("headline", "runs").arr()?.firstOrNull()?.at("text").string()
            ?: renderer.at("flexColumns").arr()?.firstOrNull()
                ?.at("musicResponsiveListItemFlexColumnRenderer", "text", "runs")
                ?.arr()?.firstOrNull()?.at("text").string())
            ?.trim()?.take(180)?.takeIf(String::isNotEmpty) ?: return null
        val id = (renderer.at("onTap", "watchEndpoint", "videoId").string()
            ?: renderer.at("navigationEndpoint", "watchEndpoint", "videoId").string()
            ?: renderer.at("playlistItemData", "videoId").string()
            ?: renderer.at("thumbnailOverlay", "musicItemThumbnailOverlayRenderer", "content",
                "musicPlayButtonRenderer", "playNavigationEndpoint", "watchEndpoint", "videoId").string()
            ?: collectStrings(renderer, "videoId").firstOrNull())?.takeIf(videoId::matches) ?: return null
        val subtitle = runs(renderer.at("subtitle", "runs").arr()).ifBlank {
            runs(renderer.at("flexColumns").arr()?.toList()?.getOrNull(1)
                ?.at("musicResponsiveListItemFlexColumnRenderer", "text", "runs").arr())
        }
        return song(id, title, subtitle.ifBlank { "Podcast episode" }, artwork(renderer))
    }

    private fun collectRenderers(root: JsonElement, rendererName: String): List<JsonElement> = buildList {
        fun visit(element: JsonElement) {
            when {
                element.isJsonObject -> element.asJsonObject.entrySet().forEach { (key, child) ->
                    if (key == rendererName) add(child)
                    else visit(child)
                }
                element.isJsonArray -> element.asJsonArray.forEach(::visit)
            }
        }
        visit(root)
    }

    private fun collectStrings(root: JsonElement, key: String): List<String> = buildList {
        fun visit(element: JsonElement) {
            when {
                element.isJsonObject -> element.asJsonObject.entrySet().forEach { (name, child) ->
                    if (name == key && child.isJsonPrimitive && child.asJsonPrimitive.isString) add(child.asString)
                    else visit(child)
                }
                element.isJsonArray -> element.asJsonArray.forEach(::visit)
            }
        }
        visit(root)
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
