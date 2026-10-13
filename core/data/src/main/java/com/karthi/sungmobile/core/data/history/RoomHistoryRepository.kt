package com.karthi.sungmobile.core.data.history

import com.karthi.sungmobile.core.data.db.*
import com.karthi.sungmobile.core.data.library.*
import com.karthi.sungmobile.core.model.CatalogKind
import com.karthi.sungmobile.core.model.library.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.util.Locale

/** Privacy changes and writes share the same Room transaction boundary, including across restarts. */
class RoomHistoryRepository(private val db: SungDatabase, private val now: () -> Long = System::currentTimeMillis) : HistoryRepository {
    private fun current() = db.historyDao().policy() ?: HistoryPolicyEntity()
    override fun policy(): Flow<HistoryPolicy> = db.changes("history_policy").map {
        withContext(Dispatchers.IO) { current().toPolicy() }
    }.distinctUntilChanged()
    override fun searches() = db.changes("search_history").map {
        withContext(Dispatchers.IO) { db.historyDao().searches().map { SearchRecord(it.id, it.query, CatalogKind.valueOf(it.kind)) } }
    }.distinctUntilChanged()
    override suspend fun setPolicy(setting: HistorySetting, enabled: Boolean) = db.transaction {
        val policy = current()
        when (setting) {
            HistorySetting.PRIVATE_MODE -> if (policy.privateMode != enabled) {
                policy.privateMode = enabled; policy.listeningEpoch++; policy.searchEpoch++
            }
            HistorySetting.LISTENING -> if (policy.listening != enabled) { policy.listening = enabled; policy.listeningEpoch++ }
            HistorySetting.SEARCHES -> if (policy.searches != enabled) { policy.searches = enabled; policy.searchEpoch++ }
        }
        db.historyDao().setPolicy(policy)
    }
    override suspend fun clearListening() = db.transaction {
        val policy = current().apply { listeningEpoch++ }
        db.historyDao().setPolicy(policy)
        db.historyDao().clearListening()
    }
    override suspend fun clearSearches() = db.transaction {
        val policy = current().apply { searchEpoch++ }
        db.historyDao().setPolicy(policy)
        db.historyDao().clearSearches()
    }
    override suspend fun removeSearch(id: String) = db.transaction {
        val policy = current().apply { searchEpoch++ }
        db.historyDao().setPolicy(policy)
        db.historyDao().removeSearch(id)
    }
    override suspend fun recordSearch(query: String, kind: CatalogKind, policy: HistoryPolicy) = db.transaction {
        val current = current().toPolicy()
        if (!policy.recordsSearches || !current.recordsSearches || current.searchEpoch != policy.searchEpoch) return@transaction
        val cleaned = query.trim().take(500)
        if (cleaned.isEmpty()) return@transaction
        db.historyDao().saveSearch(SearchEntity().apply {
            id = "${kind.name}:${cleaned.lowercase(Locale.ROOT)}"
            this.query = cleaned; this.kind = kind.name; submittedAt = now()
        })
        db.historyDao().trimSearches()
    }
    override suspend fun checkpoint(id: String): ListeningRecord? = db.transaction {
        val row = db.historyDao().event(id) ?: return@transaction null
        val track = db.libraryDao().track(row.trackKey)?.toTrack() ?: return@transaction null
        ListeningRecord(row.id, row.passId, track, row.epoch, row.listenedMs, row.startedAt, row.lastPlayedAt, row.qualified)
    }
    override suspend fun recordListening(record: ListeningRecord) = db.transaction {
        val policy = current().toPolicy()
        if (!policy.recordsListening || policy.listeningEpoch != record.epoch) return@transaction
        val old = db.historyDao().event(record.id)
        if (old != null && old.listenedMs >= record.listenedMs) return@transaction
        val key = db.libraryDao().saveMetadata(record.track)
        db.historyDao().saveEvent(ListeningEntity().apply {
            id = record.id; passId = record.passId; trackKey = key; epoch = record.epoch
            listenedMs = record.listenedMs.coerceAtLeast(0); startedAt = record.startedAt
            lastPlayedAt = record.lastPlayedAt; qualified = record.qualified
        })
    }
}
