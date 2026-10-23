package com.karthi.sungmobile.core.data

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import com.karthi.sungmobile.core.data.db.*
import com.karthi.sungmobile.core.data.queue.RoomQueueStore
import com.karthi.sungmobile.core.data.library.RoomLibraryRepository
import com.karthi.sungmobile.core.model.RepeatMode
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LibraryMigrationTest {
    @Test fun upgradeValidatesSchemaAndPreservesStage2Queue() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val name = "migration-test.db"
        context.deleteDatabase(name)
        val file = context.getDatabasePath(name).apply { parentFile!!.mkdirs() }
        val schema = JSONObject(javaClass.getResource("/stage2-schema.json")!!.readText()).getJSONObject("database")
        SQLiteDatabase.openOrCreateDatabase(file, null).use { old ->
            val entities = schema.getJSONArray("entities")
            repeat(entities.length()) { i ->
                val entity = entities.getJSONObject(i)
                old.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
            }
            old.execSQL("INSERT INTO queue_header (id,currentId,repeatMode,shuffled,positionMs) VALUES (1,'b','ALL',1,42000)")
            listOf("b", "a").forEachIndexed { index, id -> old.execSQL(
                "INSERT INTO queue_entries VALUES (?,?,?,'RB-RcX5DS5A','Track','Artist','https://www.youtube.com/watch?v=RB-RcX5DS5A',NULL,120000)",
                arrayOf<Any>(id, index, 1-index)) }
            old.version = 1
        }
        val db = DatabaseProvider.open(context, name)
        try {
            val queue = RoomQueueStore(db).read()
            assertEquals(listOf("b", "a"), queue.queue.entries.map { it.id })
            assertEquals(listOf("a", "b"), queue.queue.originalOrder)
            assertEquals("b", queue.queue.currentId)
            assertEquals(RepeatMode.ALL, queue.queue.repeat)
            assertTrue(queue.queue.shuffled)
            assertEquals(42_000L, queue.positionMs)
            assertNull(queue.passId)
            val library = RoomLibraryRepository(db)
            val id = library.create("After upgrade", queue.queue.entries.map { it.track })
            assertEquals(2, library.playlist(id)!!.songCount)
            assertEquals(queue, RoomQueueStore(db).read())
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
