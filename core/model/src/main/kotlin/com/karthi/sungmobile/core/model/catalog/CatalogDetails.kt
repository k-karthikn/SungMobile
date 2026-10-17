package com.karthi.sungmobile.core.model.catalog

import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.CatalogKind
import java.util.UUID

/** Metadata describes what the source actually exposes; a channel is not a discography. */
data class CatalogDetails(val url: String, val kind: CatalogKind, val creator: String = "",
    val artworkUrl: String? = null, val totalCount: Long? = null, val channelVideos: Boolean = false)
data class CatalogEntry(val id: String = UUID.randomUUID().toString(), val track: CatalogItem)

/** Information the provider exposes for a row that cannot currently be played. */
data class UnavailableCatalogEntry(val title: String, val reason: String)
