package com.karthi.sungmobile.feature.home

import androidx.lifecycle.*
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.discovery.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class HomeState(val data: HomeData = HomeData(), val likedMix: List<CatalogItem> = emptyList(),
    val recentMix: List<CatalogItem> = emptyList(), val loading: Boolean = true, val error: Boolean = false)
class HomeViewModel(private val repository: HomeRepository, private val saved: SavedStateHandle) : ViewModel() {
    private val mutable = MutableStateFlow(HomeState())
    val state = mutable.asStateFlow()
    private val seed = MutableStateFlow(saved.get<Long>("seed") ?: (System.currentTimeMillis() / 86_400_000))
    private var observation: Job? = null
    init { observe() }
    private fun observe() {
        observation?.cancel()
        observation = viewModelScope.launch {
            try {
                combine(repository.observe(), seed) { data, seed ->
                    HomeState(data, localMix(data.liked, seed), if (data.privateMode) emptyList() else localMix(data.recent, seed + data.historyEpoch), false)
                }.collect { mutable.value = it }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutable.value = mutable.value.copy(data = mutable.value.data.copy(recent = emptyList()), recentMix = emptyList(), loading = false, error = true) }
        }
    }
    fun refresh() { seed.value++; saved["seed"] = seed.value; observe() }
}
