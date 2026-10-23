package com.karthi.sungmobile.core.data

import com.karthi.sungmobile.core.data.db.DatabaseProvider
import com.karthi.sungmobile.core.data.history.RoomHistoryRepository
import com.karthi.sungmobile.core.data.library.RoomAdvancedLibraryRepository
import com.karthi.sungmobile.core.data.library.RoomLibraryRepository
import com.karthi.sungmobile.core.data.queue.RoomQueueStore
import com.karthi.sungmobile.core.model.*
import com.karthi.sungmobile.core.model.library.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LibraryRestartTest {
    @Test fun queueSmartRulesVersionsAndHistorySurviveReopenTogether() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val name = "stabilization-restart.db"
        context.deleteDatabase(name)
        val song = CatalogItem("dQw4w9WgXcQ", CatalogKind.SONG, "Song", "Artist", "https://youtu.be/dQw4w9WgXcQ")
        var db = DatabaseProvider.open(context, name)
        try {
            val library = RoomLibraryRepository(db)
            val advanced = RoomAdvancedLibraryRepository(db)
            library.setLiked(listOf(song), true)
            val playlistId = library.create("Before", listOf(song, song))
            val original = library.songs(LibraryCollection.Playlist(playlistId))
            library.rename(playlistId, "After")
            val revision = advanced.revisions(playlistId).single()
            val smartId = advanced.saveSmart(null, "Played Likes", SmartRule(minPlays = 1))
            RoomHistoryRepository(db).recordListening(ListeningRecord("pass:0", "pass", song, 0, 30_000, 1, 31_000, true))
            val queue = QueueSnapshot(QueueState(listOf(QueueEntry("one", song), QueueEntry("two", song)), "two"), 42_000, "pass")
            RoomQueueStore(db).write(queue)
            db.close()
            db = DatabaseProvider.open(context, name)
            val reopened = RoomAdvancedLibraryRepository(db)
            assertEquals(queue, RoomQueueStore(db).read())
            assertEquals(1, reopened.smartSongs(smartId).size)
            assertEquals(1L, reopened.statistics().qualifiedPlays)
            reopened.restoreRevision(playlistId, revision.id)
            val restored = RoomLibraryRepository(db)
            assertEquals("Before", restored.playlist(playlistId)!!.name)
            assertEquals(original.map { it.rowId }, restored.songs(LibraryCollection.Playlist(playlistId)).map { it.rowId })
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
