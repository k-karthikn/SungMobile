package com.karthi.sungmobile.core.model.discovery

import com.karthi.sungmobile.core.model.*
import com.karthi.sungmobile.core.model.library.*
import kotlinx.coroutines.flow.Flow
import kotlin.random.Random

data class HomeData(val liked: List<CatalogItem> = emptyList(), val recent: List<CatalogItem> = emptyList(),
    val pinned: List<LocalPlaylist> = emptyList(), val privateMode: Boolean = false, val historyEpoch: Long = 0)
interface HomeRepository { fun observe(): Flow<HomeData> }
fun localMix(tracks: List<CatalogItem>, seed: Long): List<CatalogItem> = tracks
    .distinctBy { it.libraryKeyOrNull() ?: it.id }.sortedBy { it.libraryKeyOrNull() ?: it.id }.shuffled(Random(seed)).take(50)
