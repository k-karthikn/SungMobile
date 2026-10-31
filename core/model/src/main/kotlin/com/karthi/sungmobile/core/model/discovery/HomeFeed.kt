package com.karthi.sungmobile.core.model.discovery

import com.karthi.sungmobile.core.model.CatalogItem

/** Provider-supplied rows; titles and order are never inferred from local listening history. */
data class HomeShelf(val key: String, val title: String, val items: List<CatalogItem>)

/** A provider-defined Home filter. The opaque parameters are sent back only to that provider. */
data class HomeChip(val title: String, val params: String, val browseId: String? = null) {
    val key: String get() = "${browseId.orEmpty()}:$params"
}

data class HomeFeedPage(val shelves: List<HomeShelf> = emptyList(), val chips: List<HomeChip> = emptyList())

interface HomeFeedSource {
    suspend fun homeShelves(): List<HomeShelf>

    suspend fun homePage(): HomeFeedPage = HomeFeedPage(shelves = homeShelves())

    suspend fun homePage(onUpdate: (HomeFeedPage) -> Unit): HomeFeedPage {
        val page = homePage()
        onUpdate(page)
        return page
    }

    suspend fun homeChip(chip: HomeChip): HomeFeedPage = HomeFeedPage()
}

/** Optional continuation of general provider Home music, without any local taste data. */
interface HomeCandidateSource {
    suspend fun extraHomeSongs(): List<CatalogItem>
}
