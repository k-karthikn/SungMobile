package com.karthi.sungmobile.app

import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.CatalogKind

data class CatalogActions(
    val editQuery: (String) -> Unit,
    val chooseKind: (CatalogKind) -> Unit,
    val search: () -> Unit,
    val open: (CatalogItem) -> Unit,
    val loadMore: () -> Unit,
    val back: () -> Unit,
    val retry: () -> Unit,
    val searchArtistSongs: (String) -> Unit,
    val playNext: (CatalogItem) -> Unit = {},
    val append: (CatalogItem) -> Unit = {},
    val active: (Boolean) -> Unit = {},
)

data class PlayerActions(
    val toggle: () -> Unit, val seek: (Long) -> Unit, val stop: () -> Unit,
    val next: () -> Unit = {}, val previous: () -> Unit = {},
    val repeat: (com.karthi.sungmobile.core.model.RepeatMode) -> Unit = {},
    val shuffle: (Boolean) -> Unit = {},
    val jump: (String) -> Unit = {}, val remove: (String) -> Unit = {},
    val move: (String, Int) -> Unit = { _, _ -> },
    val clearUpcoming: () -> Unit = {}, val undo: () -> Unit = {}, val reconnect: () -> Unit = {},
)
