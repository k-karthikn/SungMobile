package com.karthi.sungmobile.core.data.library

import com.karthi.sungmobile.core.data.db.*
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.library.*
import java.util.UUID

/** Transaction-only playlist operations, shared by creation and additions. */
internal class PlaylistWriter(private val db: SungDatabase, private val now: () -> Long,
    private val revisions: PlaylistRevisionStore) {
    fun add(id: String, tracks: List<CatalogItem>, allowDuplicates: Boolean, captureBefore: Boolean = true): AddResult {
        checkNotNull(db.playlistDao().playlist(id)) { "Playlist no longer exists" }
        val existing = db.playlistDao().entries(id)
        val seen = existing.mapTo(mutableSetOf()) { it.trackKey }
        val start = (existing.maxOfOrNull { it.position } ?: -1) + 1
        val added = mutableListOf<PlaylistEntryEntity>()
        tracks.forEach { track ->
            val key = db.libraryDao().saveMetadata(track)
            if (allowDuplicates || key !in seen) added += PlaylistEntryEntity().apply {
                this.id = UUID.randomUUID().toString()
                playlistId = id; trackKey = key; position = start + added.size
            }
            seen += key
        }
        if (added.isNotEmpty()) {
            if (captureBefore) revisions.capture(id)
            db.playlistDao().insertEntries(added)
            db.playlistDao().touch(id, now())
        }
        return AddResult(added.size, tracks.size - added.size)
    }
    fun normalize(id: String, entries: List<PlaylistEntryEntity>) {
        entries.forEachIndexed { index, row -> db.playlistDao().position(id, row.id, index) }
        db.playlistDao().touch(id, now())
    }
}
