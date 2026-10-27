package com.karthi.sungmobile.core.model

import com.karthi.sungmobile.core.model.discovery.*
import com.karthi.sungmobile.core.model.library.HistoryPolicy
import com.karthi.sungmobile.core.model.library.ListeningEvent
import org.junit.Assert.*
import org.junit.Test

class PersonalizedFeedRankerTest {
    private fun song(letter: Char, creator: String = "Creator $letter"): CatalogItem {
        val url = "https://www.youtube.com/watch?v=${letter.toString().repeat(11)}"
        return CatalogItem(url, CatalogKind.SONG, "Song $letter", creator, url)
    }
    private fun candidate(item: CatalogItem, position: Int, seed: String? = null) =
        RecommendationCandidate(item, position, seed)

    @Test fun relatedSongsLeadWhileDuplicatesAndHiddenSongsStayOut() {
        val liked = song('A')
        val related = song('B')
        val ordinary = song('C')
        val profile = RecommendationProfile(liked = listOf(liked))
        val feedback = RecommendationPreferences(feedback = mapOf("youtube:${"D".repeat(11)}" to RecommendationFeedback.HIDE))
        val items = PersonalizedFeedRanker.rank(listOf(
            candidate(ordinary, 0), candidate(related, 1, "youtube:${"A".repeat(11)}"),
            candidate(related, 2), candidate(song('D'), 3), candidate(liked, 4),
        ), profile, feedback)
        assertEquals(listOf(related, ordinary, liked), items)
        assertEquals(items, PersonalizedFeedRanker.rank(listOf(
            candidate(ordinary, 0), candidate(related, 1, "youtube:${"A".repeat(11)}"),
            candidate(related, 2), candidate(song('D'), 3), candidate(liked, 4),
        ), profile, feedback))
    }

    @Test fun privateModeAndDisabledHistoryCannotProduceHistoryOnlyRecommendations() {
        val event = ListeningEvent("one", song('A'), 1, 2, 90_000)
        val candidates = listOf(candidate(song('B'), 0))
        assertTrue(PersonalizedFeedRanker.rank(candidates,
            RecommendationProfile(policy = HistoryPolicy(privateMode = true), recentEvents = listOf(event)),
            RecommendationPreferences()).isEmpty())
        assertTrue(PersonalizedFeedRanker.rank(candidates,
            RecommendationProfile(policy = HistoryPolicy(listening = false), recentEvents = listOf(event)),
            RecommendationPreferences()).isEmpty())
    }

    @Test fun recentListeningLowersExactRepeatWithoutTreatingSubtitleAsVerifiedArtist() {
        val liked = song('A')
        val recent = song('B', "Shared")
        val profile = RecommendationProfile(liked = listOf(liked),
            recentEvents = listOf(ListeningEvent("one", recent, 1, 2, 90_000)))
        val items = PersonalizedFeedRanker.rank(listOf(
            candidate(recent, 0), candidate(song('C', "Shared"), 1),
            candidate(song('D', "Shared"), 2), candidate(song('E', "Shared"), 3),
        ), profile, RecommendationPreferences())
        assertEquals(4, items.size)
        assertEquals(song('C', "Shared"), items.first())
        assertEquals(recent, items.last())
    }

    @Test fun relatedSongsFromOneSeedLeaveRoomForOtherSources() {
        val liked = song('A')
        val key = "youtube:${"A".repeat(11)}"
        val items = PersonalizedFeedRanker.rank(listOf(
            candidate(song('B'), 0, key), candidate(song('C'), 1, key),
            candidate(song('D'), 2, key), candidate(song('E'), 3, key),
            candidate(song('F'), 4),
        ), RecommendationProfile(liked = listOf(liked)), RecommendationPreferences(), limit = 5)
        assertEquals(listOf(song('B'), song('C'), song('D'), song('F'), song('E')), items)
    }
}
