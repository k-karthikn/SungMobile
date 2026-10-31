package com.karthi.sungmobile.core.model

import java.net.URI
import java.net.URLDecoder

sealed interface YouTubeLink {
    val url: String

    data class Song(val videoId: String) : YouTubeLink {
        override val url = "https://www.youtube.com/watch?v=$videoId"
    }

    data class Playlist(val playlistId: String) : YouTubeLink {
        override val url = "https://www.youtube.com/playlist?list=$playlistId"
    }

    data class Artist(override val url: String) : YouTubeLink
    data class Browse(val browseId: String) : YouTubeLink {
        override val url = "https://music.youtube.com/browse/$browseId"
    }

    companion object {
        private val hosts = setOf("youtube.com", "www.youtube.com", "m.youtube.com", "music.youtube.com")
        private val videoId = Regex("[A-Za-z0-9_-]{11}")
        private val playlistId = Regex("[A-Za-z0-9_-]{10,200}")

        fun parse(input: String): YouTubeLink {
            try {
                val uri = URI(input.trim())
                val host = uri.host?.lowercase() ?: invalid()
                if (uri.scheme?.lowercase() != "https" || uri.userInfo != null ||
                    (uri.port != -1 && uri.port != 443)
                ) invalid()
                if (host !in hosts && host != "youtu.be") invalid()
                val query = uri.rawQuery.orEmpty().split('&').mapNotNull { part ->
                    val pair = part.split('=', limit = 2)
                    if (pair.size == 2) pair[0] to URLDecoder.decode(pair[1], "UTF-8") else null
                }.toMap()
                val path = uri.path.orEmpty()
                val id = when {
                    host == "youtu.be" -> path.removePrefix("/")
                    path == "/watch" -> query["v"]
                    path.startsWith("/shorts/") -> path.removePrefix("/shorts/")
                    path.startsWith("/embed/") -> path.removePrefix("/embed/")
                    else -> null
                }
                if (id != null) {
                    if (!videoId.matches(id)) invalid()
                    return Song(id)
                }
                if (host == "music.youtube.com" && path.startsWith("/browse/")) {
                    val browse = path.removePrefix("/browse/")
                    if (!Regex("(?:MPRE|MPSPPL|VL|UC|MPLA)[A-Za-z0-9_-]{5,190}").matches(browse)) invalid()
                    return Browse(browse)
                }
                if (path == "/playlist" && host in hosts) {
                    val list = query["list"] ?: invalid()
                    if (!playlistId.matches(list)) invalid()
                    return Playlist(list)
                }
                if (host in hosts && (Regex("/channel/UC[A-Za-z0-9_-]{22}(?:/videos)?").matches(path) ||
                    Regex("/@[^/\\s?#]{1,100}(?:/videos)?").matches(path))
                ) return Artist("https://www.youtube.com$path")
                invalid()
            } catch (error: SourceException) {
                throw error
            } catch (error: Exception) {
                throw SourceException(SourceFailure.INVALID_LINK)
            }
        }

        private fun invalid(): Nothing = throw SourceException(SourceFailure.INVALID_LINK)
    }
}
