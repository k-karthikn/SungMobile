package com.karthi.sungmobile.feature.home

import androidx.lifecycle.ViewModelStore
import com.karthi.sungmobile.core.model.SourceException
import com.karthi.sungmobile.core.model.SourceFailure
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.CatalogKind
import com.karthi.sungmobile.core.model.RelatedSource
import com.karthi.sungmobile.core.model.discovery.HomeFeedSource
import com.karthi.sungmobile.core.model.discovery.HomeShelf
import com.karthi.sungmobile.core.model.discovery.*
import com.karthi.sungmobile.core.model.library.HistoryPolicy
import com.karthi.sungmobile.core.model.library.ListeningEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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

    @Test fun forYouIsAdditiveAndStopsWhenPrivateModeStarts() = runTest(dispatcher) {
        fun song(letter: Char): CatalogItem {
            val url = "https://www.youtube.com/watch?v=${letter.toString().repeat(11)}"
            return CatalogItem(url, CatalogKind.SONG, "Song $letter", "Creator $letter", url)
        }
        val originals = HomeShelf("existing", "Existing row", listOf(song('B'), song('C'), song('D')))
        val profile = MutableStateFlow(RecommendationProfile(liked = listOf(song('A'))))
        val preferences = MutableStateFlow(RecommendationPreferences())
        var relatedCalls = 0
        val settings = object : RecommendationSettingsStore {
            override val recommendations: Flow<RecommendationPreferences> = preferences
            override suspend fun setRecommendationsEnabled(enabled: Boolean) {
                preferences.value = preferences.value.copy(enabled = enabled)
            }
            override suspend fun setRecommendationFeedback(songKey: String, feedback: RecommendationFeedback?) {
                preferences.value = preferences.value.copy(feedback = preferences.value.feedback.toMutableMap().apply {
                    if (feedback == null) remove(songKey) else put(songKey, feedback)
                })
            }
            override suspend fun clearRecommendationFeedback() { preferences.value = preferences.value.copy(feedback = emptyMap()) }
        }
        val model = DiscoveryFeedViewModel(object : HomeFeedSource {
            override suspend fun homeShelves() = listOf(originals)
        }, object : RelatedSource {
            override suspend fun related(seed: CatalogItem, limit: Int): List<CatalogItem> {
                relatedCalls++
                return listOf(song('E'), song('F'))
            }
        }, object : RecommendationProfileSource { override fun observe() = profile }, settings) { 1_000_000L }
            .also { store.put("personalized", it) }
        runCurrent()
        assertEquals(listOf(originals), model.state.value.shelves)
        assertEquals("For you", model.state.value.personalizedShelf?.title)
        assertEquals(1, relatedCalls)
        model.setRecommendationFeedback(song('E'), RecommendationFeedback.HIDE)
        runCurrent()
        assertEquals(1, relatedCalls)
        assertFalse(model.state.value.personalizedShelf!!.items.contains(song('E')))
        profile.value = profile.value.copy(policy = HistoryPolicy(privateMode = true))
        runCurrent()
        assertNull(model.state.value.personalizedShelf)
        model.refresh(); runCurrent()
        assertEquals(1, relatedCalls)
        assertEquals(listOf(originals), model.state.value.shelves)
    }

    @Test fun historyOnlyUsesGeneralContinuationWithoutRelatedTrackRequests() = runTest(dispatcher) {
        fun song(letter: Char): CatalogItem {
            val url = "https://www.youtube.com/watch?v=${letter.toString().repeat(11)}"
            return CatalogItem(url, CatalogKind.SONG, "Song $letter", "Creator $letter", url)
        }
        val originals = HomeShelf("existing", "Existing row", listOf(song('B'), song('C'), song('D')))
        var relatedCalls = 0
        var generalCalls = 0
        val model = DiscoveryFeedViewModel(object : HomeFeedSource {
            override suspend fun homeShelves() = listOf(originals)
        }, object : RelatedSource {
            override suspend fun related(seed: CatalogItem, limit: Int): List<CatalogItem> {
                relatedCalls++
                return emptyList()
            }
        }, object : RecommendationProfileSource {
            override fun observe(): Flow<RecommendationProfile> = MutableStateFlow(RecommendationProfile(
                recentEvents = listOf(ListeningEvent("one", song('A'), 1, 2, 90_000))))
        }, object : RecommendationSettingsStore {
            override val recommendations: Flow<RecommendationPreferences> = MutableStateFlow(RecommendationPreferences())
            override suspend fun setRecommendationsEnabled(enabled: Boolean) = Unit
            override suspend fun setRecommendationFeedback(songKey: String, feedback: RecommendationFeedback?) = Unit
            override suspend fun clearRecommendationFeedback() = Unit
        }, object : HomeCandidateSource {
            override suspend fun extraHomeSongs(): List<CatalogItem> {
                generalCalls++
                return listOf(song('E'), song('F'))
            }
        }) { 1_000_000L }.also { store.put("history-only", it) }
        runCurrent()
        assertEquals(0, relatedCalls)
        assertEquals(1, generalCalls)
        assertEquals(listOf(originals), model.state.value.shelves)
        assertTrue(model.state.value.personalizedShelf!!.items.contains(song('E')))
    }
}
