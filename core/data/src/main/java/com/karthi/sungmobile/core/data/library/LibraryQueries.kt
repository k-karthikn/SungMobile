package com.karthi.sungmobile.core.data.library

import androidx.sqlite.db.SimpleSQLiteQuery
import com.karthi.sungmobile.core.model.library.*

/** SQL fragments come from enums; all user text is bound as data. */
internal fun songQuery(collection: LibraryCollection, filter: String, sort: LibrarySort, limit: Int, offset: Int): SimpleSQLiteQuery {
    val args = mutableListOf<Any>()
    val source: String
    val id: String
    val position: String
    val order: String
    when (collection) {
        LibraryCollection.Likes -> {
            source = "library_likes s JOIN library_tracks t ON t.trackKey = s.trackKey"
            id = "t.trackKey"; position = "0"; order = "s.likedAt DESC, t.trackKey"
        }
        LibraryCollection.Recent -> {
            source = "(SELECT trackKey, MAX(lastPlayedAt) AS lastPlayedAt FROM listening_events WHERE qualified = 1 GROUP BY trackKey) s JOIN library_tracks t ON t.trackKey = s.trackKey"
            id = "t.trackKey"; position = "0"; order = "s.lastPlayedAt DESC, t.trackKey"
        }
        is LibraryCollection.Playlist -> {
            source = "(SELECT * FROM playlist_entries WHERE playlistId = ?) s JOIN library_tracks t ON t.trackKey = s.trackKey"
            args += collection.id
            id = "s.id"; position = "s.position"; order = "s.position, s.id"
        }
    }
    args.addAll(listOf(filter, filter, limit.coerceAtLeast(1), offset.coerceAtLeast(0)))
    val ordering = if (sort == LibrarySort.TITLE) "t.title COLLATE NOCASE, $id" else order
    return SimpleSQLiteQuery("""SELECT t.*, $id AS rowId, $position AS position,
        EXISTS(SELECT 1 FROM library_likes l WHERE l.trackKey = t.trackKey) AS liked
        FROM $source WHERE (instr(lower(t.title), lower(?)) > 0 OR instr(lower(t.artist), lower(?)) > 0)
        ORDER BY $ordering LIMIT ? OFFSET ?""", args.toTypedArray())
}
internal fun playlistQuery(filter: String, sort: LibrarySort, limit: Int, offset: Int): SimpleSQLiteQuery {
    val order = if (sort == LibrarySort.TITLE) "p.name COLLATE NOCASE, p.id" else "p.updatedAt DESC, p.id"
    return SimpleSQLiteQuery("""SELECT p.*, COUNT(e.id) AS songCount FROM local_playlists p
        LEFT JOIN playlist_entries e ON e.playlistId = p.id
        WHERE instr(lower(p.name), lower(?)) > 0 GROUP BY p.id
        ORDER BY p.pinned DESC, $order LIMIT ? OFFSET ?""", arrayOf<Any>(filter, limit, offset))
}
