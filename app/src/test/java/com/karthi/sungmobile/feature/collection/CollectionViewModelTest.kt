package com.karthi.sungmobile.feature.collection

import androidx.lifecycle.*
import com.karthi.sungmobile.core.model.*
import com.karthi.sungmobile.testing.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { store.clear(); Dispatchers.resetMain() }
    @Test fun paginationPreservesDuplicateOccurrenceIdsAndRefreshReplacesThem() = runTest(dispatcher) {
        val source = object : TestCatalog() {
            override suspend fun open(url: String) = CatalogPage("Mix", listOf(testSong), PageToken("next"))
            override suspend fun nextPage(token: PageToken) = CatalogPage("Mix", listOf(testSong))
        }
        val model = CollectionViewModel(source, SavedStateHandle()).also { store.put("details", it) }
        model.open(CollectionRoute("url", "Mix")); runCurrent()
        val firstId = model.state.value.entries.single().id
        model.more(); runCurrent()
        assertEquals(2, model.state.value.entries.size)
        assertEquals(firstId, model.state.value.entries.first().id)
        assertNotEquals(firstId, model.state.value.entries.last().id)
        model.refresh(); runCurrent(); assertNotEquals(firstId, model.state.value.entries.single().id)
    }
    @Test fun closingRejectsLateResponseAndRestorationUsesOnlyDurableRoute() = runTest(dispatcher) {
        val saved = SavedStateHandle()
        val pending = CompletableDeferred<CatalogPage>()
        val source = object : TestCatalog() { override suspend fun open(url: String) = withContext(NonCancellable) { pending.await() } }
        val model = CollectionViewModel(source, saved).also { store.put("details", it) }
        model.open(CollectionRoute("url", "Mix", startMs = 5000)); runCurrent()
        assertEquals(setOf("url", "title", "kind", "start"), saved.keys())
        val restored = CollectionViewModel(TestCatalog(), SavedStateHandle(saved.keys().associateWith { saved.get<Any?>(it) }))
            .also { store.put("restored", it) }
        runCurrent(); assertEquals(5000L, restored.state.value.route?.startMs)
        model.close(); pending.complete(CatalogPage("Old", listOf(testSong))); runCurrent()
        assertNull(model.state.value.route); assertNull(model.state.value.page); assertFalse(saved.contains("url"))
    }
    @Test fun failedRefreshRetriesRefreshEvenWhenOldPageHasNoContinuation() = runTest(dispatcher) {
        var opens = 0
        val source = object : TestCatalog() {
            override suspend fun open(url: String): CatalogPage {
                if (++opens == 2) throw SourceException(SourceFailure.NETWORK)
                return CatalogPage("Revision $opens", listOf(testSong))
            }
        }
        val model = CollectionViewModel(source, SavedStateHandle()).also { store.put("details", it) }
        model.open(CollectionRoute("url", "Mix")); runCurrent()
        model.refresh(); runCurrent(); assertEquals(SourceFailure.NETWORK, model.state.value.error)
        model.retry(); runCurrent(); assertEquals("Revision 3", model.state.value.page?.title)
        assertNull(model.state.value.error)
    }

}
