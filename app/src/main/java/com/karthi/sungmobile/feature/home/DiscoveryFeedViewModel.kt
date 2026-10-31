package com.karthi.sungmobile.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karthi.sungmobile.core.model.SourceException
import com.karthi.sungmobile.core.model.SourceFailure
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.RelatedSource
import com.karthi.sungmobile.core.model.discovery.HomeFeedSource
import com.karthi.sungmobile.core.model.discovery.HomeCandidateSource
import com.karthi.sungmobile.core.model.discovery.HomeShelf
import com.karthi.sungmobile.core.model.discovery.HomeChip
import com.karthi.sungmobile.core.model.discovery.RecommendationFeedback
import com.karthi.sungmobile.core.model.discovery.RecommendationProfileSource
import com.karthi.sungmobile.core.model.discovery.RecommendationSettingsStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiscoveryFeedState(val shelves: List<HomeShelf> = emptyList(),
    val homeChips: List<HomeChip> = emptyList(),
    val selectedHomeChip: HomeChip? = null,
    val chipShelves: List<HomeShelf> = emptyList(),
    val chipLoading: Boolean = false,
    val personalizedShelf: HomeShelf? = null,
    val recommendationFeedback: Map<String, RecommendationFeedback> = emptyMap(),
    val initialLoading: Boolean = true,
    val refreshing: Boolean = false, val error: SourceFailure? = null, val lastSuccessMs: Long = 0)

class DiscoveryFeedViewModel(private val source: HomeFeedSource,
    related: RelatedSource? = null,
    profileSource: RecommendationProfileSource? = null,
    settings: RecommendationSettingsStore? = null,
    general: HomeCandidateSource? = null,
    private val clock: () -> Long = System::currentTimeMillis) : ViewModel() {
    private val mutable = MutableStateFlow(DiscoveryFeedState())
    val state = mutable.asStateFlow()
    private var job: Job? = null
    private var chipJob: Job? = null
    private val personalized = if (related != null && profileSource != null && settings != null)
        PersonalizedHomeController(related, general, profileSource, settings, viewModelScope, clock) { shelf, feedback ->
            mutable.update { it.copy(personalizedShelf = shelf, recommendationFeedback = feedback) }
        } else null

    init { refresh() }

    fun ensureFresh() {
        val current = mutable.value
        if (job?.isActive == true || current.lastSuccessMs > 0 && clock() - current.lastSuccessMs < 20 * 60_000) return
        refresh()
    }

    fun refresh() {
        job?.cancel()
        chipJob?.cancel()
        mutable.update { it.copy(initialLoading = it.shelves.isEmpty(), refreshing = it.shelves.isNotEmpty(),
            selectedHomeChip = null, chipShelves = emptyList(), chipLoading = false, error = null) }
        job = viewModelScope.launch {
            try {
                val page = source.homePage { partial ->
                    mutable.update { it.copy(shelves = partial.shelves, homeChips = partial.chips,
                        initialLoading = false, refreshing = false) }
                    personalized?.updateProviderShelves(partial.shelves)
                }
                mutable.update { it.copy(shelves = page.shelves, homeChips = page.chips,
                    selectedHomeChip = null, chipShelves = emptyList(), chipLoading = false,
                    initialLoading = false, refreshing = false,
                    lastSuccessMs = clock(), error = null) }
                personalized?.updateProviderShelves(page.shelves)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                mutable.update { it.copy(initialLoading = false, refreshing = false,
                    error = (error as? SourceException)?.failure ?: SourceFailure.EXTRACTION) }
            }
        }
    }

    fun selectHomeChip(chip: HomeChip?) {
        val selected = chip?.takeUnless { it.key == mutable.value.selectedHomeChip?.key }
        chipJob?.cancel()
        mutable.update { it.copy(selectedHomeChip = selected, chipShelves = emptyList(),
            chipLoading = selected != null) }
        if (selected == null) return
        chipJob = viewModelScope.launch {
            try {
                val page = source.homeChip(selected)
                mutable.update { current ->
                    if (current.selectedHomeChip?.key == selected.key) current.copy(
                        chipShelves = page.shelves, chipLoading = false)
                    else current
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                mutable.update { current ->
                    if (current.selectedHomeChip?.key == selected.key) current.copy(chipLoading = false)
                    else current
                }
            }
        }
    }

    fun setRecommendationFeedback(item: CatalogItem, feedback: RecommendationFeedback?) {
        personalized?.setFeedback(item, feedback)
    }
}
