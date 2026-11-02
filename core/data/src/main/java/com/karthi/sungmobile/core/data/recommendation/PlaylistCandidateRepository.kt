package com.karthi.sungmobile.core.data.recommendation

import com.karthi.sungmobile.core.model.CatalogKind
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.RelatedSource
import com.karthi.sungmobile.core.model.discovery.HomeCandidateSource
import com.karthi.sungmobile.core.model.discovery.PlaylistCandidate
import com.karthi.sungmobile.core.model.discovery.PlaylistCandidateSource
import com.karthi.sungmobile.core.model.discovery.PlaylistSeed
import com.karthi.sungmobile.core.model.library.libraryKeyOrNull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Bounded provider expansion for the playlist generator, with a short-lived Home fallback cache. */
class PlaylistCandidateRepository(
    private val related: RelatedSource,
    private val home: HomeCandidateSource?,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val homeMutex = Mutex()
    private var cachedHome: List<CatalogItem> = emptyList()
    private var homeFetchedAt = 0L

    suspend fun candidates(seeds: List<PlaylistSeed>): List<PlaylistCandidate> = coroutineScope {
        val boundedSeeds = seeds.take(MAX_SEEDS)
        val expansions = boundedSeeds.mapIndexed { index, seed ->
            async {
                val tracks = try {
                    related.related(seed.track, RELATED_LIMIT)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    emptyList()
                }
                index to tracks
            }
        }
        val homeSongs = async { readHomeCache() }
        val relatedRows = expansions.map { it.await() }
        val generalRows = homeSongs.await()

        buildList {
            relatedRows.forEach { (seedIndex, tracks) ->
                val seed = boundedSeeds[seedIndex]
                val seedKey = seed.track.libraryKeyOrNull()
                tracks.forEachIndexed { index, track ->
                    if (track.kind == CatalogKind.SONG && track.libraryKeyOrNull() != seedKey) {
                        add(PlaylistCandidate(track, PlaylistCandidateSource.RELATED,
                            seedIndex * RELATED_LIMIT + index, seedKey, seed.strength))
                    }
                }
            }
            generalRows.forEachIndexed { index, track ->
                add(PlaylistCandidate(track, PlaylistCandidateSource.HOME,
                    boundedSeeds.size * RELATED_LIMIT + index))
            }
        }
    }

    private suspend fun readHomeCache(): List<CatalogItem> = homeMutex.withLock {
        if (homeFetchedAt > 0L && clock() - homeFetchedAt < HOME_CACHE_TTL_MS) return@withLock cachedHome
        val fetched = try {
            home?.extraHomeSongs().orEmpty()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            emptyList()
        }
        cachedHome = fetched.asSequence()
            .filter { it.kind == CatalogKind.SONG && it.libraryKeyOrNull() != null }
            .distinctBy { it.libraryKeyOrNull() }
            .take(MAX_HOME_CANDIDATES)
            .toList()
        homeFetchedAt = clock()
        cachedHome
    }

    private companion object {
        const val RELATED_LIMIT = 5
        const val MAX_SEEDS = 4
        const val MAX_HOME_CANDIDATES = 40
        const val HOME_CACHE_TTL_MS = 20 * 60_000L
    }
}
