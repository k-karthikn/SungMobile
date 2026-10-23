package com.karthi.sungmobile.core.data.library

import com.karthi.sungmobile.core.data.db.*
import com.karthi.sungmobile.core.model.*
import com.karthi.sungmobile.core.model.library.*

internal fun TrackEntity.toTrack() = CatalogItem(canonicalUrl, CatalogKind.SONG, title, artist,
    canonicalUrl, artworkUrl, durationMs)
internal fun LibrarySongRow.toSong() = LibrarySong(rowId, toTrack(), liked, position)
internal fun PlaylistRow.toPlaylist() = LocalPlaylist(id, name, pinned, songCount)

/** Call inside a database transaction. A partial provider response cannot erase useful metadata. */
internal fun LibraryDao.saveMetadata(track: CatalogItem): String {
    val key = track.libraryKey()
    val previous = track(key)
    saveTrack(TrackEntity().apply {
        trackKey = key
        canonicalUrl = if (track.source == TrackSource.DEVICE) track.url
            else (YouTubeLink.parse(track.url) as YouTubeLink.Song).url
        title = track.title.takeIf { it.isNotBlank() } ?: previous?.title.orEmpty()
        artist = track.subtitle.takeIf { it.isNotBlank() } ?: previous?.artist.orEmpty()
        artworkUrl = track.artworkUrl?.takeIf { it.isNotBlank() } ?: previous?.artworkUrl
        durationMs = track.durationMs?.takeIf { it > 0 } ?: previous?.durationMs
    })
    return key
}
