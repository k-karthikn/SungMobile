package com.karthi.sungmobile.core.youtube.catalog

import com.karthi.sungmobile.core.model.*
import com.karthi.sungmobile.core.model.catalog.*
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.ContentAvailability
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage
import org.schabi.newpipe.extractor.Page
import java.security.MessageDigest
import java.util.UUID

/** Bounded process-local continuations. Stable token fingerprints expose repeated pages to callers. */
internal class CatalogPages {
    private val continuations = object : LinkedHashMap<PageToken, () -> CatalogPage>(64, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<PageToken, () -> CatalogPage>) = size > 128
    }
    fun next(token: PageToken): CatalogPage {
        val fetch = synchronized(continuations) { continuations[token] } ?: throw SourceException(SourceFailure.EXPIRED_PAGE)
        return fetch()
    }
    fun <T : InfoItem> page(title: String, result: InfoItemsPage<T>, kind: CatalogKind,
        details: CatalogDetails? = null, context: String = UUID.randomUUID().toString(), fetch: (Page) -> InfoItemsPage<T>): CatalogPage {
        val restricted = result.items.filterIsInstance<StreamInfoItem>().filter {
            it.contentAvailability != ContentAvailability.AVAILABLE && it.contentAvailability != ContentAvailability.UNKNOWN
        }
        val unavailable = restricted.map { UnavailableCatalogEntry(it.name, when (it.contentAvailability) {
            ContentAvailability.MEMBERSHIP -> "Membership required"
            ContentAvailability.PAID -> "Paid content"
            else -> "Upcoming content"
        }) }
        val mapped = result.items.filterNot { it is StreamInfoItem && it in restricted }.mapNotNull { it.toCatalogItem(kind) }
        // Source order and duplicate occurrences are meaningful in a collection.
        val items = if (details == null) mapped.distinctBy { it.id } else mapped
        if (items.isEmpty() && unavailable.isEmpty() && result.errors.isNotEmpty()) throw SourceException(SourceFailure.EXTRACTION, result.errors.first())
        val next = result.nextPage?.takeIf(Page::isValid)?.let { page ->
            val bytes = listOf(page.url, page.id, page.ids, page.cookies?.toSortedMap(), page.body?.contentHashCode()).joinToString("|").toByteArray()
            val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
            PageToken("$context:$hash").also { token -> synchronized(continuations) {
                continuations[token] = { page(title, fetch(page), kind, details, context, fetch) }
            } }
        }
        return CatalogPage(title, items, next, result.errors.isNotEmpty() || mapped.size < result.items.size, details = details, unavailable = unavailable)
    }
    fun clear() = synchronized(continuations) { continuations.clear() }
}
