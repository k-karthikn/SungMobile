package com.karthi.sungmobile.core.data.spotify

import com.karthi.sungmobile.core.model.spotify.SpotifyLibraryPlaylist
import com.karthi.sungmobile.core.model.spotify.SpotifyLibraryTrack
import org.json.JSONArray
import org.json.JSONObject

/** Converts unstable Spotify Pathfinder payloads into SungMobile's small account-library models. */
internal object SpotifyLibraryResponseParser {
    fun playlists(body: String): List<SpotifyLibraryPlaylist> {
        val items = JSONObject(body).optJSONObject("data")?.optJSONObject("me")
            ?.optJSONObject("libraryV3")?.optJSONArray("items") ?: JSONArray()
        return buildList {
            for (index in 0 until items.length()) {
                val wrapper = items.optJSONObject(index)?.optJSONObject("item") ?: continue
                if (wrapper.optString("__typename") != "PlaylistResponseWrapper") continue
                val data = wrapper.optJSONObject("data") ?: continue
                val id = data.optString("uri").removePrefix("spotify:playlist:")
                val name = data.optString("name").takeIf(String::isNotBlank)
                if (id.isBlank() || name == null) continue
                add(SpotifyLibraryPlaylist(
                    id = id,
                    name = name,
                    description = data.optStringOrNull("description"),
                    artworkUrl = data.optJSONObject("images")?.optJSONArray("items")
                        ?.optJSONObject(0)?.optJSONObject("sources")?.optJSONArray("items")
                        ?.optJSONObject(0)?.optStringOrNull("url")
                        ?: data.optJSONObject("images")?.optJSONArray("items")
                            ?.optJSONObject(0)?.optJSONArray("sources")?.optJSONObject(0)?.optStringOrNull("url"),
                    trackCount = data.optJSONObject("content")?.optIntOrNull("totalCount")
                        ?: data.optJSONObject("tracks")?.optIntOrNull("total") ?: 0,
                ))
            }
        }
    }

    fun playlistPage(body: String): SpotifyPlaylistPage? {
        val playlist = JSONObject(body).optJSONObject("data")?.optJSONObject("playlistV2") ?: return null
        val content = playlist.optJSONObject("content") ?: return null
        val items = content.optJSONArray("items") ?: JSONArray()
        val tracks = buildList {
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                val track = item.optJSONObject("itemV2")?.optJSONObject("data")
                    ?: item.optJSONObject("track") ?: continue
                toTrack(track)?.let(::add)
            }
        }
        val title = playlist.optString("name").takeIf(String::isNotBlank) ?: "Spotify Playlist"
        val artwork = playlist.optJSONObject("images")?.optJSONArray("items")?.optJSONObject(0)
            ?.optJSONArray("sources")?.optJSONObject(0)?.optStringOrNull("url")
            ?: playlist.optJSONObject("images")?.optJSONArray("items")?.optJSONObject(0)
                ?.optJSONObject("sources")?.optJSONArray("items")?.optJSONObject(0)?.optStringOrNull("url")
        return SpotifyPlaylistPage(title, content.optIntOrNull("totalCount") ?: 0, tracks, artwork, items.length())
    }

    fun likedPage(body: String): SpotifyLikedPage? {
        val tracksObject = JSONObject(body).optJSONObject("data")?.optJSONObject("me")
            ?.optJSONObject("library")?.optJSONObject("tracks") ?: return null
        val items = tracksObject.optJSONArray("items") ?: JSONArray()
        val tracks = buildList {
            for (index in 0 until items.length()) {
                val wrapper = items.optJSONObject(index) ?: continue
                val track = wrapper.optJSONObject("track")?.optJSONObject("data")
                    ?: wrapper.optJSONObject("track") ?: continue
                toTrack(track)?.let(::add)
            }
        }
        return SpotifyLikedPage(tracksObject.optIntOrNull("totalCount") ?: 0, tracks, items.length())
    }

    fun libraryTotalCount(body: String): Int? = JSONObject(body).optJSONObject("data")
        ?.optJSONObject("me")?.optJSONObject("libraryV3")?.optIntOrNull("totalCount")

    fun libraryItemCount(body: String): Int = JSONObject(body).optJSONObject("data")
        ?.optJSONObject("me")?.optJSONObject("libraryV3")?.optJSONArray("items")?.length() ?: 0

    private fun toTrack(data: JSONObject): SpotifyLibraryTrack? {
        val title = data.optString("name").trim().takeIf(String::isNotBlank) ?: return null
        val artists = data.optJSONObject("artists")?.optJSONArray("items")
        val artist = buildList {
            for (index in 0 until (artists?.length() ?: 0)) {
                val item = artists?.optJSONObject(index) ?: continue
                (item.optJSONObject("profile")?.optStringOrNull("name") ?: item.optStringOrNull("name"))
                    ?.trim()?.takeIf(String::isNotBlank)?.let(::add)
            }
        }.distinct().joinToString(", ")
        val album = data.optJSONObject("albumOfTrack")?.optStringOrNull("name")
            ?: data.optJSONObject("album")?.optStringOrNull("name")
        val images = data.optJSONObject("albumOfTrack")?.optJSONObject("coverArt")?.optJSONArray("sources")
            ?: data.optJSONObject("album")?.optJSONObject("images")?.optJSONArray("items")
        val image = images?.optJSONObject(0)?.optStringOrNull("url")
        val duration = data.optJSONObject("duration")?.optLongOrNull("totalMilliseconds")
            ?: data.optLongOrNull("duration_ms")
        return SpotifyLibraryTrack(title, artist, album, image, duration)
    }

    private fun JSONObject.optStringOrNull(key: String): String? =
        optString(key).takeIf { it.isNotBlank() && it != "null" }

    private fun JSONObject.optIntOrNull(key: String): Int? =
        if (has(key) && !isNull(key)) optInt(key) else null

    private fun JSONObject.optLongOrNull(key: String): Long? =
        if (has(key) && !isNull(key)) optLong(key) else null
}

internal data class SpotifyPlaylistPage(
    val title: String,
    val totalCount: Int,
    val tracks: List<SpotifyLibraryTrack>,
    val artworkUrl: String?,
    val itemCount: Int,
)

internal data class SpotifyLikedPage(val totalCount: Int, val tracks: List<SpotifyLibraryTrack>, val itemCount: Int)
