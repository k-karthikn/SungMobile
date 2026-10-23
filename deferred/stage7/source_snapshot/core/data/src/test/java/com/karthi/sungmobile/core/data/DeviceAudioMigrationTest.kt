package com.karthi.sungmobile.core.data

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import com.karthi.sungmobile.core.data.db.DeviceAudioMigration
import com.karthi.sungmobile.core.data.db.SungDatabase
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DeviceAudioMigrationTest {
    @Test fun upgradeFromAcceptedStage6PreservesLibraryAndAddsDeviceTables() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val name = "stage7-migration-test.db"
        context.deleteDatabase(name)
        val file = context.getDatabasePath(name).apply { parentFile!!.mkdirs() }
        val schema = JSONObject(File("schemas/com.karthi.sungmobile.core.data.db.SungDatabase/2.json")
            .readText()).getJSONObject("database")
        SQLiteDatabase.openOrCreateDatabase(file, null).use { old ->
            val entities = schema.getJSONArray("entities")
            repeat(entities.length()) { index ->
                val entity = entities.getJSONObject(index)
                old.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
                entity.optJSONArray("indices")?.let { indices -> repeat(indices.length()) { i ->
                    old.execSQL(indices.getJSONObject(i).getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
                } }
            }
            old.execSQL("INSERT INTO library_tracks VALUES ('youtube:RB-RcX5DS5A','Existing song','Artist','https://www.youtube.com/watch?v=RB-RcX5DS5A',NULL,120000)")
            old.execSQL("INSERT INTO library_likes VALUES ('youtube:RB-RcX5DS5A',100)")
            old.version = 2
        }
        val db = Room.databaseBuilder(context, SungDatabase::class.java, name)
            .addMigrations(DeviceAudioMigration).build()
        try {
            withContext(Dispatchers.IO) {
                assertEquals("Existing song", db.libraryDao().track("youtube:RB-RcX5DS5A")!!.title)
                assertEquals(0, db.deviceAudioDao().downloads().size)
                assertEquals(0, db.deviceAudioDao().tracks().size)
            }
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
