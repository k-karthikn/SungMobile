package com.karthi.sungmobile.core.data

import androidx.room.Room
import com.karthi.sungmobile.core.data.db.*
import com.karthi.sungmobile.core.data.queue.RoomQueueStore
import com.karthi.sungmobile.core.model.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RoomQueueStoreTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private lateinit var database: SungDatabase
    @Before fun setup() { context.deleteDatabase("queue-test.db"); database = open() }
    @After fun cleanup() { database.close(); context.deleteDatabase("queue-test.db") }
    private fun open() = Room.databaseBuilder(context, SungDatabase::class.java, "queue-test.db").build()
    private fun snapshot(): QueueSnapshot {
        val song = CatalogItem("video", CatalogKind.SONG, "தமிழ் title", "Artist", "https://www.youtube.com/watch?v=video", "https://i.ytimg.com/vi/video/mqdefault.jpg", 120_000)
        val entries = listOf(QueueEntry("second", song), QueueEntry("first", song))
        return QueueSnapshot(QueueState(entries, "second", RepeatMode.ALL, true, listOf("first", "second")), 42_000)
    }
    @Test fun duplicatesOrderAndPositionSurviveDatabaseReopening() = runBlocking {
        val original = snapshot()
        RoomQueueStore(database).write(original)
        database.close(); database = open()
        assertEquals(original, RoomQueueStore(database).read())
    }
    @Test fun emptyQueueReplacesPreviousRows() = runBlocking {
        val store = RoomQueueStore(database)
        store.write(snapshot()); store.write(QueueSnapshot())
        assertEquals(QueueSnapshot(), store.read())
    }
    @Test fun failedTransactionRetainsPreviousQueueAndPosition() = runBlocking {
        val store = RoomQueueStore(database)
        val original = snapshot(); store.write(original)
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val duplicate = QueueEntryEntity().apply { entryId = "duplicate" }
            try { database.queueDao().replace(QueueHeaderEntity(), listOf(duplicate, duplicate)); fail("Expected duplicate primary-key failure") }
            catch (_: android.database.sqlite.SQLiteConstraintException) { }
        }
        assertEquals(original, store.read())
    }
    @Test fun firstLaunchHasAnEmptyQueue() = runBlocking { assertEquals(QueueSnapshot(), RoomQueueStore(database).read()) }
}
