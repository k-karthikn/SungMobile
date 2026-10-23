package com.karthi.sungmobile.core.data.library

import com.karthi.sungmobile.core.data.db.PlaylistRevisionEntity
import com.karthi.sungmobile.core.data.db.SungDatabase
import com.karthi.sungmobile.core.model.library.PlaylistRevision
import com.karthi.sungmobile.core.model.library.RevisionDetail
import com.karthi.sungmobile.core.model.library.RevisionEntry
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** All methods are called from a Room transaction. Snapshot entries keep their occurrence IDs. */
internal class PlaylistRevisionStore(private val db: SungDatabase, private val now: () -> Long) {
    fun capture(playlistId: String) {
        val playlist = checkNotNull(db.playlistDao().playlist(playlistId)) { "Playlist no longer exists" }
        val entries = db.playlistDao().entries(playlistId)
        val json = JSONArray()
        entries.forEach { entry -> json.put(JSONObject().put("id", entry.id).put("trackKey", entry.trackKey)) }
        db.advancedLibraryDao().insertRevision(PlaylistRevisionEntity().apply {
            id = UUID.randomUUID().toString()
            this.playlistId = playlistId
            name = playlist.name
            createdAt = now()
            entriesJson = json.toString()
            songCount = entries.size
        })
        db.advancedLibraryDao().trimRevisions(playlistId)
    }

    fun detail(row: PlaylistRevisionEntity): RevisionDetail {
        val array = JSONArray(row.entriesJson)
        val entries = (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            RevisionEntry(item.getString("id"), item.getString("trackKey"))
        }
        require(entries.size == row.songCount && entries.map { it.id }.toSet().size == entries.size) { "Invalid playlist version" }
        return RevisionDetail(row.toRevision(), entries)
    }
}

internal fun PlaylistRevisionEntity.toRevision() = PlaylistRevision(id, playlistId, name, createdAt, songCount)
