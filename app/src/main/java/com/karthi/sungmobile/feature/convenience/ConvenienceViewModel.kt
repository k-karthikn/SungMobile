package com.karthi.sungmobile.feature.convenience

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karthi.sungmobile.core.data.convenience.ConvenienceRepository
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.QueueSnapshot
import com.karthi.sungmobile.core.model.ResumePoint
import com.karthi.sungmobile.core.model.SavedSession
import com.karthi.sungmobile.core.model.lyrics.lyricsVideoId
import com.karthi.sungmobile.core.playback.PlaybackState
import com.karthi.sungmobile.playback.PlaybackConnection
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ConvenienceUiState(
    val sessions: List<SavedSession> = emptyList(),
    val resume: ResumePoint? = null,
    val message: String? = null,
)

class ConvenienceViewModel(
    private val repository: ConvenienceRepository,
    private val playback: PlaybackConnection,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ConvenienceUiState())
    val state = mutableState.asStateFlow()
    private var resumeJob: Job? = null

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        runCatching { repository.sessions() }.onSuccess { mutableState.value = mutableState.value.copy(sessions = it) }
            .onFailure { mutableState.value = mutableState.value.copy(message = "Saved sessions could not be loaded.") }
    }

    fun loadResume(track: CatalogItem?, durationMs: Long) {
        resumeJob?.cancel()
        val id = track?.lyricsVideoId()
        mutableState.value = mutableState.value.copy(resume = null)
        val duration = durationMs.takeIf { it > 0 } ?: track?.durationMs ?: 0
        if (id == null || duration < 20 * 60_000) return
        resumeJob = viewModelScope.launch {
            val point = runCatching { repository.resumePoint(id) }.getOrNull()
            if (point != null && point.positionMs < duration - 30_000)
                mutableState.value = mutableState.value.copy(resume = point)
        }
    }

    fun resume() {
        val point = mutableState.value.resume ?: return
        playback.seekTo(point.positionMs)
        mutableState.value = mutableState.value.copy(resume = null)
    }

    fun startOver() {
        val point = mutableState.value.resume ?: return
        playback.seekTo(0)
        mutableState.value = mutableState.value.copy(resume = null)
        viewModelScope.launch {
            try { repository.clearResume(point.videoId) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutableState.value = mutableState.value.copy(message = "The saved resume point could not be cleared.") }
        }
    }

    fun save(name: String, state: PlaybackState) = viewModelScope.launch {
        runCatching { repository.save(name, QueueSnapshot(state.queue, state.engine.positionMs), state.speed) }
            .onSuccess { refresh(); mutableState.value = mutableState.value.copy(message = "Session saved.") }
            .onFailure { mutableState.value = mutableState.value.copy(message = it.message ?: "Session could not be saved.") }
    }

    fun rename(id: String, name: String) = viewModelScope.launch {
        runCatching { repository.rename(id, name) }.onSuccess { refresh() }
            .onFailure { mutableState.value = mutableState.value.copy(message = "Session could not be renamed.") }
    }

    fun delete(id: String) = viewModelScope.launch {
        runCatching { repository.delete(id) }.onSuccess { refresh() }
            .onFailure { mutableState.value = mutableState.value.copy(message = "Session could not be deleted.") }
    }

    fun restore(id: String, completed: (Boolean) -> Unit) {
        playback.restoreSession(id) { success ->
            if (!success) mutableState.value = mutableState.value.copy(message = "Session could not be restored.")
            completed(success)
        }
    }
    fun clearMessage() { mutableState.value = mutableState.value.copy(message = null) }
}
