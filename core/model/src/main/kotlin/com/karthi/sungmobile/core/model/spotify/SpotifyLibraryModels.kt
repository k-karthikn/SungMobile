package com.karthi.sungmobile.core.model.spotify

data class SpotifyLibraryPlaylist(
    val id: String,
    val name: String,
    val description: String? = null,
    val artworkUrl: String? = null,
    val trackCount: Int = 0,
)

data class SpotifyLibraryTrack(
    val title: String,
    val artist: String,
    val album: String? = null,
    val artworkUrl: String? = null,
    val durationMs: Long? = null,
)

data class SpotifyLibraryCollection(
    val title: String,
    val tracks: List<SpotifyLibraryTrack>,
)
