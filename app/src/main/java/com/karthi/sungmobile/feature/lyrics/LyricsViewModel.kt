package com.karthi.sungmobile.feature.lyrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karthi.sungmobile.core.data.lyrics.LyricsCache
import com.karthi.sungmobile.core.data.settings.LyricsSettings
import com.karthi.sungmobile.core.data.settings.SettingsRepository
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.lyrics.*
import com.karthi.sungmobile.feature.feedback.DiagnosticLog
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class LyricsUiState(
    val videoId: String? = null,
    val loading: Boolean = false,
    val document: LyricsDocument? = null,
    val message: String? = null,
    val offsetMs: Int = 0,
    val cacheBytes: Long = 0,
)

class LyricsViewModel(
    private val gateway: LyricsGateway,
    private val cache: LyricsCache,
    private val settings: SettingsRepository,
    private val local: LocalLyricsSource = LocalLyricsSource { null },
) : ViewModel() {
    val preferences = settings.lyrics.stateIn(viewModelScope, SharingStarted.Eagerly, LyricsSettings())
    private val mutableState = MutableStateFlow(LyricsUiState())
    val state = mutableState.asStateFlow()
    private var request: Job? = null
    private var generation = 0

    init { viewModelScope.launch { mutableState.update { it.copy(cacheBytes = runCatching { cache.bytes() }.getOrDefault(0)) } } }

    fun cancel() {
        generation++
        request?.cancel()
        request = null
        mutableState.update { it.copy(loading = false) }
    }

    fun load(track: CatalogItem?, privateMode: Boolean, refresh: Boolean = false) {
        request?.cancel()
        val current = ++generation
        if (track == null) { mutableState.value = LyricsUiState(); return }
        val key = track.lyricsStorageKey()
        if (key == null) { mutableState.value = LyricsUiState(videoId = track.id, message = "Lyrics are unavailable for this song."); return }
        mutableState.value = LyricsUiState(videoId = track.id, loading = true)
        request = viewModelScope.launch {
            val offset = runCatching { settings.offset(key) }.getOrDefault(0)
            val bytes = runCatching { cache.bytes() }.getOrDefault(0)
            ensureActive()
            mutableState.update { it.copy(offsetMs = offset, cacheBytes = bytes) }
            var error: String? = null
            var primary: LyricsDocument? = null
            if (track.isLocalSong()) {
                try { primary = local.read(track) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (failure: Exception) {
                    DiagnosticLog.recordError("local_lyrics_failed", failure)
                    error = "Lyrics in this file could not be read."
                }
            } else {
                try {
                    if (!refresh && preferences.value.cacheEnabled) primary = cache.get(key, LyricsOrigin.YOUTUBE)
                    if (primary == null) primary = gateway.youtube(track)?.also {
                        ensureActive()
                        if (!privateMode && preferences.value.cacheEnabled) cache.put(key, it)
                    }
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (failure: Exception) {
                    DiagnosticLog.recordError("youtube_lyrics_failed", failure)
                    error = "YouTube lyrics could not be loaded."
                }
            }
            var lrclib: LyricsDocument? = null
            ensureActive()
            if (primary?.synchronized != true && preferences.value.lrclibEnabled && !privateMode) {
                try {
                    if (!refresh && preferences.value.cacheEnabled) lrclib = cache.get(key, LyricsOrigin.LRCLIB)
                    if (lrclib == null) lrclib = gateway.lrclib(track)?.also {
                        ensureActive()
                        if (preferences.value.cacheEnabled) cache.put(key, it)
                    }
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (failure: Exception) {
                    DiagnosticLog.recordError("lrclib_lyrics_failed", failure)
                    error = "Lyrics could not be loaded. Check your connection and retry."
                }
            }
            val document = selectLyrics(primary, lrclib)
            val finalBytes = runCatching { cache.bytes() }.getOrDefault(bytes)
            ensureActive()
            if (current == generation) mutableState.value = LyricsUiState(track.id, false, document,
                if (document == null) error ?: when {
                    track.isLocalSong() && !preferences.value.lrclibEnabled ->
                        "No lyrics found in this file. You can also search LRCLIB online."
                    track.isLocalSong() && privateMode ->
                        "No lyrics found in this file. Private mode skips online lookup."
                    else -> "No lyrics available for this song."
                } else null,
                offset, finalBytes)
        }
    }

    fun setPreferences(value: LyricsSettings) = viewModelScope.launch {
        settings.setLyrics(value)
        if (!value.cacheEnabled) { cache.clear(); mutableState.update { it.copy(cacheBytes = 0) } }
    }
    fun setOffset(track: CatalogItem, offsetMs: Int) {
        val key = track.lyricsStorageKey() ?: return
        val clamped = offsetMs.coerceIn(-10_000, 10_000)
        mutableState.update { if (it.videoId == track.id) it.copy(offsetMs = clamped) else it }
        viewModelScope.launch { settings.setOffset(key, clamped) }
    }
    fun clearCache() {
        cancel()
        viewModelScope.launch { cache.clear(); mutableState.update { it.copy(cacheBytes = 0) } }
    }
}
