package com.karthi.sungmobile.core.data

import com.karthi.sungmobile.core.data.convenience.ConvenienceRepository
import com.karthi.sungmobile.core.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ConvenienceRepositoryTest {
    @Test fun damagedSessionFileIsReportedAndNeverOverwrittenByAnotherSave() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val file = context.filesDir.resolve("saved_sessions_v1.json")
        val damaged = "[{broken session data"
        file.writeText(damaged)
        val repository = ConvenienceRepository(context)
        val track = CatalogItem("dQw4w9WgXcQ", CatalogKind.SONG, "Song", "Artist", "https://youtu.be/dQw4w9WgXcQ")
        val snapshot = QueueSnapshot(QueueState(listOf(QueueEntry("one", track)), "one"), 0)
        try {
            assertTrue(runCatching { repository.sessions() }.isFailure)
            assertTrue(runCatching { repository.save("New", snapshot, PlaybackSpeed()) }.isFailure)
            assertEquals(damaged, file.readText())
        } finally { file.delete() }
    }
    @Test fun duplicateQueueSessionAndResumeSurviveReopenWithoutChangingActiveQueue() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        context.filesDir.resolve("saved_sessions_v1.json").delete()
        context.filesDir.resolve("resume_points_v1.json").delete()
        val repository = ConvenienceRepository(context)
        val track = CatalogItem("https://www.youtube.com/watch?v=dQw4w9WgXcQ", CatalogKind.SONG,
            "Song", "Artist", "https://www.youtube.com/watch?v=dQw4w9WgXcQ", durationMs = 1_500_000)
        val entries = listOf(QueueEntry("first", track), QueueEntry("second", track))
        val snapshot = QueueSnapshot(QueueState(entries, "second", RepeatMode.ALL, true,
            listOf("second", "first")), 42_000)
        val saved = repository.save("Evening", snapshot, PlaybackSpeed(12, false))
        val reopened = ConvenienceRepository(context)
        assertEquals(snapshot, reopened.session(saved.id)?.snapshot)
        assertEquals(PlaybackSpeed(12, false), reopened.session(saved.id)?.speed)
        repository.recordResume("dQw4w9WgXcQ", 90_000)
        assertEquals(90_000L, reopened.resumePoint("dQw4w9WgXcQ")?.positionMs)
        reopened.clearResume("dQw4w9WgXcQ")
        assertNull(repository.resumePoint("dQw4w9WgXcQ"))
        reopened.delete(saved.id)
        assertTrue(repository.sessions().isEmpty())
    }
}
