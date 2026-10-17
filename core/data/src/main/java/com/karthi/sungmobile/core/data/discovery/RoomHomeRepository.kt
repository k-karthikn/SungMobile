package com.karthi.sungmobile.core.data.discovery

import com.karthi.sungmobile.core.data.db.*
import com.karthi.sungmobile.core.data.library.*
import com.karthi.sungmobile.core.model.discovery.*
import com.karthi.sungmobile.core.model.library.*
import kotlinx.coroutines.flow.*

/** Read privacy and its derived sections in one transaction. Never cache a private listening segment. */
class RoomHomeRepository(private val db: SungDatabase) : HomeRepository {
    override fun observe(): Flow<HomeData> = db.changes("library_tracks", "library_likes", "local_playlists",
        "playlist_entries", "listening_events", "history_policy").map {
        db.transaction {
            val policy = db.historyDao().policy() ?: HistoryPolicyEntity()
            fun tracks(collection: LibraryCollection) = db.libraryDao().songs(songQuery(collection, "", LibrarySort.DEFAULT, 50, 0)).map { it.toTrack() }
            HomeData(tracks(LibraryCollection.Likes), if (policy.privateMode) emptyList() else tracks(LibraryCollection.Recent),
                db.playlistDao().playlists(playlistQuery("", LibrarySort.DEFAULT, 12, 0)).filter { it.pinned }.map { it.toPlaylist() },
                policy.privateMode, policy.listeningEpoch)
        }
    }.distinctUntilChanged()
}
