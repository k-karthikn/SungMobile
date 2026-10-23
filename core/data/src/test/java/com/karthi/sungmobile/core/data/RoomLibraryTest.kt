package com.karthi.sungmobile.core.data

import androidx.room.Room
import com.karthi.sungmobile.core.data.db.SungDatabase
import com.karthi.sungmobile.core.data.library.RoomLibraryRepository
import com.karthi.sungmobile.core.data.history.RoomHistoryRepository
import com.karthi.sungmobile.core.model.*
import com.karthi.sungmobile.core.model.library.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RoomLibraryTest {
    private lateinit var db: SungDatabase
    private lateinit var library: RoomLibraryRepository
    private lateinit var history: RoomHistoryRepository
    private var clock = 100L
    private val song = CatalogItem("RB-RcX5DS5A", CatalogKind.SONG, "Yellow", "Coldplay",
        "https://www.youtube.com/watch?v=RB-RcX5DS5A", "https://example.test/art", 120_000)
    private val other = song.copy(id = "dvgZkm1xWPE", title = "Viva", url = "https://www.youtube.com/watch?v=dvgZkm1xWPE")
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), SungDatabase::class.java).build()
        library = RoomLibraryRepository(db) { ++clock }; history = RoomHistoryRepository(db) { ++clock }
    }
    @After fun cleanup() { db.close() }
    @Test fun canonicalLikesMergeMissingMetadataAndPersistIndependentlyOfPlaylists() = runBlocking {
        library.setLiked(listOf(song), true)
        library.setLiked(listOf(song.copy(url = "https://youtu.be/RB-RcX5DS5A?t=4", artworkUrl = null, subtitle = "", durationMs = null)), true)
        assertEquals(setOf("youtube:RB-RcX5DS5A"), library.likedKeys().first())
        val saved = library.songs(LibraryCollection.Likes).single().track
        assertEquals("Coldplay", saved.subtitle); assertEquals(song.artworkUrl, saved.artworkUrl)
        val id = library.create("Playlist", listOf(song))
        library.deletePlaylist(id)
        assertEquals(1, library.songs(LibraryCollection.Likes).size)
    }
    @Test fun duplicatesHaveSeparateIdentityAndReorderRemoveByEntry() = runBlocking {
        val id = library.create("  Queue copy  ", listOf(song, other, song))
        assertEquals("Queue copy", library.playlist(id)!!.name)
        val collection = LibraryCollection.Playlist(id)
        val rows = library.songs(collection)
        assertEquals(3, rows.map { it.rowId }.distinct().size)
        library.moveEntry(id, rows.last().rowId, -1)
        assertEquals(listOf(song.title, song.title, other.title), library.snapshot(collection).map { it.title })
        library.removeEntries(id, setOf(rows.first().rowId))
        assertEquals(listOf(song.title, other.title), library.snapshot(collection).map { it.title })
        assertEquals(AddResult(0, 2), library.add(id, listOf(song, song)))
        assertEquals(AddResult(2, 0), library.add(id, listOf(song, song), true))
    }
    @Test fun invalidBulkEditRollsBackAndNamesAreValidated() = runBlocking {
        val id = library.create("Safe")
        try { library.add(id, listOf(song, song.copy(url = "broken"))); fail("Expected invalid key") } catch (_: SourceException) { }
        assertEquals(0, library.playlist(id)!!.songCount)
        for (name in listOf(" ", "x".repeat(121))) {
            try { library.rename(id, name); fail("Expected invalid name") } catch (_: IllegalArgumentException) { }
        }
        assertEquals("Safe", library.playlist(id)!!.name)
    }
    @Test fun filteringSortingPagingAndPinsHaveStableResults() = runBlocking {
        library.setLiked(listOf(song, other), true)
        assertEquals("Viva", library.songs(LibraryCollection.Likes, sort = LibrarySort.TITLE, limit = 1).single().track.title)
        assertEquals("Yellow", library.songs(LibraryCollection.Likes, sort = LibrarySort.TITLE, limit = 1, offset = 1).single().track.title)
        assertEquals(2, library.songs(LibraryCollection.Likes, "coldplay").size)
        assertTrue(library.songs(LibraryCollection.Likes, "%").isEmpty())
        val pinned = library.create("Z pinned"); library.create("A newer"); library.pin(pinned, true)
        assertEquals(pinned, library.playlists(sort = LibrarySort.TITLE).first().id)
    }
    @Test fun staleWritesCannotUndoClearOrPrivacyAndManualLikesStillWork() = runBlocking {
        val policy = history.policy().first()
        val event = ListeningRecord("pass:0", "pass", song, 0, 30_000, 1, 31_000, true)
        history.recordListening(event); history.recordSearch("hello", CatalogKind.SONG, policy)
        history.clearListening(); history.clearSearches()
        history.recordListening(event.copy(listenedMs = 40_000)); history.recordSearch("hello", CatalogKind.SONG, policy)
        assertNull(history.checkpoint(event.id)); assertTrue(history.searches().first().isEmpty())
        val afterClear = history.policy().first()
        history.setPolicy(HistorySetting.PRIVATE_MODE, true)
        history.setPolicy(HistorySetting.PRIVATE_MODE, false)
        history.recordSearch("stale", CatalogKind.SONG, afterClear)
        assertTrue(history.searches().first().isEmpty())
        history.setPolicy(HistorySetting.PRIVATE_MODE, true)
        library.setLiked(listOf(song), true)
        assertEquals(1, library.songs(LibraryCollection.Likes).size)
    }
    @Test fun checkpointsAreIdempotentAndRecentDeduplicatesQualifiedPasses() = runBlocking {
        val event = ListeningRecord("pass:0", "pass", song, 0, 30_000, 1, 31_000, true)
        history.recordListening(event); history.recordListening(event.copy(listenedMs = 10_000, qualified = false))
        history.recordListening(event.copy(id = "next:0", passId = "next", lastPlayedAt = 50_000))
        history.recordListening(event.copy(id = "short:0", track = other, qualified = false))
        assertEquals(30_000L, history.checkpoint(event.id)!!.listenedMs)
        assertEquals(listOf(song.title), library.songs(LibraryCollection.Recent).map { it.track.title })
        assertNotNull(history.checkpoint("next:0"))
    }
    @Test fun privateModeHidesRecentListsAndPlaybackSnapshotsButKeepsLikes() = runBlocking {
        history.recordListening(ListeningRecord("pass:0", "pass", song, 0, 30_000, 1, 31_000, true))
        library.setLiked(listOf(song), true)
        assertEquals(1, library.snapshot(LibraryCollection.Recent).size)
        history.setPolicy(HistorySetting.PRIVATE_MODE, true)
        assertTrue(library.songs(LibraryCollection.Recent).isEmpty())
        assertTrue(library.snapshot(LibraryCollection.Recent).isEmpty())
        assertEquals(1, library.songs(LibraryCollection.Likes).size)
        history.setPolicy(HistorySetting.PRIVATE_MODE, false)
        assertEquals(1, library.songs(LibraryCollection.Recent).size)
    }
    @Test fun searchSubmissionDeduplicatesByCategoryTrimsAndKeepsLatestFifty() = runBlocking {
        val policy = history.policy().first()
        history.recordSearch(" HELLO ", CatalogKind.SONG, policy); history.recordSearch("hello", CatalogKind.SONG, policy)
        history.recordSearch("hello", CatalogKind.ALBUM, policy)
        assertEquals(2, history.searches().first().size)
        repeat(55) { history.recordSearch("query $it", CatalogKind.SONG, policy) }
        val searches = history.searches().first()
        assertEquals(50, searches.size); assertEquals("query 54", searches.first().query)
        history.removeSearch(searches.first().id)
        history.recordSearch("query 54", CatalogKind.SONG, policy)
        assertEquals(49, history.searches().first().size)
    }
}
