package com.karthi.sungmobile.core.model

import com.karthi.sungmobile.core.model.discovery.DiscoveryPlaylistRanker
import com.karthi.sungmobile.core.model.discovery.PlaylistCandidate
import com.karthi.sungmobile.core.model.discovery.PlaylistCandidateSource
import com.karthi.sungmobile.core.model.discovery.PlaylistGenerationOptions
import com.karthi.sungmobile.core.model.discovery.PlaylistSeed
import com.karthi.sungmobile.core.model.discovery.RecommendationFeedback
import com.karthi.sungmobile.core.model.discovery.RecommendationPreferences
import com.karthi.sungmobile.core.model.discovery.RecommendationProfile
import com.karthi.sungmobile.core.model.library.libraryKeyOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoveryPlaylistRankerTest {
    private fun song(letter: Char, artist: String = "Artist $letter"): CatalogItem {
        val id = letter.toString().repeat(11)
        val url = "https://www.youtube.com/watch?v=$id"
        return CatalogItem(url, CatalogKind.SONG, "Song $letter", artist, url)
    }

    private fun related(track: CatalogItem, position: Int, seed: CatalogItem, strength: Int = 100) =
        PlaylistCandidate(track, PlaylistCandidateSource.RELATED, position,
            seed.trackKey(), strength)

    private fun CatalogItem.trackKey() = libraryKeyOrNull()

    @Test
    fun hiddenTracksAreRemovedAndDuplicateCandidatesBecomeOneSong() {
        val seed = song('A')
        val duplicate = song('B')
        val hidden = song('C')
        val hiddenKey = requireNotNull(hidden.libraryKeyOrNull())
        val ranked = DiscoveryPlaylistRanker.rank(
            candidates = listOf(
                related(duplicate, 0, seed),
                PlaylistCandidate(duplicate, PlaylistCandidateSource.HOME, 10),
                related(hidden, 1, seed),
                related(song('D'), 2, seed),
            ),
            profile = RecommendationProfile(),
            seeds = listOf(PlaylistSeed(seed, 100)),
            preferences = RecommendationPreferences(
                feedback = mapOf(hiddenKey to RecommendationFeedback.HIDE),
            ),
            options = PlaylistGenerationOptions(trackCount = 12, discoveryLevel = 60),
        )

        assertEquals(2, ranked.size)
        assertEquals(2, ranked.mapNotNull { it.libraryKeyOrNull() }.distinct().size)
        assertFalse(ranked.any { it.libraryKeyOrNull() == hiddenKey })
    }

    @Test
    fun discoveryControlMovesFamiliarSongsBehindUnfamiliarMatches() {
        val seed = song('A')
        val familiar = song('B')
        val newTrack = song('C')
        val candidates = listOf(
            related(newTrack, 0, seed, strength = 50),
            PlaylistCandidate(familiar, PlaylistCandidateSource.LIBRARY, 1),
        )
        val profile = RecommendationProfile(liked = listOf(familiar))
        val seeds = listOf(PlaylistSeed(seed, 50))

        val familiarMix = DiscoveryPlaylistRanker.rank(candidates, profile, seeds,
            RecommendationPreferences(), PlaylistGenerationOptions(12, 0))
        val discoveryMix = DiscoveryPlaylistRanker.rank(candidates, profile, seeds,
            RecommendationPreferences(), PlaylistGenerationOptions(12, 100))

        assertEquals(familiar, familiarMix.first())
        assertEquals(newTrack, discoveryMix.first())
    }

    @Test
    fun artistLimitLeavesRoomForOtherArtistsWhenEnoughCandidatesExist() {
        val seed = song('A')
        val candidates = buildList {
            "BCDE".forEachIndexed { index, letter ->
                add(related(song(letter, "Same Artist"), index, seed))
            }
            "FGHIJKLMNOPQ".forEachIndexed { index, letter ->
                add(related(song(letter, "Artist $letter"), index + 4, seed))
            }
        }

        val ranked = DiscoveryPlaylistRanker.rank(candidates, RecommendationProfile(),
            listOf(PlaylistSeed(seed, 100)), RecommendationPreferences(),
            PlaylistGenerationOptions(trackCount = 12, discoveryLevel = 50))

        assertEquals(12, ranked.size)
        assertTrue(ranked.count { it.subtitle == "Same Artist" } <= 3)
    }
}
