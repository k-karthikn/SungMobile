package com.karthi.sungmobile.core.model.discovery

import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.library.HistoryPolicy
import com.karthi.sungmobile.core.model.library.ListeningEvent
import com.karthi.sungmobile.core.model.library.TopSong
import kotlinx.coroutines.flow.Flow

enum class RecommendationFeedback { MORE, HIDE }

data class RecommendationPreferences(
    val enabled: Boolean = true,
    val feedback: Map<String, RecommendationFeedback> = emptyMap(),
)

data class RecommendationProfile(
    val policy: HistoryPolicy = HistoryPolicy(),
    val liked: List<CatalogItem> = emptyList(),
    val saved: List<CatalogItem> = emptyList(),
    val topSongs: List<TopSong> = emptyList(),
    val recentEvents: List<ListeningEvent> = emptyList(),
) {
    val hasSignals: Boolean get() = liked.isNotEmpty() ||
        policy.listening && (topSongs.isNotEmpty() || recentEvents.isNotEmpty())
}

data class RecommendationCandidate(
    val item: CatalogItem,
    val sourcePosition: Int,
    val relatedTo: String? = null,
)

interface RecommendationProfileSource {
    fun observe(): Flow<RecommendationProfile>
}

interface RecommendationSettingsStore {
    val recommendations: Flow<RecommendationPreferences>
    suspend fun setRecommendationsEnabled(enabled: Boolean)
    suspend fun setRecommendationFeedback(songKey: String, feedback: RecommendationFeedback?)
    suspend fun clearRecommendationFeedback()
}
