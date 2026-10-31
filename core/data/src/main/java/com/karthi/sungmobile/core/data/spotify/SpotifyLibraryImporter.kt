package com.karthi.sungmobile.core.data.spotify

import com.karthi.sungmobile.core.model.spotify.SpotifyLibraryCollection
import com.karthi.sungmobile.core.model.spotify.SpotifyLibraryPlaylist
import com.karthi.sungmobile.core.model.spotify.SpotifyLibraryTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Reads the connected account's Spotify library using its saved Web Player session. */
class SpotifyLibraryImporter(
    private val sessions: SpotifySessionStore,
    private val sessionValidator: SpotifySessionValidator,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()
    private val clientTokens = SpotifyClientTokenProvider()

    suspend fun fetchPlaylists(): List<SpotifyLibraryPlaylist> = withContext(Dispatchers.IO) {
        val token = accountToken()
        val playlists = LinkedHashMap<String, SpotifyLibraryPlaylist>()
        var offset = 0
        repeat(MAX_PLAYLIST_PAGES) {
            val variables = JSONObject()
                .put("filters", org.json.JSONArray().put("Playlists"))
                .put("order", JSONObject.NULL)
                .put("textFilter", JSONObject.NULL)
                .put("features", org.json.JSONArray())
                .put("limit", PLAYLIST_PAGE_SIZE)
                .put("offset", offset)
                .put("flatten", false)
                .put("expandedFolders", org.json.JSONArray())
                .put("folderUri", JSONObject.NULL)
                .put("includeFoldersWhenFlattening", true)
                .toString()
            val body = pathfinder(token, OP_LIBRARY, HASH_LIBRARY_V3, variables)
            val page = SpotifyLibraryResponseParser.playlists(body)
            page.forEach { playlists.putIfAbsent(it.id, it) }
            val count = SpotifyLibraryResponseParser.libraryItemCount(body)
            offset += count
            if (count < PLAYLIST_PAGE_SIZE) return@withContext playlists.values.toList()
        }
        playlists.values.toList()
    }

    suspend fun fetchPlaylist(playlist: SpotifyLibraryPlaylist): SpotifyLibraryCollection =
        fetchPlaylist(playlist.id, playlist.name)

    suspend fun fetchLikedSongs(): SpotifyLibraryCollection = withContext(Dispatchers.IO) {
        val token = accountToken()
        val tracks = ArrayList<SpotifyLibraryTrack>()
        var offset = 0
        var total = Int.MAX_VALUE
        for (pageIndex in 0 until MAX_LIKED_PAGES) {
            if (offset >= total) break
            val variables = JSONObject().put("offset", offset).put("limit", LIKED_PAGE_SIZE).toString()
            val body = pathfinder(token, OP_LIKED_TRACKS, HASH_FETCH_LIBRARY_TRACKS, variables)
            val page = SpotifyLibraryResponseParser.likedPage(body)
                ?: throw SpotifyLibraryImportException("Spotify returned an unreadable Liked Songs page.")
            total = page.totalCount
            if (page.itemCount == 0) break
            tracks += page.tracks
            offset += page.itemCount
        }
        SpotifyLibraryCollection("Spotify Liked Songs", tracks)
    }

    private suspend fun fetchPlaylist(id: String, fallbackTitle: String): SpotifyLibraryCollection =
        withContext(Dispatchers.IO) {
            val token = accountToken()
            val tracks = ArrayList<SpotifyLibraryTrack>()
            var title = fallbackTitle
            var total = Int.MAX_VALUE
            var offset = 0
            for (pageIndex in 0 until MAX_PLAYLIST_TRACK_PAGES) {
                if (offset >= total) break
                val variables = JSONObject()
                    .put("uri", "spotify:playlist:$id")
                    .put("offset", offset)
                    .put("limit", PLAYLIST_TRACK_PAGE_SIZE)
                    .put("enableWatchFeedEntrypoint", false)
                    .toString()
                val body = pathfinder(token, OP_PLAYLIST, HASH_FETCH_PLAYLIST, variables)
                val page = SpotifyLibraryResponseParser.playlistPage(body)
                    ?: throw SpotifyLibraryImportException("Spotify returned an unreadable playlist.")
                if (offset == 0) title = page.title
                total = page.totalCount
                if (page.itemCount == 0) break
                tracks += page.tracks
                offset += page.itemCount
            }
            SpotifyLibraryCollection(title, tracks)
        }

    private suspend fun accountToken(): String {
        when (sessionValidator.validate()) {
            SpotifySessionValidation.VALID -> Unit
            SpotifySessionValidation.INVALID -> throw SpotifyLibraryImportException(
                "Spotify sign-in expired. Reconnect your Spotify account in Settings.")
            SpotifySessionValidation.UNAVAILABLE -> throw SpotifyLibraryImportException(
                "Spotify could not verify the saved session. Check your connection and try again.")
        }
        return sessions.readPersonalToken()?.value?.takeIf(String::isNotBlank)
            ?: throw SpotifyLibraryImportException("No Spotify session is available. Connect Spotify in Settings.")
    }

    private suspend fun pathfinder(token: String, operation: String, hash: String, variables: String): String {
        val extensions = JSONObject().put("persistedQuery", JSONObject()
            .put("version", 1).put("sha256Hash", hash)).toString()
        val url = PATHFINDER_URL.toHttpUrl().newBuilder()
            .addQueryParameter("operationName", operation)
            .addQueryParameter("variables", variables)
            .addQueryParameter("extensions", extensions)
            .build()
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .header("User-Agent", DESKTOP_USER_AGENT)
            .header("Accept", "application/json")
            .apply { clientTokens.get()?.let { header("Client-Token", it) } }
            .get()
            .build()
        return client.newCall(request).execute().use { response ->
            if (response.code == 401 || response.code == 403) {
                throw SpotifyLibraryImportException("Spotify rejected the saved session. Reconnect Spotify and try again.")
            }
            if (response.code == 429) {
                throw SpotifyLibraryImportException("Spotify is rate limiting library requests. Wait a little and retry.")
            }
            if (!response.isSuccessful) {
                throw SpotifyLibraryImportException(
                    if (response.code == 400 || response.code == 404) {
                        "Spotify changed its library endpoint. Update SungMobile and retry."
                    } else "Spotify library request failed (HTTP ${response.code}).")
            }
            response.body?.string()?.takeIf(String::isNotBlank)
                ?: throw SpotifyLibraryImportException("Spotify returned an empty library response.")
        }
    }

    private companion object {
        const val PATHFINDER_URL = "https://api-partner.spotify.com/pathfinder/v1/query"
        const val HASH_FETCH_PLAYLIST = "21ea11a6ac75a5f3e01fd365d29c18e0a4a8b7dc09f89bbb5d73910190c6acaf"
        const val HASH_LIBRARY_V3 = "390c78e5b951029bad359785e69b07b536a509c581cbcd0aded5e5067f187455"
        const val HASH_FETCH_LIBRARY_TRACKS = "087278b20b743578a6262c2b0b4bcd20d879c503cc359a2285baf083ef944240"
        const val OP_PLAYLIST = "fetchPlaylist"
        const val OP_LIBRARY = "libraryV3"
        const val OP_LIKED_TRACKS = "fetchLibraryTracks"
        const val PLAYLIST_PAGE_SIZE = 50
        const val PLAYLIST_TRACK_PAGE_SIZE = 100
        const val LIKED_PAGE_SIZE = 50
        const val MAX_PLAYLIST_PAGES = 40
        const val MAX_PLAYLIST_TRACK_PAGES = 100
        const val MAX_LIKED_PAGES = 100
        const val DESKTOP_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    }
}

class SpotifyLibraryImportException(message: String) : Exception(message)
