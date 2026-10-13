package com.karthi.sungmobile.core.data.library

import com.karthi.sungmobile.core.data.db.*
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.library.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.util.UUID

class RoomLibraryRepository(private val db: SungDatabase, private val now: () -> Long = System::currentTimeMillis) : LibraryRepository {
    private val writer = PlaylistWriter(db, now)
    override val changes = db.changes("library_tracks", "library_likes", "local_playlists", "playlist_entries", "listening_events")
    override fun likedKeys(): Flow<Set<String>> = db.changes("library_likes")
        .map { withContext(Dispatchers.IO) { db.libraryDao().likedKeys().toSet() } }.distinctUntilChanged()
    override suspend fun songs(collection: LibraryCollection, filter: String, sort: LibrarySort, limit: Int, offset: Int) =
        withContext(Dispatchers.IO) { db.libraryDao().songs(songQuery(collection, filter, sort, limit, offset)).map { it.toSong() } }
    override suspend fun snapshot(collection: LibraryCollection): List<CatalogItem> =
        db.transaction { db.libraryDao().songs(songQuery(collection, "", LibrarySort.DEFAULT, Int.MAX_VALUE, 0)).map { it.toTrack() } }
    override suspend fun setLiked(tracks: List<CatalogItem>, liked: Boolean) = db.transaction {
        tracks.forEach { track ->
            val key = db.libraryDao().saveMetadata(track)
            if (liked) db.libraryDao().like(LikeEntity().apply { trackKey = key; likedAt = now() })
            else db.libraryDao().unlike(key)
        }
    }
    override suspend fun playlists(filter: String, sort: LibrarySort, limit: Int, offset: Int) = withContext(Dispatchers.IO) {
        db.playlistDao().playlists(playlistQuery(filter, sort, limit, offset)).map { it.toPlaylist() }
    }
    override suspend fun playlist(id: String) = withContext(Dispatchers.IO) { db.playlistDao().playlist(id)?.toPlaylist() }
    override suspend fun create(name: String, tracks: List<CatalogItem>, allowDuplicates: Boolean): String = db.transaction {
        val id = UUID.randomUUID().toString()
        db.playlistDao().create(PlaylistEntity().apply {
            this.id = id; this.name = validatedPlaylistName(name); createdAt = now(); updatedAt = createdAt
        })
        writer.add(id, tracks, allowDuplicates)
        id
    }
    override suspend fun rename(id: String, name: String) { db.transaction { check(db.playlistDao().rename(id, validatedPlaylistName(name), now()) == 1) } }
    override suspend fun deletePlaylist(id: String) = db.transaction { db.playlistDao().delete(id) }
    override suspend fun pin(id: String, pinned: Boolean) = db.transaction { db.playlistDao().pin(id, pinned) }
    override suspend fun add(id: String, tracks: List<CatalogItem>, allowDuplicates: Boolean) = db.transaction { writer.add(id, tracks, allowDuplicates) }
    override suspend fun removeEntries(id: String, entryIds: Set<String>) = db.transaction {
        // Chunk to respect SQLite's bind-variable limit, retaining one transaction for the whole edit.
        entryIds.toList().chunked(500).forEach { db.playlistDao().remove(id, it) }
        writer.normalize(id, db.playlistDao().entries(id))
    }
    override suspend fun moveEntry(id: String, entryId: String, offset: Int) = db.transaction {
        val entries = db.playlistDao().entries(id).toMutableList()
        val from = entries.indexOfFirst { it.id == entryId }
        if (from >= 0) {
            val to = (from + offset).coerceIn(0, entries.lastIndex)
            entries.add(to, entries.removeAt(from))
            writer.normalize(id, entries)
        }
    }
}
