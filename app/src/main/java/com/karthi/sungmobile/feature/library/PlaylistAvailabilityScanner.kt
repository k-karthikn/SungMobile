package com.karthi.sungmobile.feature.library

import com.karthi.sungmobile.core.model.CatalogSource
import com.karthi.sungmobile.core.model.library.LibrarySong
import kotlinx.coroutines.*

/** Progress follows occurrence IDs, so removing/reordering rows cannot skip unchecked songs. */
internal class PlaylistAvailabilityScanner(private val scope: CoroutineScope, private val source: CatalogSource,
    private val publish: (Map<String, AvailabilityOutcome>, Boolean) -> Unit) {
    private val results = linkedMapOf<String, AvailabilityOutcome>()
    private var retainedIds: Set<String> = emptySet()
    private var job: Job? = null
    private var generation = 0L

    fun retain(ids: Set<String>) {
        retainedIds = ids
        results.keys.retainAll(ids)
        publish(results.toMap(), job?.isActive == true)
    }
    fun scan(rows: List<LibrarySong>) = start(rows.filter { it.rowId !in results })
    fun retryUnknown(rows: List<LibrarySong>) = start(rows.filter { results[it.rowId] == AvailabilityOutcome.UNKNOWN })

    private fun start(rows: List<LibrarySong>) {
        if (job?.isActive == true || rows.isEmpty()) return
        val request = ++generation
        publish(results.toMap(), true)
        job = scope.launch {
            try {
                for (row in rows.take(100)) {
                    ensureActive()
                    if (row.rowId !in retainedIds) continue
                    val outcome = probeAvailability(source, row.track)
                    ensureActive()
                    if (request != generation) return@launch
                    if (row.rowId in retainedIds) results[row.rowId] = outcome
                    publish(results.toMap(), true)
                }
            } finally { if (request == generation) publish(results.toMap(), false) }
        }
    }
    fun cancel() { generation++; job?.cancel(); job = null; publish(results.toMap(), false) }
    fun reset() { cancel(); results.clear(); retainedIds = emptySet(); publish(emptyMap(), false) }
}
