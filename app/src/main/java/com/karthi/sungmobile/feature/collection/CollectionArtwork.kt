package com.karthi.sungmobile.feature.collection

import com.karthi.sungmobile.core.model.CatalogKind

/** Search thumbnails often exist even when an opened YouTube collection has no cover thumbnail. */
internal fun CollectionState.coverArtworkUrl(): String? {
    val candidates = sequenceOf(page?.details?.artworkUrl, route?.artworkUrl,
        page?.items?.firstOrNull { it.artworkUrl?.isNotBlank() == true }?.artworkUrl)
    return candidates.firstOrNull { !it.isNullOrBlank() }
}

internal fun CollectionState.displayKind(): CatalogKind =
    route?.kind ?: page?.details?.kind ?: CatalogKind.PLAYLIST
