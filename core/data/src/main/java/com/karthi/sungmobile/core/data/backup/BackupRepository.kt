package com.karthi.sungmobile.core.data.backup

import com.karthi.sungmobile.core.data.db.SungDatabase
import com.karthi.sungmobile.core.data.db.HistoryPolicyEntity
import com.karthi.sungmobile.core.data.db.BackupRestoreEntity
import com.karthi.sungmobile.core.data.db.PlaylistEntryEntity
import com.karthi.sungmobile.core.data.db.transaction
import com.karthi.sungmobile.core.data.settings.PortableSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets
import java.util.UUID

/** Coordinates DataStore and Room; all rows are decoded and checked before any import writes. */
class BackupRepository(private val db: SungDatabase, private val settings: PortableSettingsStore) {
    private val restoreMutex = Mutex()

    suspend fun export(): ByteArray = restoreMutex.withLock {
        finishPendingRestore()
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
        withContext(Dispatchers.Default) { BackupJson.encode(snapshot) }
    }

    suspend fun restore(bytes: ByteArray): BackupSummary {
        val snapshot = withContext(Dispatchers.Default) { BackupJson.decode(bytes) }
        return restoreMutex.withLock {
            finishPendingRestore()
            db.transaction {
                db.backupRestoreDao().save(BackupRestoreEntity().apply {
                    settingsJson = BackupJson.encodeSettings(snapshot.settings)
                })
                db.libraryDao().apply {
                    importTracks(snapshot.tracks)
                    snapshot.likes.forEach(::like)
                }
                db.playlistDao().apply {
                    snapshot.playlists.forEach { mergePlaylist(it) }
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
            finishPendingRestore()
            BackupSummary(snapshot.likes.size, snapshot.playlists.size, snapshot.listening.size)
        }
    }

    /** Replays only settings after Room's import transaction committed and the process stopped. */
    suspend fun recoverPendingRestore(): Boolean = restoreMutex.withLock { finishPendingRestore() }

    private suspend fun finishPendingRestore(): Boolean {
        val pending = withContext(Dispatchers.IO) { db.backupRestoreDao().pendingSettings() } ?: return false
        settings.importPortable(withContext(Dispatchers.Default) { BackupJson.decodeSettings(pending) })
        db.transaction { db.backupRestoreDao().clear() }
        return true
    }

    private fun com.karthi.sungmobile.core.data.db.PlaylistDao.mergePlaylist(item: PlaylistBackup) {
        val playlistId = item.playlist.id
        if (playlist(playlistId) == null) create(item.playlist)
        val existing = entries(playlistId).sortedWith(compareBy({ it.position }, { it.id }))
        val additions = mutableListOf<PlaylistEntryEntity>()
        item.entries.sortedWith(compareBy({ it.position }, { it.id })).forEach { incoming ->
            val current = entry(incoming.id)
            val incomingId = when {
                current == null -> incoming.id
                current.playlistId == playlistId && current.trackKey == incoming.trackKey -> return@forEach
                else -> mergedEntryId(playlistId, incoming)
            }
            val duplicate = entry(incomingId)
            if (duplicate != null) {
                if (duplicate.playlistId == playlistId && duplicate.trackKey == incoming.trackKey) return@forEach
                error("Could not assign a stable ID to an imported playlist entry.")
            }
            val position = if (existing.isEmpty()) incoming.position else 0
            additions += PlaylistEntryEntity().apply {
                id = incomingId
                this.playlistId = playlistId
                trackKey = incoming.trackKey
                this.position = position
            }
        }
        if (existing.isNotEmpty() && additions.isNotEmpty()) {
            existing.forEachIndexed { index, row -> position(playlistId, row.id, index) }
            additions.forEachIndexed { index, row -> row.position = existing.size + index }
        }
        if (additions.isNotEmpty()) db.playlistDao().insertEntries(additions)
    }

    private fun mergedEntryId(playlistId: String, entry: com.karthi.sungmobile.core.data.db.PlaylistEntryEntity): String =
        UUID.nameUUIDFromBytes("sung-backup-entry:$playlistId:${entry.id}:${entry.trackKey}:${entry.position}"
            .toByteArray(StandardCharsets.UTF_8)).toString()
}
