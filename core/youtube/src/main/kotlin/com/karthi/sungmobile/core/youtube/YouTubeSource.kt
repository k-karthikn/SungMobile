package com.karthi.sungmobile.core.youtube

import com.karthi.sungmobile.core.model.*
import com.karthi.sungmobile.core.youtube.catalog.*
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.*
import java.io.Closeable

/** Catalog work cannot occupy the stream resolver's executor. No provider DTO leaves this module. */
class YouTubeSource : CatalogSource, StreamResolver, RelatedSource, Closeable {
    private val catalogExecution = ExtractorExecution(2)
    private val streamExecution = ExtractorExecution(1)
    private val catalog = CatalogExtractor()
    private val streams = StreamExtractor()
    init { initializeExtractor() }
    override suspend fun search(query: String, kind: CatalogKind) = catalogExecution.run { catalog.search(query, kind) }
    override suspend fun nextPage(token: PageToken) = catalogExecution.run { catalog.nextPage(token) }
    override suspend fun open(url: String) = catalogExecution.run { catalog.open(url) }
    override suspend fun resolve(track: CatalogItem) = streamExecution.run { streams.resolve(track) }
    override suspend fun related(seed: CatalogItem, limit: Int) = catalogExecution.run { catalog.related(seed, limit) }
    override fun close() { catalogExecution.close(); streamExecution.close(); catalog.clear() }
    companion object {
        private var initialized = false
        @Synchronized private fun initializeExtractor() {
            if (!initialized) {
                NewPipe.init(ExtractorDownloader(), Localization("en", "IN"), ContentCountry("IN"))
                initialized = true
            }
        }
    }
}
