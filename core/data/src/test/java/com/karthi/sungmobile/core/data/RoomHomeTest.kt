package com.karthi.sungmobile.core.data

import androidx.room.Room
import com.karthi.sungmobile.core.data.db.SungDatabase
import com.karthi.sungmobile.core.data.discovery.RoomHomeRepository
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
class RoomHomeTest {
    private lateinit var db: SungDatabase
    @Before fun setup() { db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), SungDatabase::class.java).build() }
    @After fun cleanup() { db.close() }
    @Test fun homeUsesOnlyQualifiedPublicHistoryAndKeepsLikesWhenCleared() = runBlocking {
        val library = RoomLibraryRepository(db)
        val history = RoomHistoryRepository(db)
        val home = RoomHomeRepository(db)
        val song = CatalogItem("dQw4w9WgXcQ", CatalogKind.SONG, "Song", "Artist", "https://youtu.be/dQw4w9WgXcQ")
        library.setLiked(listOf(song), true)
        history.recordListening(ListeningRecord("pass:0", "pass", song, 0, 30_000, 1, 31_000, true))
        assertEquals(1, home.observe().first().recent.size)
        history.setPolicy(HistorySetting.PRIVATE_MODE, true)
        val privateHome = home.observe().first()
        assertTrue(privateHome.privateMode); assertTrue(privateHome.recent.isEmpty()); assertEquals(1, privateHome.liked.size)
        history.clearListening(); history.setPolicy(HistorySetting.PRIVATE_MODE, false)
        assertTrue(home.observe().first().recent.isEmpty())
        assertEquals(1, home.observe().first().liked.size)
    }
    @Test fun candidatesAndPinnedPreviewsAreBoundedAndPinnedOnly() = runBlocking {
        val library = RoomLibraryRepository(db)
        val home = RoomHomeRepository(db)
        library.setLiked((1..70).map { CatalogItem("$it", CatalogKind.SONG, "Song $it", "Artist", "https://youtu.be/${it.toString().padStart(11, '0')}") }, true)
        repeat(15) { library.pin(library.create("Pinned $it"), true) }
        library.create("Newest unpinned")
        val data = home.observe().first()
        assertEquals(50, data.liked.size); assertEquals(12, data.pinned.size)
        assertTrue(data.pinned.all { it.pinned }); assertTrue(data.recent.isEmpty())
    }
}
