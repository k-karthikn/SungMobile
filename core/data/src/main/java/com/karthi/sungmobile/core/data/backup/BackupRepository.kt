package com.karthi.sungmobile.core.data.backup

import com.karthi.sungmobile.core.data.db.SungDatabase
import com.karthi.sungmobile.core.data.db.HistoryPolicyEntity
import com.karthi.sungmobile.core.data.db.transaction
import com.karthi.sungmobile.core.data.settings.PortableSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Coordinates DataStore and Room; all rows are decoded and checked before any import writes. */
class BackupRepository(private val db: SungDatabase, private val settings: PortableSettingsStore) {
    suspend fun export(): ByteArray {
        val preferences = settings.exportPortable()
        val snapshot = db.transaction {
            val library = db.libraryDao()
            val playlists = db.playlistDao()
            val listening = db.historyDao().qualifiedForBackup()
            val likes = library.likesForBackup()
            val playlistRows = playlists.allForBackup().map { PlaylistBackup(it, playlists.entries(it.id)) }
            val needed = (likes.map { it.trackKey } +
                playlistRows.flatMap { it.entries.map { entry -> entry.trackKey } } +
                listening.map { it.trackKey }).toSet()
            BackupSnapshot(preferences, library.tracksForBackup().filter { it.trackKey in needed },
                likes, playlistRows, db.advancedLibraryDao().smartPlaylists(), listening,
                (db.historyDao().policy() ?: HistoryPolicyEntity()).let {
                    HistoryPreferences(it.privateMode, it.listening, it.searches)
                })
        }
        return withContext(Dispatchers.Default) { BackupJson.encode(snapshot) }
    }

    suspend fun restore(bytes: ByteArray): BackupSummary {
        val snapshot = withContext(Dispatchers.Default) { BackupJson.decode(bytes) }
        db.transaction {
            db.libraryDao().apply {
                importTracks(snapshot.tracks)
                snapshot.likes.forEach(::like)
            }
            db.playlistDao().apply {
                snapshot.playlists.forEach { item ->
                    if (playlist(item.playlist.id) == null) create(item.playlist)
                    importEntries(item.entries)
                }
            }
            db.advancedLibraryDao().apply {
                snapshot.smartPlaylists.forEach { item ->
                    if (smartPlaylist(item.id) == null) saveSmart(item)
                }
            }
            db.historyDao().apply {
                importEvents(snapshot.listening)
                val policy = policy() ?: HistoryPolicyEntity()
                if (policy.privateMode != snapshot.history.privateMode) {
                    policy.privateMode = snapshot.history.privateMode
                    policy.listeningEpoch++; policy.searchEpoch++
                }
                if (policy.listening != snapshot.history.listening) {
                    policy.listening = snapshot.history.listening; policy.listeningEpoch++
                }
                if (policy.searches != snapshot.history.searches) {
                    policy.searches = snapshot.history.searches; policy.searchEpoch++
                }
                setPolicy(policy)
            }
        }
        settings.importPortable(snapshot.settings)
        return BackupSummary(snapshot.likes.size, snapshot.playlists.size, snapshot.listening.size)
    }
}
