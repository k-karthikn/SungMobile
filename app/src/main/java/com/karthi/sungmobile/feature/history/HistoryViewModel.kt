package com.karthi.sungmobile.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karthi.sungmobile.core.model.CatalogKind
import com.karthi.sungmobile.core.model.library.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

class HistoryViewModel(private val repository: HistoryRepository) : ViewModel() {
    val policy = repository.policy().stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val searches = repository.searches().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val mutableError = MutableStateFlow(false)
    val error = mutableError.asStateFlow()
    private val commands = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    init { viewModelScope.launch { for (command in commands) try { command(); mutableError.value = false }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { mutableError.value = true }
    } }
    fun setting(setting: HistorySetting, enabled: Boolean) { commands.trySend { repository.setPolicy(setting, enabled) } }
    fun clearListening() { commands.trySend { repository.clearListening() } }
    fun clearSearches() { commands.trySend { repository.clearSearches() } }
    fun removeSearch(id: String) { commands.trySend { repository.removeSearch(id) } }
    fun submitted(query: String, kind: CatalogKind) {
        // Capture at submission; a later privacy change must never authorize this old write.
        val captured = policy.value ?: return
        commands.trySend { repository.recordSearch(query, kind, captured) }
    }
}
