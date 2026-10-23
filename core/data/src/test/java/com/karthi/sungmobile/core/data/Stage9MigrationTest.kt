package com.karthi.sungmobile.core.data

import android.database.sqlite.SQLiteDatabase
import com.karthi.sungmobile.core.data.db.DatabaseProvider
import com.karthi.sungmobile.core.data.library.RoomAdvancedLibraryRepository
import com.karthi.sungmobile.core.data.library.RoomLibraryRepository
import com.karthi.sungmobile.core.model.library.LibraryCollection
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class Stage9MigrationTest {
    @Test fun code11DatabaseUpgradesWithoutLosingLocalData() = verifyUpgrade(2)
    @Test fun code14DatabaseUpgradesWithoutLosingLocalData() = verifyUpgrade(4)

    // Schemas 2 and 4 have identical active table/index definitions; 4 excluded the deferred Stage 7 tables.
    private fun verifyUpgrade(version: Int) = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val name = "schema-$version-to-stage9.db"
        context.deleteDatabase(name)
        val file = context.getDatabasePath(name).apply { parentFile!!.mkdirs() }
        val schema = JSONObject(javaClass.getResource("/stage8-schema.json")!!.readText()).getJSONObject("database")
        SQLiteDatabase.openOrCreateDatabase(file, null).use { old ->
            val entities = schema.getJSONArray("entities")
            repeat(entities.length()) { i ->
                val entity = entities.getJSONObject(i)
                old.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
                entity.optJSONArray("indices")?.let { indices -> repeat(indices.length()) { j ->
                    old.execSQL(indices.getJSONObject(j).getString("createSql")
                        .replace("\${TABLE_NAME}", entity.getString("tableName")))
                } }
            }
            old.execSQL("INSERT INTO library_tracks VALUES ('youtube:RB-RcX5DS5A','Yellow','Coldplay','https://www.youtube.com/watch?v=RB-RcX5DS5A',NULL,120000)")
            old.execSQL("INSERT INTO library_likes VALUES ('youtube:RB-RcX5DS5A',123)")
            old.execSQL("INSERT INTO local_playlists VALUES ('saved','Old playlist',0,100,100)")
            old.execSQL("INSERT INTO playlist_entries VALUES ('occurrence','saved','youtube:RB-RcX5DS5A',0)")
            old.execSQL("INSERT INTO listening_events VALUES ('pass:0','pass','youtube:RB-RcX5DS5A',0,30000,1,31000,1)")
            old.version = version
        }
        val db = DatabaseProvider.open(context, name)
        try {
            val library = RoomLibraryRepository(db)
            val advanced = RoomAdvancedLibraryRepository(db)
            assertEquals("Old playlist", library.playlist("saved")!!.name)
            assertEquals("occurrence", library.songs(LibraryCollection.Playlist("saved")).single().rowId)
            assertEquals(1, library.songs(LibraryCollection.Likes).size)
            assertEquals(1L, advanced.statistics().qualifiedPlays)
            assertTrue(advanced.smartPlaylists().isEmpty())
            assertTrue(advanced.revisions("saved").isEmpty())
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
