package com.karthi.sungmobile.core.data.recommendation

import com.karthi.sungmobile.core.data.db.HistoryPolicyEntity
import com.karthi.sungmobile.core.data.db.SungDatabase
import com.karthi.sungmobile.core.data.db.changes
import com.karthi.sungmobile.core.data.db.transaction
import com.karthi.sungmobile.core.data.history.toPolicy
import com.karthi.sungmobile.core.data.library.playlistQuery
import com.karthi.sungmobile.core.data.library.songQuery
import com.karthi.sungmobile.core.data.library.toTrack
import com.karthi.sungmobile.core.model.discovery.RecommendationProfile
import com.karthi.sungmobile.core.model.discovery.RecommendationProfileSource
import com.karthi.sungmobile.core.model.library.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Reads only a bounded slice of existing library and qualified-listening data. */
class RoomRecommendationProfileSource(private val db: SungDatabase) : RecommendationProfileSource {
    override fun observe(): Flow<RecommendationProfile> = db.changes("library_tracks", "library_likes",
        "local_playlists", "playlist_entries", "listening_events", "history_policy").map {
        db.transaction {
            val policy = (db.historyDao().policy() ?: HistoryPolicyEntity()).toPolicy()
            if (policy.privateMode) return@transaction RecommendationProfile(policy = policy)
            val library = db.libraryDao()
            val liked = library.songs(songQuery(LibraryCollection.Likes, "", LibrarySort.DEFAULT, 50, 0))
                .map { it.toTrack() }
            val playlists = db.playlistDao().playlists(playlistQuery("", LibrarySort.DEFAULT, 4, 0))
            val saved = playlists.flatMap { playlist ->
                library.songs(songQuery(LibraryCollection.Playlist(playlist.id), "", LibrarySort.DEFAULT, 20, 0))
                    .map { it.toTrack() }
            }
            val statistics = if (policy.listening) db.advancedLibraryDao() else null
            RecommendationProfile(policy, liked, saved,
                statistics?.topSongs(20)?.map { TopSong(it.toTrack(), it.plays, it.listenedMs) }.orEmpty(),
                statistics?.recentEvents(25, 0)?.map {
                    ListeningEvent(it.id, it.toTrack(), it.startedAt, it.lastPlayedAt, it.listenedMs)
                }.orEmpty())
        }
    }.distinctUntilChanged()
}
