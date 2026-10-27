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

    private class FakeSettings : PortableSettingsStore {
        var value = PortableSettings(AppearanceSettings(), LyricsSettings(), PlaybackSpeed(),
            false, false, emptyMap(), emptyMap())
        override suspend fun exportPortable() = value
        override suspend fun importPortable(value: PortableSettings) { this.value = value }
    }
}
