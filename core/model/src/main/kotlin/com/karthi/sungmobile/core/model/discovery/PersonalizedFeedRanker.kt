package com.karthi.sungmobile.core.model.discovery

import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.CatalogKind
import com.karthi.sungmobile.core.model.library.libraryKeyOrNull

/** A bounded, deterministic ordering over provider songs. It never fetches or stores music. */
object PersonalizedFeedRanker {
    fun rank(
        candidates: List<RecommendationCandidate>,
        profile: RecommendationProfile,
        preferences: RecommendationPreferences,
        limit: Int = 12,
    ): List<CatalogItem> {
        if (!preferences.enabled || profile.policy.privateMode || !profile.hasSignals || limit <= 0) return emptyList()
        val liked = profile.liked.mapNotNull { it.libraryKeyOrNull() }.toSet()
        val saved = profile.saved.mapNotNull { it.libraryKeyOrNull() }.toSet()
        val top = if (profile.policy.listening) profile.topSongs.associateBy { it.track.libraryKeyOrNull() }
            else emptyMap()
        val recent = if (profile.policy.listening) profile.recentEvents.mapNotNull {
            it.track.libraryKeyOrNull()
        }.toSet() else emptySet()

        val scored = candidates.asSequence().filter { it.item.kind == CatalogKind.SONG }
            .mapNotNull { candidate -> candidate.item.libraryKeyOrNull()?.let { it to candidate } }
            .filterNot { (key, _) -> preferences.feedback[key] == RecommendationFeedback.HIDE }
            .distinctBy { it.first }
            .map { (key, candidate) ->
                val source = candidate.relatedTo
                val sourceStrength = when {
                    source == null -> 0
                    preferences.feedback[source] == RecommendationFeedback.MORE -> 70
                    source in liked -> 55
                    source in saved -> 35
                    else -> 15
                }
                val score = sourceStrength +
                    (if (key in saved) 12 else 0) +
                    (if (preferences.feedback[key] == RecommendationFeedback.MORE) 30 else 0) +
                    (top[key]?.let { minOf(it.plays, 5) * 3 } ?: 0) -
                    (if (key in liked) 45 else 0) -
                    (if (key in recent) 35 else 0)
                Triple(candidate, score, key)
            }.sortedWith(compareByDescending<Triple<RecommendationCandidate, Int, String>> { it.second }
                .thenBy { it.first.sourcePosition }.thenBy { it.third }).toList()

        val perSeed = mutableMapOf<String, Int>()
        val selected = mutableListOf<CatalogItem>()
        val deferred = mutableListOf<CatalogItem>()
        for ((candidate) in scored) {
            val seed = candidate.relatedTo
            if (seed != null && perSeed.getOrDefault(seed, 0) >= 3) {
                deferred += candidate.item
                continue
            }
            selected += candidate.item
            if (seed != null) perSeed[seed] = perSeed.getOrDefault(seed, 0) + 1
            if (selected.size == limit) break
        }
        selected += deferred.take(limit - selected.size)
        return selected
    }
}
