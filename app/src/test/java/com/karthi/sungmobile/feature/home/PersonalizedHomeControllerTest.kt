package com.karthi.sungmobile.feature.home

import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.CatalogKind
import com.karthi.sungmobile.core.model.RelatedSource
import com.karthi.sungmobile.core.model.discovery.HomeShelf
import com.karthi.sungmobile.core.model.discovery.RecommendationFeedback
import com.karthi.sungmobile.core.model.discovery.RecommendationPreferences
import com.karthi.sungmobile.core.model.discovery.RecommendationProfile
import com.karthi.sungmobile.core.model.discovery.RecommendationProfileSource
import com.karthi.sungmobile.core.model.discovery.RecommendationSettingsStore
import com.karthi.sungmobile.core.model.library.HistoryPolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PersonalizedHomeControllerTest {
    private fun song(letter: Char): CatalogItem {
        val url = "https://www.youtube.com/watch?v=${letter.toString().repeat(11)}"
        return CatalogItem(url, CatalogKind.SONG, "Song $letter", "Creator $letter", url)
    }

    private class Settings : RecommendationSettingsStore {
        val state = MutableStateFlow(RecommendationPreferences())
        override val recommendations: Flow<RecommendationPreferences> = state
        override suspend fun setRecommendationsEnabled(enabled: Boolean) {
            state.value = state.value.copy(enabled = enabled)
        }
        override suspend fun setRecommendationFeedback(songKey: String, feedback: RecommendationFeedback?) {
            state.value = state.value.copy(feedback = state.value.feedback.toMutableMap().apply {
                if (feedback == null) remove(songKey) else put(songKey, feedback)
            })
        }
        override suspend fun clearRecommendationFeedback() {
            state.value = state.value.copy(feedback = emptyMap())
        }
    }

    @Test fun removingLastLikeDiscardsRelatedSongsAndHidesShelf() = runTest {
        val profile = MutableStateFlow(RecommendationProfile(liked = listOf(song('A'))))
        val settings = Settings()
        var shelf: HomeShelf? = null
        val controller = PersonalizedHomeController(object : RelatedSource {
            override suspend fun related(seed: CatalogItem, limit: Int) = listOf(song('E'), song('F'))
        }, null, object : RecommendationProfileSource { override fun observe() = profile },
            settings, backgroundScope, { 1_000_000L }) { updated, _ -> shelf = updated }
        controller.updateProviderShelves(listOf(HomeShelf("ordinary", "Ordinary", listOf(song('B'), song('C'), song('D')))))
        runCurrent()
        assertTrue(shelf!!.items.contains(song('E')))

        profile.value = RecommendationProfile()
        runCurrent()
        assertNull(shelf)
        assertEquals(emptyMap<String, RecommendationFeedback>(), settings.state.value.feedback)
    }

    @Test fun moreFromSongImmediatelyRequestsThatSongAndKeepsOrdinaryCandidates() = runTest {
        val profile = MutableStateFlow(RecommendationProfile(liked = listOf(song('A'))))
        val settings = Settings()
        val requested = mutableListOf<CatalogItem>()
        var shelf: HomeShelf? = null
        val controller = PersonalizedHomeController(object : RelatedSource {
            override suspend fun related(seed: CatalogItem, limit: Int): List<CatalogItem> {
                requested += seed
                return if (seed == song('B')) listOf(song('G'), song('H')) else listOf(song('E'), song('F'))
            }
        }, null, object : RecommendationProfileSource { override fun observe() = profile },
            settings, backgroundScope, { 1_000_000L }) { updated, _ -> shelf = updated }
        controller.updateProviderShelves(listOf(HomeShelf("ordinary", "Ordinary", listOf(song('B'), song('C'), song('D')))))
        runCurrent()
        assertEquals(listOf(song('A')), requested)

        controller.setFeedback(song('B'), RecommendationFeedback.MORE)
        runCurrent()
        assertEquals(listOf(song('A'), song('B'), song('A')), requested)
        assertTrue(shelf!!.items.contains(song('G')))
        assertTrue(shelf!!.items.contains(song('C')))

        settings.state.value = settings.state.value.copy(enabled = false)
        runCurrent()
        assertNull(shelf)
        assertEquals(3, requested.size)

        settings.state.value = settings.state.value.copy(enabled = true)
        runCurrent()
        assertTrue(shelf!!.items.contains(song('G')))
        assertEquals(5, requested.size)

        profile.value = profile.value.copy(policy = HistoryPolicy(privateMode = true))
        runCurrent()
        assertNull(shelf)
        assertEquals(5, requested.size)
    }
}
