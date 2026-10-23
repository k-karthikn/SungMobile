package com.karthi.sungmobile.feature.lyrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karthi.sungmobile.core.data.lyrics.LyricsCache
import com.karthi.sungmobile.core.data.settings.LyricsSettings
import com.karthi.sungmobile.core.data.settings.SettingsRepository
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.lyrics.*
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
        val videoId = track.lyricsVideoId()
        if (videoId == null) { mutableState.value = LyricsUiState(videoId = track.id, message = "This song has no supported YouTube ID."); return }
        mutableState.value = LyricsUiState(videoId = track.id, loading = true)
        request = viewModelScope.launch {
            val offset = runCatching { settings.offset(videoId) }.getOrDefault(0)
            val bytes = runCatching { cache.bytes() }.getOrDefault(0)
            ensureActive()
            mutableState.update { it.copy(offsetMs = offset, cacheBytes = bytes) }
            var error: String? = null
            var youtube: LyricsDocument? = null
            try {
                if (!refresh && preferences.value.cacheEnabled) youtube = cache.get(videoId, LyricsOrigin.YOUTUBE)
                if (youtube == null) youtube = gateway.youtube(track)?.also {
                    ensureActive()
                    if (!privateMode && preferences.value.cacheEnabled) cache.put(videoId, it)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { error = "YouTube lyrics could not be loaded." }
            var lrclib: LyricsDocument? = null
            ensureActive()
            if (youtube?.synchronized != true && preferences.value.lrclibEnabled && !privateMode) {
                try {
                    if (!refresh && preferences.value.cacheEnabled) lrclib = cache.get(videoId, LyricsOrigin.LRCLIB)
                    if (lrclib == null) lrclib = gateway.lrclib(track)?.also {
                        ensureActive()
                        if (preferences.value.cacheEnabled) cache.put(videoId, it)
                    }
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { error = "Lyrics could not be loaded. Check your connection and retry." }
            }
            val document = selectLyrics(youtube, lrclib)
            val finalBytes = runCatching { cache.bytes() }.getOrDefault(bytes)
            ensureActive()
            if (current == generation) mutableState.value = LyricsUiState(track.id, false, document,
                if (document == null) error ?: "No lyrics available for this song." else null,
                offset, finalBytes)
        }
    }

    fun setPreferences(value: LyricsSettings) = viewModelScope.launch {
        settings.setLyrics(value)
        if (!value.cacheEnabled) { cache.clear(); mutableState.update { it.copy(cacheBytes = 0) } }
    }
    fun setOffset(track: CatalogItem, offsetMs: Int) {
        val videoId = track.lyricsVideoId() ?: return
        val clamped = offsetMs.coerceIn(-10_000, 10_000)
        mutableState.update { if (it.videoId == track.id) it.copy(offsetMs = clamped) else it }
        viewModelScope.launch { settings.setOffset(videoId, clamped) }
    }
    fun clearCache() {
        cancel()
        viewModelScope.launch { cache.clear(); mutableState.update { it.copy(cacheBytes = 0) } }
    }
}
