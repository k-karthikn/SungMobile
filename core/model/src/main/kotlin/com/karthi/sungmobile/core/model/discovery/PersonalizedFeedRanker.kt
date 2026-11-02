package com.karthi.sungmobile.core.model.discovery

import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.CatalogKind
import com.karthi.sungmobile.core.model.library.libraryKeyOrNull

private const val MORE_SEED_SCORE = 80
private const val LIKED_SEED_SCORE = 60
private const val TOP_SEED_SCORE = 48
private const val RECENT_SEED_SCORE = 40
private const val SAVED_SEED_SCORE = 32
private const val OTHER_SEED_SCORE = 15
private const val SOURCE_CONSENSUS_STEP = 8
private const val SOURCE_CONSENSUS_CAP = 16

/** A bounded, deterministic ordering over provider songs. It never fetches or stores music. */
object PersonalizedFeedRanker {
    fun rank(
        candidates: List<RecommendationCandidate>,
        profile: RecommendationProfile,
        preferences: RecommendationPreferences,
        limit: Int = 12,
    ): List<CatalogItem> {
        val hasExplicitTaste = preferences.feedback.values.any { it == RecommendationFeedback.MORE }
        if (!preferences.enabled || profile.policy.privateMode || (!profile.hasSignals && !hasExplicitTaste) || limit <= 0) return emptyList()
        val liked = profile.liked.mapNotNull { it.libraryKeyOrNull() }.toSet()
        val saved = profile.saved.mapNotNull { it.libraryKeyOrNull() }.toSet()
        val top = if (profile.policy.listening) {
            profile.topSongs.mapNotNull { song -> song.track.libraryKeyOrNull()?.let { it to song } }.toMap()
        } else emptyMap()
        val recentRank = if (profile.policy.listening) buildMap {
            profile.recentEvents.forEachIndexed { index, event ->
                event.track.libraryKeyOrNull()?.let { key ->
                    if (key !in this) put(key, index)
                }
            }
        } else emptyMap()
        val sourceCount = candidates.asSequence()
            .filter { it.item.kind == CatalogKind.SONG }
            .mapNotNull { candidate -> candidate.item.libraryKeyOrNull()?.let { it to candidate.relatedTo } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, sources) -> sources.distinct().size }

        val scored = candidates.asSequence().filter { it.item.kind == CatalogKind.SONG }
            .mapNotNull { candidate -> candidate.item.libraryKeyOrNull()?.let { it to candidate } }
            .filterNot { (key, _) -> preferences.feedback[key] == RecommendationFeedback.HIDE }
            .map { (key, candidate) ->
                val source = candidate.relatedTo
                val sourceStrength = when {
                    source != null && preferences.feedback[source] == RecommendationFeedback.MORE -> MORE_SEED_SCORE
                    source != null && source in liked -> LIKED_SEED_SCORE
                    source != null && source in top -> TOP_SEED_SCORE
                    source != null && source in recentRank -> RECENT_SEED_SCORE
                    source != null && source in saved -> SAVED_SEED_SCORE
                    else -> OTHER_SEED_SCORE
                }
                val topSong = top[key]
                val familiarPenalty = topSong?.let {
                    minOf(18, 8 + minOf(it.plays, 5) * 2 + (it.listenedMs / 1_800_000L).toInt())
                } ?: 0
                val recentPenalty = recentRank[key]?.let { index -> maxOf(12, 35 - index) } ?: 0
                val score = sourceStrength +
                    (sourceCount[key]?.let {
                        minOf(SOURCE_CONSENSUS_CAP, maxOf(0, it - 1) * SOURCE_CONSENSUS_STEP)
                    } ?: 0) +
                    (if (key in saved) 8 else 0) +
                    (if (preferences.feedback[key] == RecommendationFeedback.MORE) 30 else 0) +
                    (-familiarPenalty) - (if (key in liked) 45 else 0) - recentPenalty
                Triple(candidate, score, key)
            }.sortedWith(compareByDescending<Triple<RecommendationCandidate, Int, String>> { it.second }
                .thenBy { it.first.sourcePosition }.thenBy { it.third })
            .distinctBy { it.third }
            .toList()

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
