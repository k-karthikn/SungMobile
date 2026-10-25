package com.karthi.sungmobile.core.data.library

import com.karthi.sungmobile.core.data.db.*
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.library.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.UUID

class RoomAdvancedLibraryRepository(private val db: SungDatabase,
    private val now: () -> Long = System::currentTimeMillis) : AdvancedLibraryRepository {
    private val dao = db.advancedLibraryDao()
    private val versions = PlaylistRevisionStore(db, now)
    override val changes: Flow<Unit> = db.changes("smart_playlists", "library_tracks", "library_likes",
        "listening_events", "history_policy", "local_playlists", "playlist_entries", "playlist_revisions")

    override suspend fun smartPlaylists() = withContext(Dispatchers.IO) { dao.smartPlaylists().map { it.toSmart() } }
    override suspend fun saveSmart(id: String?, name: String, rule: SmartRule): String = db.transaction {
        val valid = validatedPlaylistName(name)
        val old = id?.let { checkNotNull(dao.smartPlaylist(it)) { "Smart playlist no longer exists" } }
        val key = old?.id ?: UUID.randomUUID().toString()
        dao.saveSmart(SmartPlaylistEntity().apply {
            this.id = key; this.name = valid; requireLiked = rule.requireLiked; minPlays = rule.minPlays
            recentDays = rule.recentDays; sort = rule.sort.name; maxSongs = rule.maxSongs
            createdAt = old?.createdAt ?: now(); updatedAt = now()
        })
        key
    }
    override suspend fun deleteSmart(id: String) = db.transaction { dao.deleteSmart(id) }
    override suspend fun smartSongs(id: String): List<CatalogItem> = db.transaction {
        val rule = checkNotNull(dao.smartPlaylist(id)) { "Smart playlist no longer exists" }.toSmart().rule
        evaluate(rule)
    }
    override suspend fun previewSmart(rule: SmartRule): List<CatalogItem> = db.transaction { evaluate(rule) }
    private fun evaluate(rule: SmartRule): List<CatalogItem> {
        val privateMode = db.historyDao().policy()?.privateMode == true
        return evaluateSmartRule(rule, dao.candidates().map { SmartCandidate(it.toTrack(), it.liked, it.plays, it.lastPlayedAt) }, now(), privateMode)
    }
    override suspend fun revisions(playlistId: String) = withContext(Dispatchers.IO) {
        dao.revisions(playlistId).map { it.toRevision() }
    }
    override suspend fun revision(id: String): RevisionDetail? = withContext(Dispatchers.IO) {
        dao.revision(id)?.let { row -> versions.detail(row).let { detail -> detail.copy(entries = detail.entries.map { entry ->
            entry.copy(title = db.libraryDao().track(entry.trackKey)?.title ?: entry.trackKey)
        }) } }
    }
    override suspend fun restoreRevision(playlistId: String, revisionId: String) = db.transaction {
        checkNotNull(db.playlistDao().playlist(playlistId)) { "Playlist no longer exists" }
        val row = checkNotNull(dao.revision(revisionId)) { "Playlist version no longer exists" }
        require(row.playlistId == playlistId) { "Version belongs to a different playlist" }
        val detail = versions.detail(row)
        detail.entries.forEach { require(db.libraryDao().track(it.trackKey) != null) { "Song metadata is missing" } }
        versions.capture(playlistId)
        db.playlistDao().clearEntries(playlistId)
        db.playlistDao().insertEntries(detail.entries.mapIndexed { index, entry -> PlaylistEntryEntity().apply {
            id = entry.id; this.playlistId = playlistId; trackKey = entry.trackKey; position = index
        } })
        check(db.playlistDao().rename(playlistId, row.name, now()) == 1)
    }
    override suspend fun statistics(limit: Int, offset: Int): ListeningStats = db.transaction {
        if (db.historyDao().policy()?.privateMode == true) return@transaction ListeningStats(privateMode = true)
        val bound = limit.coerceIn(1, 100)
        val summary = dao.summary()
        val since = Calendar.getInstance().apply {
            timeInMillis = now()
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -29)
        }.timeInMillis
        ListeningStats(qualifiedPlays = summary.qualifiedPlays, listenedMs = summary.listenedMs,
            topSongs = dao.topSongs(bound).map { TopSong(it.toTrack(), it.plays, it.listenedMs) },
            recentEvents = dao.recentEvents(bound, offset.coerceAtLeast(0)).map { ListeningEvent(it.id, it.toTrack(), it.startedAt, it.lastPlayedAt, it.listenedMs) },
            uniqueSongs = summary.uniqueSongs,
            dailyActivity = dao.dailyActivity(since).map { DailyListening(it.day, it.listenedMs, it.plays) })
    }
}

private fun SmartPlaylistEntity.toSmart() = SmartPlaylist(id, name,
    SmartRule(requireLiked, minPlays, recentDays, SmartSort.valueOf(sort), maxSongs))
