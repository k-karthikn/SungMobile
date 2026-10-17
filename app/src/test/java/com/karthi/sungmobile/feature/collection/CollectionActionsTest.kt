package com.karthi.sungmobile.feature.collection

import androidx.lifecycle.ViewModelStore
import com.karthi.sungmobile.core.model.*
import com.karthi.sungmobile.testing.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionActionsTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val player = RecordingPlayer()
    private val first = CatalogPage("Mix", listOf(testSong), PageToken("next"))
    private fun details(page: CatalogPage = first) = CollectionState(CollectionRoute("url", "Mix"), page)
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { store.clear(); Dispatchers.resetMain() }
    private fun model(source: CatalogSource) = CollectionActionsViewModel(source, player).also { store.put("operations", it) }

    @Test fun playbackRemainsUntouchedUntilAllPagesArriveThenCommitsOnce() = runTest(dispatcher) {
        val pending = CompletableDeferred<CatalogPage>()
        val model = model(object : TestCatalog() { override suspend fun nextPage(token: PageToken) = pending.await() })
        model.begin(details(), CollectionAction.PLAY); runCurrent()
        assertTrue(player.batches.isEmpty()); assertTrue(model.state.value!!.loading)
        pending.complete(CatalogPage("Mix", listOf(testSong))); runCurrent()
        assertEquals(listOf(listOf(testSong, testSong)), player.batches)
        assertEquals(2, player.state.value.queue.entries.map { it.id }.distinct().size)
        assertNull(model.state.value)
    }
    @Test fun cancellationAndNewQueueIntentRejectUncancellableLateResults() = runTest(dispatcher) {
        for (changeQueue in listOf(false, true)) {
            val pending = CompletableDeferred<CatalogPage>()
            val model = model(object : TestCatalog() {
                override suspend fun nextPage(token: PageToken) = withContext(NonCancellable) { pending.await() }
            })
            model.begin(details(), CollectionAction.PLAY); runCurrent()
            if (changeQueue) player.select(listOf(testSong.copy(title = "New choice")), 0) else model.cancel()
            runCurrent(); val commits = player.batches.size
            pending.complete(CatalogPage("Mix", listOf(testSong))); runCurrent()
            assertEquals(commits, player.batches.size)
            if (changeQueue) assertNotNull(model.state.value?.message) else assertNull(model.state.value)
        }
    }
    @Test fun partialCollectionRequiresExplicitConsentAndSaveDoesNotPlay() = runTest(dispatcher) {
        val model = model(TestCatalog())
        model.begin(details(first.copy(next = null, partial = true)), CollectionAction.SAVE); runCurrent()
        assertTrue(player.batches.isEmpty()); assertNull(model.save.value)
        model.commit(); assertEquals(listOf(testSong), model.save.value)
        assertTrue(player.batches.isEmpty()); model.savedHandled(); assertNull(model.save.value)
    }
    @Test fun failedPageRetriesAndShuffleHappensOnlyAtCommit() = runTest(dispatcher) {
        var attempts = 0
        val model = model(object : TestCatalog() {
            override suspend fun nextPage(token: PageToken): CatalogPage {
                if (++attempts == 1) throw SourceException(SourceFailure.NETWORK)
                return CatalogPage("Mix", listOf(testSong))
            }
        })
        model.begin(details(), CollectionAction.SHUFFLE); runCurrent()
        assertTrue(player.batches.isEmpty()); assertEquals(1, model.state.value!!.count)
        model.retry(); runCurrent()
        assertTrue(player.shuffled); assertEquals(1, player.batches.size)
    }
    @Test fun timestampNeedsExplicitPlayAndDisconnectedPlayerDoesNotDropActionSilently() = runTest(dispatcher) {
        val model = model(TestCatalog())
        val details = details(first.copy(next = null)).copy(route = CollectionRoute("url", "Song", startMs = 12_000))
        assertTrue(player.batches.isEmpty())
        model.begin(details, CollectionAction.PLAY); runCurrent(); assertEquals(12_000L, player.startMs)
        player.state.value = player.state.value.copy(connected = false)
        model.begin(details, CollectionAction.APPEND); runCurrent()
        assertNotNull(model.state.value?.message); assertEquals(1, player.batches.size)
    }
}
