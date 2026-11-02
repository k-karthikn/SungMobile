package com.karthi.sungmobile.core.model.discovery

import com.karthi.sungmobile.core.model.CatalogItem

data class PlaylistSeed(val track: CatalogItem, val strength: Int)

enum class PlaylistCandidateSource { RELATED, HOME, LIBRARY, SEED }

data class PlaylistCandidate(
    val track: CatalogItem,
    val source: PlaylistCandidateSource,
    val sourcePosition: Int,
    val relatedTo: String? = null,
    val seedStrength: Int = 0,
)

data class PlaylistGenerationOptions(
    val trackCount: Int = 20,
    val discoveryLevel: Int = 55,
) {
    init {
        require(trackCount in ALLOWED_TRACK_COUNTS)
        require(discoveryLevel in 0..100)
    }

    companion object {
        val ALLOWED_TRACK_COUNTS = setOf(12, 20, 30)
    }
}
