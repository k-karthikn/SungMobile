package com.karthi.sungmobile.feature.home

import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.CatalogKind
import com.karthi.sungmobile.core.model.RelatedSource
import com.karthi.sungmobile.core.model.discovery.*
import com.karthi.sungmobile.core.model.library.libraryKeyOrNull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

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
                val previousLiked = profile.liked.mapNotNull { it.libraryKeyOrNull() }.toSet()
                val previousSaved = profile.saved.mapNotNull { it.libraryKeyOrNull() }.toSet()
                profile = updatedProfile
                preferences = updatedPreferences
                ready = true
                val chosenMore = preferences.feedback.filterValues { it == RecommendationFeedback.MORE }.keys
                val allowedSeeds = profileSeedKeys() + chosenMore
                if (seedKeys.isNotEmpty() && seedKeys.any { it !in allowedSeeds }) {
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
                        chosenMore.any { it !in previousMore } ||
                            profile.liked.mapNotNull { it.libraryKeyOrNull() }.any { it !in previousLiked } ||
                            profile.saved.mapNotNull { it.libraryKeyOrNull() }.any { it !in previousSaved } -> fetchRelated(force = true)
                        fetchedAt == 0L || seedKeys.isEmpty() -> fetchRelated(force = false)
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
        if (eligible()) fetchRelated(force = false)
    }

    fun setFeedback(item: CatalogItem, feedback: RecommendationFeedback?) {
        val key = item.libraryKeyOrNull() ?: return
        if (!eligible()) return
        scope.launch { settings.setRecommendationFeedback(key, feedback) }
    }

    private fun eligible() = ready && preferences.enabled && !profile.policy.privateMode &&
        (profile.hasSignals || preferences.feedback.values.any { it == RecommendationFeedback.MORE })

    private fun publishRanked() {
        val items = PersonalizedFeedRanker.rank(expanded + provider, profile, preferences)
        publish(items.takeIf { it.size >= 3 }?.let { HomeShelf("personalized_for_you", "For you", it) },
            preferences.feedback)
    }

    private fun fetchRelated(force: Boolean) {
        if (!eligible()) return
        val seeds = selectSeeds()
        if (seeds.isEmpty()) { fetchGeneral(force); return }
        val requests = seeds.mapIndexedNotNull { seedIndex, seed ->
            seed.libraryKeyOrNull()?.let { key -> SeedRequest(seedIndex, key, seed) }
        }
        if (requests.isEmpty()) { fetchGeneral(force); return }
        val keys = requests.map { it.key }
        if (!force && (fetch?.isActive == true ||
                fetchedAt > 0L && clock() - fetchedAt < RELATED_CACHE_TTL_MS)) return
        fetch?.cancel()
        seedKeys = keys
        fetch = scope.launch {
            val results = expanded.toMutableList()
            val completed = Channel<Pair<Int, List<CatalogItem>?>>(capacity = requests.size)
            requests.forEach { (seedIndex, key, seed) ->
                launch {
                    val found = try { withTimeoutOrNull(RELATED_TIMEOUT_MS) { related.related(seed, RELATED_LIMIT) } }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { null }
                    val playable = found?.let { tracks ->
                        tracks.filter { item ->
                            item.kind == CatalogKind.SONG && item.libraryKeyOrNull() != null &&
                                item.libraryKeyOrNull() != key
                        }.distinctBy { it.libraryKeyOrNull() }.take(RELATED_LIMIT)
                    }
                    completed.send(seedIndex to playable)
                }
            }
            repeat(requests.size) {
                val (seedIndex, found) = completed.receive()
                val seedRequest = requests.firstOrNull { it.index == seedIndex } ?: return@repeat
                if (found != null) {
                    results.removeAll { it.relatedTo == seedRequest.key }
                    found.forEachIndexed { itemIndex, item ->
                        results += RecommendationCandidate(item,
                            provider.size + seedIndex * RELATED_LIMIT + itemIndex, seedRequest.key)
                    }
                    if (!eligible() || keys != seedKeys) return@launch
                    expanded = results.sortedBy { it.sourcePosition }
                    publishRanked()
                }
            }
            if (!eligible() || keys != seedKeys) return@launch
            fetchedAt = clock()
            if (results.isEmpty()) publishRanked()
        }
    }

    private fun fetchGeneral(force: Boolean) {
        val source = general ?: return
        if (!profile.policy.listening || profile.topSongs.isEmpty() && profile.recentEvents.isEmpty()) return
        if (!force && (fetch?.isActive == true || fetchedAt > 0 && clock() - fetchedAt < 20 * 60_000)) return
        fetch?.cancel()
        fetch = scope.launch {
            val songs = try { withTimeoutOrNull(RELATED_TIMEOUT_MS) { source.extraHomeSongs() }.orEmpty() }
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
        // Listening history ranks candidates; explicit library signals seed related-track requests.
        // History-only profiles use the general provider continuation below.
        val known = (provider + expanded).map { it.item } + profile.liked + profile.saved
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
        val chosen = linkedMapOf<String, CatalogItem>()
        fun add(items: List<CatalogItem>, count: Int) {
            var added = 0
            for (item in rotate(items)) {
                val key = item.libraryKeyOrNull() ?: continue
                if (key in chosen) continue
                chosen[key] = item
                added++
                if (added >= count || chosen.size >= MAX_SEEDS) break
            }
        }
        add(explicit, 1)
        add(liked, 1)
        add(profile.saved, 1)
        add(explicit + liked + profile.saved, MAX_SEEDS - chosen.size)
        return chosen.values.toList()
    }

    private fun profileSeedKeys(): Set<String> = buildSet {
        profile.liked.mapNotNullTo(this) { it.libraryKeyOrNull() }
        profile.saved.mapNotNullTo(this) { it.libraryKeyOrNull() }
        if (profile.policy.listening) {
            profile.topSongs.mapNotNullTo(this) { it.track.libraryKeyOrNull() }
            profile.recentEvents.mapNotNullTo(this) { it.track.libraryKeyOrNull() }
        }
    }

    private companion object {
        const val RELATED_LIMIT = 5
        const val MAX_SEEDS = 4
        const val RELATED_TIMEOUT_MS = 20_000L
        const val RELATED_CACHE_TTL_MS = 20 * 60_000L
    }

    private data class SeedRequest(val index: Int, val key: String, val item: CatalogItem)
}
