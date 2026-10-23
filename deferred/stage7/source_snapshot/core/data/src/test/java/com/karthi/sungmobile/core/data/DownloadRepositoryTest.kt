package com.karthi.sungmobile.core.data

import androidx.room.Room
import com.karthi.sungmobile.core.data.db.SungDatabase
import com.karthi.sungmobile.core.data.device.DownloadRepository
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.CatalogKind
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DownloadRepositoryTest {
    @Test fun duplicateVideoSharesJobAndSignedUrlIsNeverStored() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val db = Room.inMemoryDatabaseBuilder(context, SungDatabase::class.java).build()
        try {
            val repo = DownloadRepository(context, db)
            val song = CatalogItem("one", CatalogKind.SONG, "Song", "Artist",
                "https://www.youtube.com/watch?v=RB-RcX5DS5A")
            assertEquals("RB-RcX5DS5A", repo.queue(song, null))
            assertEquals("RB-RcX5DS5A", repo.queue(song.copy(title = "Another occurrence"), null))
            assertEquals(1, repo.state.value.items.size)
            assertEquals("QUEUED", repo.state.value.items.single().state)
            assertFalse(repo.state.value.items.single().toString().contains("expire="))
            repo.update("RB-RcX5DS5A", "FAILED", error = "Network interrupted")
            assertEquals("FAILED", repo.row("RB-RcX5DS5A")!!.state)
            repo.remove("RB-RcX5DS5A", deleteFile = false)
            assertTrue(repo.state.value.items.isEmpty())
        } finally { db.close() }
    }
}
