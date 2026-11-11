package com.karthi.sungmobile.feature.streak

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karthi.sungmobile.core.model.library.ConsistencyStreakRepository
import com.karthi.sungmobile.core.model.library.StreakProgress
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StreakUiState(
    val progress: StreakProgress = StreakProgress(),
    val loading: Boolean = true,
    val error: Boolean = false,
)

class StreakViewModel(private val repository: ConsistencyStreakRepository) : ViewModel() {
    private val mutable = MutableStateFlow(StreakUiState())
    val state = mutable.asStateFlow()

    init {
        viewModelScope.launch { repository.changes.collectLatest { load() } }
    }

    fun retry() = viewModelScope.launch { load() }

    private suspend fun load() {
        mutable.update { it.copy(loading = true, error = false) }
        try {
            mutable.value = StreakUiState(progress = repository.progress(), loading = false)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            mutable.update { it.copy(loading = false, error = true) }
        }
    }
}
