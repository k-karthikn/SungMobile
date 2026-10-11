package com.karthi.sungmobile.app

import com.karthi.sungmobile.core.model.*

data class CatalogState(
    val query: String = "",
    val kind: CatalogKind = CatalogKind.SONG,
    val page: CatalogPage? = null,
    val loading: Boolean = false,
    val error: SourceFailure? = null,
    val inCollection: Boolean = false,
)

