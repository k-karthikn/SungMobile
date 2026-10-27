package com.karthi.sungmobile.feature.home

import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.CatalogKind
import com.karthi.sungmobile.core.model.RelatedSource
import com.karthi.sungmobile.core.model.discovery.*
import com.karthi.sungmobile.core.model.library.libraryKeyOrNull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/** Keeps provider expansion and local ranking out of Compose and the ordinary Home fetch path. */
internal class PersonalizedHomeController(
    private val related: RelatedSource,
    private val general: HomeCandidateSource?,
    profileSource: RecommendationProfileSource,
    private val settings: RecommendationSettingsStore,
    private val scope: CoroutineScope,
    private val clock: () -> Long,
    private val publish: (HomeShelf?, Map<String, RecommendationFeedback>) -> Unit,
) {
    private var profile = RecommendationProfile()
    private var preferences = RecommendationPreferences()
    private var ready = false
    private var provider = emptyList<RecommendationCandidate>()
    private var expanded = emptyList<RecommendationCandidate>()
    private var fetch: Job? = null
    private var fetchedAt = 0L
    private var seedKeys = emptyList<String>()

    init {
        scope.launch {
            combine(profileSource.observe(), settings.recommendations) { profile, preferences ->
                profile to preferences
            }.collect { (updatedProfile, updatedPreferences) ->
                val previousMore = preferences.feedback.filterValues { it == RecommendationFeedback.MORE }.keys
                profile = updatedProfile
                preferences = updatedPreferences
                ready = true
                val chosenMore = preferences.feedback.filterValues { it == RecommendationFeedback.MORE }.keys
                val allowedSeeds = profile.liked.mapNotNull { it.libraryKeyOrNull() }.toSet() + chosenMore
                if (seedKeys.any { it !in allowedSeeds }) {
                    fetch?.cancel()
                    seedKeys = emptyList()
                    fetchedAt = 0L
                }
                expanded = expanded.filter { it.relatedTo == null || it.relatedTo in allowedSeeds }
                if (!eligible()) {
                    fetch?.cancel()
                    expanded = emptyList()
                    seedKeys = emptyList()
                    fetchedAt = 0L
                    if (!preferences.enabled || profile.policy.privateMode) {
                        profile = RecommendationProfile(policy = profile.policy)
                    }
                }
                publishRanked()
                if (eligible() && provider.isNotEmpty()) {
                    when {
                        chosenMore.any { it !in previousMore } -> fetchRelated(force = true)
                        fetchedAt == 0L -> fetchRelated(force = false)
                    }
                }
            }
        }
    }

    fun updateProviderShelves(shelves: List<HomeShelf>) {
        provider = shelves.flatMap { it.items }.asSequence()
            .filter { it.kind == CatalogKind.SONG && it.libraryKeyOrNull() != null }
            .distinctBy { it.libraryKeyOrNull() }
            .mapIndexed { index, item -> RecommendationCandidate(item, index * 2) }.take(70).toList()
        publishRanked()
        if (eligible()) fetchRelated(force = true)
    }

    fun setFeedback(item: CatalogItem, feedback: RecommendationFeedback?) {
        val key = item.libraryKeyOrNull() ?: return
        if (!eligible()) return
        scope.launch { settings.setRecommendationFeedback(key, feedback) }
    }

    private fun eligible() = ready && preferences.enabled && !profile.policy.privateMode && profile.hasSignals

    private fun publishRanked() {
        val items = PersonalizedFeedRanker.rank(expanded + provider, profile, preferences)
        publish(items.takeIf { it.size >= 3 }?.let { HomeShelf("personalized_for_you", "For you", it) },
            preferences.feedback)
    }

    private fun fetchRelated(force: Boolean) {
        if (!eligible()) return
        val seeds = selectSeeds()
        if (seeds.isEmpty()) { fetchGeneral(force); return }
        val keys = seeds.mapNotNull { it.libraryKeyOrNull() }
        if (!force && (fetch?.isActive == true || keys == seedKeys && clock() - fetchedAt < 20 * 60_000)) return
        fetch?.cancel()
        seedKeys = keys
        fetch = scope.launch {
            val results = mutableListOf<RecommendationCandidate>()
            for (seed in seeds) {
                val key = seed.libraryKeyOrNull() ?: continue
                val found = try { withTimeout(20_000) { related.related(seed, 5) } }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { emptyList() }
                found.asSequence().filter { it.kind == CatalogKind.SONG && it.libraryKeyOrNull() != null }
                    .filter { it.libraryKeyOrNull() != key }.take(5).forEach { item ->
                        results += RecommendationCandidate(item, provider.size + results.size, key)
                    }
            }
            if (!eligible() || keys != seedKeys) return@launch
            expanded = results
            fetchedAt = clock()
            publishRanked()
        }
    }

    private fun fetchGeneral(force: Boolean) {
        val source = general ?: return
        if (!profile.policy.listening || profile.topSongs.isEmpty() && profile.recentEvents.isEmpty()) return
        if (!force && (fetch?.isActive == true || fetchedAt > 0 && clock() - fetchedAt < 20 * 60_000)) return
        fetch?.cancel()
        fetch = scope.launch {
            val songs = try { withTimeout(20_000) { source.extraHomeSongs() } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { emptyList() }
            if (!eligible() || selectSeeds().isNotEmpty()) return@launch
            expanded = songs.asSequence().filter { it.kind == CatalogKind.SONG && it.libraryKeyOrNull() != null }
                .distinctBy { it.libraryKeyOrNull() }.take(30)
                .mapIndexed { index, item -> RecommendationCandidate(item, index * 2 + 1) }.toList()
            fetchedAt = clock()
            publishRanked()
        }
    }

    private fun selectSeeds(): List<CatalogItem> {
        val more = preferences.feedback.filterValues { it == RecommendationFeedback.MORE }.keys
        val known = (provider + expanded).map { it.item } + profile.liked
        val explicit = more.mapNotNull { key ->
            known.firstOrNull { it.libraryKeyOrNull() == key } ?: key.removePrefix("youtube:")
                .takeIf { key.startsWith("youtube:") && it.length == 11 }?.let { id ->
                    val url = "https://www.youtube.com/watch?v=$id"
                    CatalogItem(url, CatalogKind.SONG, "", "", url)
                }
        }.distinctBy { it.libraryKeyOrNull() }
        val explicitKeys = explicit.mapNotNull { it.libraryKeyOrNull() }.toSet()
        val liked = profile.liked.filter { it.libraryKeyOrNull() !in explicitKeys }
            .distinctBy { it.libraryKeyOrNull() }
        fun rotate(items: List<CatalogItem>): List<CatalogItem> {
            if (items.size <= 1) return items
            val offset = ((clock() / 86_400_000L) % items.size).toInt()
            return items.drop(offset) + items.take(offset)
        }
        val chosen = rotate(explicit).take(2)
        return chosen + rotate(liked).take(2 - chosen.size)
    }
}
