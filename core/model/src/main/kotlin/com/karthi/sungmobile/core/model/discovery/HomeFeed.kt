package com.karthi.sungmobile.core.model.discovery

import com.karthi.sungmobile.core.model.CatalogItem

/** Provider-supplied rows; titles and order are never inferred from local listening history. */
data class HomeShelf(val key: String, val title: String, val items: List<CatalogItem>)

interface HomeFeedSource {
    suspend fun homeShelves(): List<HomeShelf>
}
