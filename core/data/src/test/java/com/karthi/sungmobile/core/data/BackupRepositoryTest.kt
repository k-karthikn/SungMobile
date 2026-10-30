package com.karthi.sungmobile.core.data

import androidx.room.Room
import com.karthi.sungmobile.core.data.backup.BackupRepository
import com.karthi.sungmobile.core.data.db.*
import com.karthi.sungmobile.core.data.settings.*
import com.karthi.sungmobile.core.model.PlaybackSpeed
import com.karthi.sungmobile.core.model.discovery.RecommendationFeedback
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.json.JSONObject

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BackupRepositoryTest {
    @Test fun exportRestoresPortableLibraryStatisticsAndSettingsWithoutDuplicates() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val source = Room.inMemoryDatabaseBuilder(context, SungDatabase::class.java).allowMainThreadQueries().build()
        val target = Room.inMemoryDatabaseBuilder(context, SungDatabase::class.java).allowMainThreadQueries().build()
        try {
            val settings = FakeSettings()
            settings.value = settings.value.copy(appearance = AppearanceSettings(theme = "DARK"),
                recommendationsEnabled = false,
                recommendationFeedback = mapOf("youtube:RB-RcX5DS5A" to RecommendationFeedback.HIDE))
            val track = TrackEntity().apply {
                trackKey = "youtube:RB-RcX5DS5A"; title = "Yellow"; artist = "Coldplay"
                canonicalUrl = "https://www.youtube.com/watch?v=RB-RcX5DS5A"
            }
            source.libraryDao().saveTrack(track)
            source.libraryDao().like(LikeEntity().apply { trackKey = track.trackKey; likedAt = 1 })
            source.playlistDao().create(PlaylistEntity().apply {
                id = "playlist-1"; name = "Favorites"; createdAt = 1; updatedAt = 2
            })
            source.playlistDao().insertEntries(listOf(PlaylistEntryEntity().apply {
                id = "entry-1"; playlistId = "playlist-1"; trackKey = track.trackKey
            }))
            source.historyDao().saveEvent(ListeningEntity().apply {
                id = "play-1"; passId = "pass-1"; trackKey = track.trackKey
                listenedMs = 60_000; startedAt = 10; lastPlayedAt = 60_010; qualified = true
            })
            source.historyDao().setPolicy(HistoryPolicyEntity().apply { searches = false })
            val bytes = BackupRepository(source, settings).export()
            settings.value = settings.value.copy(appearance = AppearanceSettings(theme = "LIGHT"))
            val importer = BackupRepository(target, settings)
            val result = importer.restore(bytes)
            importer.restore(bytes)
            assertEquals(1, result.likedSongs)
            assertEquals(1, target.libraryDao().likedKeys().size)
            assertEquals(1, target.playlistDao().entries("playlist-1").size)
            assertEquals(1, target.historyDao().qualifiedForBackup().size)
            assertFalse(target.historyDao().policy().searches)
            assertEquals("DARK", settings.exportPortable().appearance.theme)
            assertFalse(settings.exportPortable().recommendationsEnabled)
            assertEquals(RecommendationFeedback.HIDE,
                settings.exportPortable().recommendationFeedback["youtube:RB-RcX5DS5A"])
        } finally {
            source.close(); target.close()
        }
    }

    @Test fun invalidBackupDoesNotWriteToDatabase() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val db = Room.inMemoryDatabaseBuilder(context, SungDatabase::class.java).allowMainThreadQueries().build()
        try {
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { BackupRepository(db, FakeSettings()).restore("{}".toByteArray()) }
            }
            assertTrue(db.libraryDao().likedKeys().isEmpty())
        } finally { db.close() }
    }

    @Test fun olderBackupWithoutRecommendationFieldsUsesSafeDefaults() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val db = Room.inMemoryDatabaseBuilder(context, SungDatabase::class.java).allowMainThreadQueries().build()
        try {
            val settings = FakeSettings()
            val root = JSONObject(BackupRepository(db, settings).export().toString(Charsets.UTF_8))
            root.getJSONObject("settings").remove("recommendationsEnabled")
            root.getJSONObject("settings").remove("recommendationFeedback")
            settings.value = settings.value.copy(recommendationsEnabled = false,
                recommendationFeedback = mapOf("youtube:RB-RcX5DS5A" to RecommendationFeedback.HIDE))
            BackupRepository(db, settings).restore(root.toString().toByteArray())
            assertTrue(settings.value.recommendationsEnabled)
            assertTrue(settings.value.recommendationFeedback.isEmpty())
        } finally { db.close() }
    }

    @Test fun settingsFailureLeavesJournalAndStartupRecoveryFinishesImport() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val source = Room.inMemoryDatabaseBuilder(context, SungDatabase::class.java).allowMainThreadQueries().build()
        val target = Room.inMemoryDatabaseBuilder(context, SungDatabase::class.java).allowMainThreadQueries().build()
        try {
            val sourceSettings = FakeSettings().apply {
                value = value.copy(appearance = AppearanceSettings(theme = "DARK"))
            }
            source.libraryDao().saveTrack(track("RB-RcX5DS5A", "Yellow"))
            source.libraryDao().like(LikeEntity().apply { trackKey = "youtube:RB-RcX5DS5A"; likedAt = 10 })
            val bytes = BackupRepository(source, sourceSettings).export()
            val targetSettings = FakeSettings().apply { failImports = true }
            val repository = BackupRepository(target, targetSettings)

            assertThrows(IllegalStateException::class.java) {
                runBlocking { repository.restore(bytes) }
            }
            assertEquals(1, target.libraryDao().likedKeys().size)
            assertNotNull(target.backupRestoreDao().pendingSettings())

            targetSettings.failImports = false
            assertTrue(repository.recoverPendingRestore())
            assertEquals("DARK", targetSettings.value.appearance.theme)
            assertNull(target.backupRestoreDao().pendingSettings())
            assertFalse(repository.recoverPendingRestore())
        } finally {
            source.close(); target.close()
        }
    }

    @Test fun conflictingPlaylistEntriesAreMergedWithoutLossOrDuplicates() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val source = Room.inMemoryDatabaseBuilder(context, SungDatabase::class.java).allowMainThreadQueries().build()
        val target = Room.inMemoryDatabaseBuilder(context, SungDatabase::class.java).allowMainThreadQueries().build()
        try {
            source.libraryDao().saveTrack(track("RB-RcX5DS5A", "Yellow"))
            source.libraryDao().saveTrack(track("dvgZkm1xWPE", "Song Two"))
            source.playlistDao().create(playlist("shared", "Imported name"))
            source.playlistDao().insertEntries(listOf(
                entry("shared", "same-entry", "youtube:dvgZkm1xWPE", 0),
                entry("shared", "new-entry", "youtube:RB-RcX5DS5A", 1),
            ))
            target.libraryDao().saveTrack(track("RB-RcX5DS5A", "Yellow"))
            target.playlistDao().create(playlist("shared", "Keep this name"))
            target.playlistDao().insertEntries(listOf(entry("shared", "same-entry", "youtube:RB-RcX5DS5A", 0)))
            val repository = BackupRepository(target, FakeSettings())
            val backup = BackupRepository(source, FakeSettings()).export()

            repository.restore(backup)
            repository.restore(backup)

            val entries = target.playlistDao().entries("shared")
            assertEquals("Keep this name", target.playlistDao().playlist("shared").name)
            assertEquals(3, entries.size)
            assertEquals(listOf("youtube:RB-RcX5DS5A", "youtube:dvgZkm1xWPE", "youtube:RB-RcX5DS5A"),
                entries.sortedBy { it.position }.map { it.trackKey })
            assertEquals(3, entries.map { it.id }.toSet().size)
        } finally {
            source.close(); target.close()
        }
    }

    @Test fun duplicateBackupEntryIdsAreRejectedBeforeDatabaseWrites() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val source = Room.inMemoryDatabaseBuilder(context, SungDatabase::class.java).allowMainThreadQueries().build()
        val target = Room.inMemoryDatabaseBuilder(context, SungDatabase::class.java).allowMainThreadQueries().build()
        try {
            source.libraryDao().saveTrack(track("RB-RcX5DS5A", "Yellow"))
            source.playlistDao().create(playlist("source", "Source"))
            source.playlistDao().insertEntries(listOf(entry("source", "same-id", "youtube:RB-RcX5DS5A", 0)))
            val root = JSONObject(BackupRepository(source, FakeSettings()).export().toString(Charsets.UTF_8))
            val rows = root.getJSONArray("playlists").getJSONObject(0).getJSONArray("entries")
            rows.put(JSONObject(rows.getJSONObject(0).toString()))

            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { BackupRepository(target, FakeSettings()).restore(root.toString().toByteArray()) }
            }
            assertTrue(target.libraryDao().likedKeys().isEmpty())
            assertNull(target.libraryDao().track("youtube:RB-RcX5DS5A"))
            assertNull(target.playlistDao().playlist("source"))
            assertNull(target.backupRestoreDao().pendingSettings())
        } finally {
            source.close(); target.close()
        }
    }

    private fun track(id: String, title: String) = TrackEntity().apply {
        trackKey = "youtube:$id"
        this.title = title
        artist = "Artist"
        canonicalUrl = "https://www.youtube.com/watch?v=$id"
    }

    private fun playlist(id: String, name: String) = PlaylistEntity().apply {
        this.id = id; this.name = name; createdAt = 1; updatedAt = 1
    }

    private fun entry(playlistId: String, id: String, key: String, position: Int) = PlaylistEntryEntity().apply {
        this.id = id; this.playlistId = playlistId; trackKey = key; this.position = position
    }

    private class FakeSettings : PortableSettingsStore {
        var value = PortableSettings(AppearanceSettings(), LyricsSettings(), PlaybackSpeed(),
            false, false, emptyMap(), emptyMap())
        var failImports = false
        override suspend fun exportPortable() = value
        override suspend fun importPortable(value: PortableSettings) {
            if (failImports) error("Simulated settings write failure")
            this.value = value
        }
    }
}
