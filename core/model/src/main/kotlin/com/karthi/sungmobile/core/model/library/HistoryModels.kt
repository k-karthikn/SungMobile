package com.karthi.sungmobile.core.model.library

import com.karthi.sungmobile.core.model.*
import kotlinx.coroutines.flow.Flow

data class HistoryPolicy(val privateMode: Boolean = false, val listening: Boolean = true,
    val searches: Boolean = true, val listeningEpoch: Long = 0, val searchEpoch: Long = 0) {
    val recordsListening get() = listening && !privateMode
    val recordsSearches get() = searches && !privateMode
}
enum class HistorySetting { PRIVATE_MODE, LISTENING, SEARCHES }
data class SearchRecord(val id: String, val query: String, val kind: CatalogKind)
data class ListeningRecord(val id: String, val passId: String, val track: CatalogItem, val epoch: Long,
    val listenedMs: Long, val startedAt: Long, val lastPlayedAt: Long, val qualified: Boolean)

interface HistoryRepository {
    fun policy(): Flow<HistoryPolicy>
    fun searches(): Flow<List<SearchRecord>>
    suspend fun setPolicy(setting: HistorySetting, enabled: Boolean)
    suspend fun clearListening()
    suspend fun clearSearches()
    suspend fun removeSearch(id: String)
    suspend fun recordSearch(query: String, kind: CatalogKind, policy: HistoryPolicy)
    suspend fun checkpoint(id: String): ListeningRecord?
    suspend fun recordListening(record: ListeningRecord)
}
