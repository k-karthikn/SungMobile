package com.karthi.sungmobile.core.model.library

import com.karthi.sungmobile.core.model.*

/** A video identity is independent of the URL used to reach it and of playlist occurrences. */
fun CatalogItem.libraryKey(): String {
    require(kind == CatalogKind.SONG) { "Only songs can be saved" }
    if (source == TrackSource.DEVICE) {
        require(url.length <= 2_000 && url.substringAfter("content://").contains('/')) { "A valid device audio URI is required" }
        return "device:$url"
    }
    val link = YouTubeLink.parse(url) as? YouTubeLink.Song
        ?: throw IllegalArgumentException("A YouTube song is required")
    return "youtube:${link.videoId}"
}
fun CatalogItem.libraryKeyOrNull(): String? = runCatching { libraryKey() }.getOrNull()

sealed interface LibraryCollection {
    data object Likes : LibraryCollection
    data object Recent : LibraryCollection
    data class Playlist(val id: String) : LibraryCollection
}
enum class LibrarySort { DEFAULT, TITLE }
data class LibrarySong(val rowId: String, val track: CatalogItem, val liked: Boolean, val position: Int = 0)
data class LocalPlaylist(val id: String, val name: String, val pinned: Boolean, val songCount: Int)
data class AddResult(val added: Int, val skipped: Int)
fun validatedPlaylistName(name: String): String = name.trim().also {
    require(it.isNotEmpty() && it.length <= 120) { "Use a playlist name of 1–120 characters" }
}
