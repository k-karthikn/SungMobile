package com.karthi.sungmobile.feature.search

import com.karthi.sungmobile.R
import com.karthi.sungmobile.core.model.CatalogKind
import com.karthi.sungmobile.core.model.SourceFailure

internal fun CatalogKind.label() = when (this) {
    CatalogKind.SONG -> R.string.songs
    CatalogKind.ALBUM -> R.string.albums
    CatalogKind.ARTIST -> R.string.artists
    CatalogKind.PLAYLIST -> R.string.playlists
}

internal fun SourceFailure.message() = when (this) {
    SourceFailure.INVALID_LINK -> R.string.error_link
    SourceFailure.NETWORK -> R.string.error_network
    SourceFailure.UNAVAILABLE -> R.string.error_unavailable
    SourceFailure.AUTHENTICATION_REQUIRED -> R.string.error_auth
    SourceFailure.RATE_LIMITED -> R.string.error_rate
    SourceFailure.UNSUPPORTED -> R.string.error_unsupported
    SourceFailure.EXTRACTION -> R.string.error_extraction
    SourceFailure.EXPIRED_PAGE -> R.string.error_page
}
