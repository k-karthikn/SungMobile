package com.karthi.sungmobile.feature.home

import androidx.lifecycle.*
import com.karthi.sungmobile.core.model.discovery.*
import com.karthi.sungmobile.testing.testSong
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { store.clear(); Dispatchers.resetMain() }
    @Test fun privateModeAndClearInvalidateRecentMixWithoutRemovingDeliberateLikes() = runTest(dispatcher) {
        val data = MutableStateFlow(HomeData(liked = listOf(testSong), recent = listOf(testSong)))
        val repository = object : HomeRepository { override fun observe() = data }
        val saved = SavedStateHandle(mapOf("seed" to 42L))
        val model = HomeViewModel(repository, saved).also { store.put("home", it) }; runCurrent()
        assertEquals(listOf(testSong), model.state.value.recentMix)
        data.value = data.value.copy(privateMode = true); runCurrent()
        assertTrue(model.state.value.recentMix.isEmpty()); assertEquals(listOf(testSong), model.state.value.likedMix)
        data.value = data.value.copy(privateMode = false, recent = emptyList(), historyEpoch = 1); runCurrent()
        assertTrue(model.state.value.recentMix.isEmpty())
        model.refresh(); runCurrent(); assertEquals(43L, saved.get<Long>("seed"))
    }
}
