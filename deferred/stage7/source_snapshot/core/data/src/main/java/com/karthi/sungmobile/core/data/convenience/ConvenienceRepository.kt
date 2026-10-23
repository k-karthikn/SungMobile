package com.karthi.sungmobile.core.data.convenience

import android.content.Context
import android.util.AtomicFile
import com.karthi.sungmobile.core.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** Atomic, bounded app-private snapshots. Independent of the active Room queue and library. */
class ConvenienceRepository(context: Context) {
    private val sessionsFile = AtomicFile(File(context.filesDir, "saved_sessions_v1.json"))
    private val resumeFile = AtomicFile(File(context.filesDir, "resume_points_v1.json"))
    private val lock = Mutex()

    suspend fun sessions(): List<SavedSession> = withContext(Dispatchers.IO) { lock.withLock { readSessions() } }
    suspend fun session(id: String): SavedSession? = sessions().firstOrNull { it.id == id }

    suspend fun save(name: String, snapshot: QueueSnapshot, speed: PlaybackSpeed): SavedSession =
        withContext(Dispatchers.IO) { lock.withLock {
            require(snapshot.queue.entries.isNotEmpty() && snapshot.queue.entries.size <= 500)
            require(name.trim().length in 1..80)
            require(snapshot.queue.entries.all { it.track.lyricsIdentityValid() })
            val existing = readSessions()
            require(existing.size < 20)
            val saved = SavedSession(UUID.randomUUID().toString(), name.trim(), System.currentTimeMillis(),
                snapshot.copy(positionMs = snapshot.positionMs.coerceAtLeast(0)), speed)
            write(sessionsFile, JSONArray().apply { (listOf(saved) + existing).forEach { put(it.toJson()) } })
            saved
        } }

    suspend fun rename(id: String, name: String) = withContext(Dispatchers.IO) { lock.withLock {
        require(name.trim().length in 1..80)
        val existing = readSessions()
        if (existing.any { it.id == id }) write(sessionsFile,
            JSONArray().apply { existing.forEach { put((if (it.id == id) it.copy(name = name.trim()) else it).toJson()) } })
    } }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) { lock.withLock {
        write(sessionsFile, JSONArray().apply { readSessions().filterNot { it.id == id }.forEach { put(it.toJson()) } })
    } }

    suspend fun resumePoint(videoId: String): ResumePoint? = withContext(Dispatchers.IO) { lock.withLock {
        if (!VIDEO_ID.matches(videoId)) return@withLock null
        readPoints().firstOrNull { it.videoId == videoId }
    } }

    suspend fun recordResume(videoId: String, positionMs: Long) = withContext(Dispatchers.IO) { lock.withLock {
        if (!VIDEO_ID.matches(videoId) || positionMs < 30_000) return@withLock
        val now = System.currentTimeMillis()
        val points = (listOf(ResumePoint(videoId, positionMs, now)) + readPoints().filterNot { it.videoId == videoId })
            .take(100)
        write(resumeFile, JSONArray().apply { points.forEach { point -> put(JSONObject().apply {
            put("id", point.videoId); put("position", point.positionMs); put("updated", point.updatedAtMs)
        }) } })
    } }

    suspend fun clearResume(videoId: String) = withContext(Dispatchers.IO) { lock.withLock {
        if (VIDEO_ID.matches(videoId)) write(resumeFile, JSONArray().apply {
            readPoints().filterNot { it.videoId == videoId }.forEach { point -> put(JSONObject().apply {
                put("id", point.videoId); put("position", point.positionMs); put("updated", point.updatedAtMs)
            }) }
        })
    } }

    private fun readSessions(): List<SavedSession> = read(sessionsFile).let { array ->
        (0 until minOf(array.length(), 20)).mapNotNull { index -> runCatching { array.getJSONObject(index).toSession() }.getOrNull() }
    }
    private fun readPoints(): List<ResumePoint> {
        val now = System.currentTimeMillis()
        val array = read(resumeFile)
        return (0 until minOf(array.length(), 100)).mapNotNull { index -> runCatching {
            val row = array.getJSONObject(index)
            ResumePoint(row.getString("id"), row.getLong("position"), row.getLong("updated"))
        }.getOrNull() }.filter { VIDEO_ID.matches(it.videoId) && it.positionMs >= 30_000 &&
            it.updatedAtMs in (now - 90L * 24 * 60 * 60 * 1000)..now }
    }

    private fun read(file: AtomicFile): JSONArray = runCatching {
        if (!file.baseFile.exists() || file.baseFile.length() > 2_000_000) JSONArray()
        else JSONArray(file.openRead().bufferedReader().use { it.readText() })
    }.getOrDefault(JSONArray())

    private fun write(file: AtomicFile, array: JSONArray) {
        val bytes = array.toString().toByteArray(Charsets.UTF_8)
        require(bytes.size <= 2_000_000)
        val stream = file.startWrite()
        try { stream.write(bytes); file.finishWrite(stream) }
        catch (error: Exception) { file.failWrite(stream); throw error }
    }

    private fun SavedSession.toJson() = JSONObject().apply {
        put("id", id); put("name", name); put("saved", savedAtMs)
        put("speed", speed.tenths); put("pitch", speed.preservePitch)
        put("position", snapshot.positionMs)
        val queue = snapshot.queue
        put("current", queue.currentId); put("repeat", queue.repeat.name); put("shuffled", queue.shuffled)
        put("original", JSONArray(queue.originalOrder))
        put("entries", JSONArray().apply { queue.entries.forEach { entry -> put(JSONObject().apply {
            put("id", entry.id); put("trackId", entry.track.id); put("title", entry.track.title)
            put("artist", entry.track.subtitle); put("url", entry.track.url)
            put("artwork", entry.track.artworkUrl); put("duration", entry.track.durationMs)
        }) } })
    }

    private fun JSONObject.toSession(): SavedSession {
        val rows = getJSONArray("entries")
        require(rows.length() in 1..500)
        val entries = (0 until rows.length()).map { index -> rows.getJSONObject(index).let { row ->
            QueueEntry(row.getString("id"), CatalogItem(row.getString("trackId"), CatalogKind.SONG,
                row.getString("title").take(500), row.getString("artist").take(500), row.getString("url"),
                row.optString("artwork").takeIf { it.isNotBlank() && it != "null" },
                row.optLong("duration", -1).takeIf { it > 0 }))
        } }
        require(entries.all { it.id.isNotBlank() && it.track.lyricsIdentityValid() })
        val original = getJSONArray("original").let { array -> (0 until array.length()).map(array::getString) }
        val queue = QueueState(entries, getString("current"), RepeatMode.valueOf(getString("repeat")),
            getBoolean("shuffled"), original)
        val name = getString("name")
        require(name.length in 1..80)
        return SavedSession(getString("id"), name, getLong("saved"),
            QueueSnapshot(queue, getLong("position").coerceAtLeast(0)),
            PlaybackSpeed(getInt("speed"), getBoolean("pitch")))
    }

    private fun CatalogItem.lyricsIdentityValid() =
        if (source == TrackSource.DEVICE) url.length <= 2_000 && url.substringAfter("content://").contains('/')
        else runCatching { YouTubeLink.parse(url) is YouTubeLink.Song }.getOrDefault(false)

    private companion object { val VIDEO_ID = Regex("[A-Za-z0-9_-]{11}") }
}
