package com.karthi.sungmobile.core.data.local

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Keeps the last scan in memory so switching Library tabs does not walk SAF trees again. */
class LocalAudioRepository(private val context: Context) {
    private val resolver get() = context.contentResolver
    private val preferences = context.getSharedPreferences("local_audio_folders", Context.MODE_PRIVATE)
    private val artwork = LocalArtworkRepository(context)
    private val scanner = LocalAudioScanner(context, artwork)
    private val artworkScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var artworkJob: Job? = null
    private val scanMutex = Mutex()
    @Volatile private var cachedScan: LocalScan? = null
    @Volatile private var scannedAtMs = 0L
    @Volatile private var revision = 0L
    @Volatile private var sidecarsDirty = false
    private val mutableUpdates = MutableStateFlow<LocalScan?>(null)
    val updates = mutableUpdates.asStateFlow()

    fun snapshot(): LocalScan? = cachedScan
    fun invalidate() { revision++; artworkJob?.cancel(); cachedScan = null }

    fun hasAudioPermission(): Boolean = scanner.hasAudioPermission()

    fun audioPermission(): String = scanner.audioPermission()

    fun addedFolders(): List<Uri> = preferences.getStringSet("trees", emptySet()).orEmpty()
        .mapNotNull { runCatching { Uri.parse(it) }.getOrNull() }.sortedBy(Uri::toString)

    fun addFolder(uri: Uri) {
        resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        preferences.edit().putStringSet("trees", addedFolders().map(Uri::toString).toSet() + uri.toString()).apply()
        sidecarsDirty = true
        invalidate()
    }

    fun removeFolder(uri: Uri) {
        preferences.edit().putStringSet("trees", addedFolders().map(Uri::toString).toSet() - uri.toString()).apply()
        runCatching { resolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        sidecarsDirty = true
        invalidate()
    }

    suspend fun scan(force: Boolean = false): LocalScan = withContext(Dispatchers.IO) { scanMutex.withLock {
        val now = SystemClock.elapsedRealtime()
        cachedScan?.takeIf { !force && now - scannedAtMs < SCAN_FRESHNESS_MS }?.let { return@withLock it }
        val result = scanner.scan(addedFolders())
        Log.i(LOCAL_AUDIO_TAG, "Device scan found ${result.songs.size} audio tracks; permissionNeeded=${result.needsAudioPermission}")
        val recheckSidecars = force || sidecarsDirty
        sidecarsDirty = false
        result.also { scan ->
            val generation = ++revision
            artworkJob?.cancel()
            cachedScan = scan
            mutableUpdates.value = scan
            scannedAtMs = SystemClock.elapsedRealtime()
            artworkJob = artworkScope.launch { enrichArtwork(scan, generation, recheckSidecars) }
        }
    } }

    private suspend fun enrichArtwork(initial: LocalScan, generation: Long, recheckSidecars: Boolean) {
        val songs = initial.songs.toMutableList()
        var changed = 0
        var extracted = 0
        var attempted = 0
        var withoutArtwork = 0
        songs.forEachIndexed { index, song ->
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            if (revision != generation) return
            val recheck = recheckSidecars && song.sidecarArt != null
            if (song.track.artworkUrl != null && !recheck) return@forEachIndexed
            attempted++
            val audio = Uri.parse(song.track.url)
            val art = try {
                artwork.resolve(audio, song.sidecarArt, recheck)
            } catch (error: Exception) {
                Log.w(LOCAL_AUDIO_TAG, "Cover extraction failed for ${audio.scheme}://${audio.authority}", error)
                null
            }
            if (art == null) {
                withoutArtwork++
                return@forEachIndexed
            }
            if (revision != generation) return
            songs[index] = song.copy(track = song.track.copy(artworkUrl = art))
            changed++
            extracted++
            if (changed >= 8) {
                publishArtwork(initial, songs)
                changed = 0
            }
        }
        if (changed > 0 && revision == generation) publishArtwork(initial, songs)
        Log.i(LOCAL_AUDIO_TAG, "Cover extraction finished: $extracted extracted, $withoutArtwork unavailable, $attempted attempted")
    }

    private fun publishArtwork(initial: LocalScan, songs: List<LocalSong>) {
        val updated = initial.copy(songs = songs.toList())
        cachedScan = updated
        mutableUpdates.value = updated
    }

    private companion object {
        const val LOCAL_AUDIO_TAG = "SungLocalAudio"
        const val SCAN_FRESHNESS_MS = 60_000L
    }
}
