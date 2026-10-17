package com.karthi.sungmobile.core.model.catalog

import com.karthi.sungmobile.core.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

enum class BatchStop { COMPLETE, PARTIAL, LIMIT, TIMEOUT, PAGE_LOOP, FAILED }
data class CollectionBatch(val page: CatalogPage, val stop: BatchStop, val failure: SourceFailure? = null)

/** Fetch first, commit later. No queue or library mutation is possible inside this loader. */
class CollectionBatchLoader(private val source: CatalogSource, private val maxEntries: Int = 500,
    private val maxPages: Int = 50, private val timeoutMs: Long = 90_000) {
    suspend fun load(initial: CatalogPage, progress: (Int) -> Unit): CollectionBatch {
        var page = initial.copy(items = initial.items.take(maxEntries))
        if (initial.items.size > maxEntries) return CollectionBatch(page, BatchStop.LIMIT)
        val visited = mutableSetOf<PageToken>()
        var requests = 0
        val outcome = withTimeoutOrNull(timeoutMs) {
            try {
                while (page.next != null) {
                    progress(page.items.size)
                    if (page.items.size >= maxEntries || requests >= maxPages) return@withTimeoutOrNull CollectionBatch(page, BatchStop.LIMIT)
                    val token = requireNotNull(page.next)
                    if (!visited.add(token)) return@withTimeoutOrNull CollectionBatch(page, BatchStop.PAGE_LOOP)
                    val next = source.nextPage(token)
                    requests++
                    if (next.items.isEmpty() && next.next != null) return@withTimeoutOrNull CollectionBatch(page, BatchStop.PAGE_LOOP)
                    val remaining = maxEntries - page.items.size
                    page = page.copy(items = page.items + next.items.take(remaining), next = next.next,
                        partial = page.partial || next.partial, unavailable = page.unavailable + next.unavailable)
                    if (next.items.size > remaining) return@withTimeoutOrNull CollectionBatch(page, BatchStop.LIMIT)
                }
                CollectionBatch(page, if (page.partial) BatchStop.PARTIAL else BatchStop.COMPLETE)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { CollectionBatch(page, BatchStop.FAILED, (error as? SourceException)?.failure ?: SourceFailure.EXTRACTION) }
        }
        return outcome ?: CollectionBatch(page, BatchStop.TIMEOUT)
    }
}
