package com.karthi.sungmobile.core.data

import android.database.sqlite.SQLiteDatabase
import com.karthi.sungmobile.core.data.db.DatabaseProvider
import com.karthi.sungmobile.core.data.library.RoomLibraryRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OnlineOnlyMigrationTest {
    @Test fun stage7DatabaseOpensWithoutLosingEarlierLibraryOrDeferredTables() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val name = "stage7-to-online-only.db"
        context.deleteDatabase(name)
        val file = context.getDatabasePath(name).apply { parentFile!!.mkdirs() }
        val schema = JSONObject(javaClass.getResource("/stage7-schema.json")!!.readText()).getJSONObject("database")
        SQLiteDatabase.openOrCreateDatabase(file, null).use { old ->
            val entities = schema.getJSONArray("entities")
            repeat(entities.length()) { i ->
                val entity = entities.getJSONObject(i)
                old.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
                val indices = entity.optJSONArray("indices")
                if (indices != null) repeat(indices.length()) { index ->
                    old.execSQL(indices.getJSONObject(index).getString("createSql")
                        .replace("\${TABLE_NAME}", entity.getString("tableName")))
                }
            }
            old.execSQL("INSERT INTO library_tracks VALUES ('youtube:RB-RcX5DS5A','Track','Artist','https://www.youtube.com/watch?v=RB-RcX5DS5A',NULL,120000)")
            old.execSQL("INSERT INTO library_likes VALUES ('youtube:RB-RcX5DS5A',123)")
            old.execSQL("INSERT INTO library_tracks VALUES ('device:content://media/42','Local','Artist','content://media/42',NULL,90000)")
            old.execSQL("INSERT INTO library_likes VALUES ('device:content://media/42',124)")
            old.execSQL("INSERT INTO device_folders VALUES ('content://test/tree/Music','Music','INCLUDE')")
            old.version = 3
        }
        val db = DatabaseProvider.open(context, name)
        try {
            assertEquals(setOf("youtube:RB-RcX5DS5A"), RoomLibraryRepository(db).likedKeys().first())
            assertEquals(1, RoomLibraryRepository(db).songs(com.karthi.sungmobile.core.model.library.LibraryCollection.Likes,
                "", com.karthi.sungmobile.core.model.library.LibrarySort.DEFAULT, 50, 0).size)
            db.openHelper.readableDatabase.query("SELECT name FROM sqlite_master WHERE name='device_folders'").use {
                assertTrue(it.moveToFirst())
            }
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
