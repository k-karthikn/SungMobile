package com.karthi.sungmobile.core.model.discovery

import com.karthi.sungmobile.core.model.CatalogKind
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.library.libraryKeyOrNull

/** Pure ranking and artist balancing for generated playlists. */
object DiscoveryPlaylistRanker {
    fun rank(
        candidates: List<PlaylistCandidate>,
        profile: RecommendationProfile,
        seeds: List<PlaylistSeed>,
        preferences: RecommendationPreferences,
        options: PlaylistGenerationOptions,
        variation: Long = 0L,
    ): List<CatalogItem> {
        if (candidates.isEmpty()) return emptyList()

        val seedStrength = seeds.mapNotNull { seed ->
            seed.track.libraryKeyOrNull()?.let { it to seed.strength.coerceIn(0, 100) }
        }.toMap()
        val liked = profile.liked.mapNotNull { it.libraryKeyOrNull() }.toSet()
        val saved = profile.saved.mapNotNull { it.libraryKeyOrNull() }.toSet()
        val top = if (profile.policy.listening) profile.topSongs.mapNotNull { song ->
            song.track.libraryKeyOrNull()?.let { it to song }
        }.toMap() else emptyMap()
        val recentOrder = if (profile.policy.listening) profile.recentEvents
            .sortedByDescending { it.lastPlayedAt }
            .mapNotNull { it.track.libraryKeyOrNull() }
            .distinct()
            .withIndex()
            .associate { it.value to it.index } else emptyMap()
        val known = liked + saved + top.keys + recentOrder.keys + seedStrength.keys
        val support = candidates.asSequence()
            .filter { it.source == PlaylistCandidateSource.RELATED }
            .mapNotNull { candidate -> candidate.track.libraryKeyOrNull()?.let { it to candidate.relatedTo } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, sources) -> sources.filterNotNull().distinct().size }

        val scored = candidates.asSequence()
            .filter { it.track.kind == CatalogKind.SONG }
            .mapNotNull { candidate -> candidate.track.libraryKeyOrNull()?.let { it to candidate } }
            .filterNot { (key, _) -> preferences.feedback[key] == RecommendationFeedback.HIDE }
            .groupBy({ it.first }, { it.second })
            .mapNotNull { (key, rows) ->
                val bestRelatedStrength = rows.asSequence()
                    .filter { it.source == PlaylistCandidateSource.RELATED }
                    .map { row ->
                        val strength = row.relatedTo?.let(seedStrength::get) ?: row.seedStrength
                        strength.coerceIn(0, 100)
                    }.maxOrNull() ?: 0
                val candidate = rows.maxWithOrNull(compareBy<PlaylistCandidate> {
                    when (it.source) {
                        PlaylistCandidateSource.RELATED -> 3
                        PlaylistCandidateSource.HOME -> 2
                        PlaylistCandidateSource.LIBRARY -> 1
                        PlaylistCandidateSource.SEED -> 0
                    }
                }.thenByDescending { it.sourcePosition }) ?: return@mapNotNull null
                val topSong = top[key]
                val recencyPenalty = recentOrder[key]?.let { index -> maxOf(8, 32 - index * 2) } ?: 0
                val knownPenalty = if (key in known) options.discoveryLevel * 38 / 100 else 0
                val newMusicBonus = if (key !in known) options.discoveryLevel * 24 / 100 else 0
                val libraryAffinity = (if (key in liked) 42 else 0) +
                    (if (key in saved) 12 else 0) +
                    (topSong?.let { minOf(18, 8 + it.plays.coerceAtMost(5) * 2) } ?: 0)
                val sourceScore = when (candidate.source) {
                    PlaylistCandidateSource.RELATED -> 10 + bestRelatedStrength * 55 / 100
                    PlaylistCandidateSource.HOME -> 12
                    PlaylistCandidateSource.LIBRARY -> 4
                    PlaylistCandidateSource.SEED -> 0
                }
                val score = sourceScore + libraryAffinity + newMusicBonus - knownPenalty - recencyPenalty +
                    (if (preferences.feedback[key] == RecommendationFeedback.MORE) 28 else 0) +
                    minOf(16, maxOf(0, (support[key] ?: 0) - 1) * 8) +
                    if (candidate.source == PlaylistCandidateSource.SEED) 18 - options.discoveryLevel / 3 else 0
                ScoredTrack(candidate.track, score, candidate.sourcePosition, key)
            }
            .sortedWith(compareByDescending<ScoredTrack> { it.score }
                .thenBy { variationOrder(it.key, variation) }
                .thenBy { it.sourcePosition }
                .thenBy { it.key })
            .toList()

        val selected = mutableListOf<ScoredTrack>()
        val deferred = mutableListOf<ScoredTrack>()
        val artistCounts = mutableMapOf<String, Int>()
        for (row in scored) {
            val artist = artistKey(row.track)
            if (artist.isNotBlank() && artistCounts.getOrDefault(artist, 0) >= MAX_TRACKS_PER_ARTIST) {
                deferred += row
                continue
            }
            selected += row
            if (artist.isNotBlank()) artistCounts[artist] = artistCounts.getOrDefault(artist, 0) + 1
            if (selected.size >= options.trackCount) break
        }
        if (selected.size < options.trackCount) {
            selected += deferred.take(options.trackCount - selected.size)
        }
        return selected.map { it.track }
    }

    private fun artistKey(track: CatalogItem): String = track.subtitle
        .substringBefore(" · ")
        .substringBefore(" - ")
        .trim()
        .lowercase()

    private fun variationOrder(key: String, variation: Long): Int =
        (key.hashCode() xor variation.hashCode()).let { it and Int.MAX_VALUE }

    private data class ScoredTrack(
        val track: CatalogItem,
        val score: Int,
        val sourcePosition: Int,
        val key: String,
    )

    private const val MAX_TRACKS_PER_ARTIST = 3
}
