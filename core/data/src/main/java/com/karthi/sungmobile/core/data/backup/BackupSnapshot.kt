package com.karthi.sungmobile.core.data.backup

import com.karthi.sungmobile.core.data.db.*
import com.karthi.sungmobile.core.data.settings.PortableSettings

/** Portable user data; playback queue, searches, caches, and device file grants are excluded. */
internal data class BackupSnapshot(
    val settings: PortableSettings,
    val tracks: List<TrackEntity>,
    val likes: List<LikeEntity>,
    val playlists: List<PlaylistBackup>,
    val smartPlaylists: List<SmartPlaylistEntity>,
    val listening: List<ListeningEntity>,
    val history: HistoryPreferences,
)

internal data class PlaylistBackup(val playlist: PlaylistEntity, val entries: List<PlaylistEntryEntity>)

internal data class HistoryPreferences(val privateMode: Boolean, val listening: Boolean, val searches: Boolean)

data class BackupSummary(val likedSongs: Int, val playlists: Int, val qualifiedPlays: Int)
