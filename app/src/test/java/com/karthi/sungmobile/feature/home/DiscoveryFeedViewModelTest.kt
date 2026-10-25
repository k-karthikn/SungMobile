package com.karthi.sungmobile.feature.home

import androidx.lifecycle.ViewModelStore
import com.karthi.sungmobile.core.model.SourceException
import com.karthi.sungmobile.core.model.SourceFailure
import com.karthi.sungmobile.core.model.discovery.HomeFeedSource
import com.karthi.sungmobile.core.model.discovery.HomeShelf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DiscoveryFeedViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { store.clear(); Dispatchers.resetMain() }

    @Test fun returningToHomeUsesFreshRowsAndManualFailureRetainsThem() = runTest(dispatcher) {
        val rows = listOf(HomeShelf("one", "Provider row", emptyList()))
        var calls = 0
        var fail = false
        var now = 1_000_000L
        val source = object : HomeFeedSource {
            override suspend fun homeShelves(): List<HomeShelf> {
                calls++
                if (fail) throw SourceException(SourceFailure.NETWORK)
                return rows
            }
        }
        val model = DiscoveryFeedViewModel(source) { now }.also { store.put("feed", it) }
        runCurrent()
        assertEquals(rows, model.state.value.shelves)
        assertEquals(1, calls)
        model.ensureFresh(); runCurrent()
        assertEquals(1, calls)
        fail = true
        model.refresh(); runCurrent()
        assertEquals(rows, model.state.value.shelves)
        assertEquals(SourceFailure.NETWORK, model.state.value.error)
        fail = false
        now += 21 * 60_000
        model.ensureFresh(); runCurrent()
        assertEquals(3, calls)
        assertNull(model.state.value.error)
    }
}
