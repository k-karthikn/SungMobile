package com.karthi.sungmobile.core.data

import androidx.room.Room
import com.karthi.sungmobile.core.data.db.SungDatabase
import com.karthi.sungmobile.core.data.history.RoomHistoryRepository
import com.karthi.sungmobile.core.data.library.RoomAdvancedLibraryRepository
import com.karthi.sungmobile.core.data.library.RoomLibraryRepository
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.CatalogKind
import com.karthi.sungmobile.core.model.library.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RoomAdvancedLibraryTest {
    private lateinit var db: SungDatabase
    private lateinit var library: RoomLibraryRepository
    private lateinit var advanced: RoomAdvancedLibraryRepository
    private lateinit var history: RoomHistoryRepository
    private var now = 1_000_000L
    private val first = CatalogItem("RB-RcX5DS5A", CatalogKind.SONG, "Yellow", "Coldplay",
        "https://www.youtube.com/watch?v=RB-RcX5DS5A")
    private val second = CatalogItem("dvgZkm1xWPE", CatalogKind.SONG, "Viva", "Coldplay",
        "https://www.youtube.com/watch?v=dvgZkm1xWPE")
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), SungDatabase::class.java).build()
        library = RoomLibraryRepository(db) { ++now }
        advanced = RoomAdvancedLibraryRepository(db) { ++now }
        history = RoomHistoryRepository(db) { ++now }
    }
    @After fun cleanup() { db.close() }

    @Test fun smartResultsFollowLikesHistoryPrivacyAndClear() = runBlocking {
        library.setLiked(listOf(first, second), true)
        val pure = advanced.saveSmart(null, "Likes", SmartRule())
        val played = advanced.saveSmart(null, "Played", SmartRule(true, 1, 0, SmartSort.MOST_PLAYED))
        assertEquals(2, advanced.smartSongs(pure).size)
        assertTrue(advanced.smartSongs(played).isEmpty())
        history.recordListening(ListeningRecord("pass:0", "pass", first, 0, 30_000, 1, 31_000, true))
        assertEquals(listOf("Yellow"), advanced.smartSongs(played).map { it.title })
        assertEquals(1L, advanced.statistics().qualifiedPlays)
        history.setPolicy(HistorySetting.PRIVATE_MODE, true)
        assertEquals(2, advanced.smartSongs(pure).size)
        assertTrue(advanced.smartSongs(played).isEmpty())
        assertTrue(advanced.statistics().privateMode)
        history.setPolicy(HistorySetting.PRIVATE_MODE, false)
        history.clearListening()
        assertTrue(advanced.smartSongs(played).isEmpty())
        assertEquals(0L, advanced.statistics().qualifiedPlays)
    }

    @Test fun versionsRestoreNameOccurrenceIdsAndOrder() = runBlocking {
        val id = library.create("Original", listOf(first, second, first))
        val before = library.songs(LibraryCollection.Playlist(id))
        library.rename(id, "Renamed")
        library.moveEntry(id, before.last().rowId, -1)
        library.removeEntries(id, setOf(before.first().rowId))
        val version = advanced.revisions(id).last()
        assertEquals("Original", version.name)
        assertEquals(3, version.songCount)
        advanced.restoreRevision(id, version.id)
        assertEquals("Original", library.playlist(id)!!.name)
        assertEquals(before.map { it.rowId }, library.songs(LibraryCollection.Playlist(id)).map { it.rowId })
        assertEquals(4, advanced.revisions(id).size)
    }

    @Test fun failedRestoreDoesNotChangePlaylistAndVersionsAreBounded() = runBlocking {
        val id = library.create("One", listOf(first))
        repeat(25) { library.rename(id, "Name $it") }
        assertEquals(20, advanced.revisions(id).size)
        try { advanced.restoreRevision(id, "missing"); fail("Expected failure") } catch (_: IllegalStateException) { }
        assertEquals("Name 24", library.playlist(id)!!.name)
        assertEquals(1, library.playlist(id)!!.songCount)
    }
}
