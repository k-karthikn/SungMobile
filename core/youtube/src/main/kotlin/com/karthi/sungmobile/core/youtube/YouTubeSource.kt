package com.karthi.sungmobile.core.youtube

import com.karthi.sungmobile.core.model.*
import com.karthi.sungmobile.core.youtube.catalog.*
import com.karthi.sungmobile.core.youtube.home.MusicBrowseGateway
import com.karthi.sungmobile.core.model.discovery.HomeFeedSource
import com.karthi.sungmobile.core.model.discovery.HomeShelf
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.*
import java.io.Closeable

/** Catalog work cannot occupy the stream resolver's executor. No provider DTO leaves this module. */
class YouTubeSource : CatalogSource, StreamResolver, DownloadStreamResolver, RelatedSource, HomeFeedSource, Closeable {
    private val catalogExecution = ExtractorExecution(2)
    private val streamExecution = ExtractorExecution(1)
    private val catalog = CatalogExtractor()
    private val streams = StreamExtractor()
    private val musicBrowse = MusicBrowseGateway()
    init { initializeExtractor() }
    override suspend fun search(query: String, kind: CatalogKind) = catalogExecution.run { catalog.search(query, kind) }
    override suspend fun nextPage(token: PageToken) = catalogExecution.run { catalog.nextPage(token) }
    override suspend fun open(url: String): CatalogPage {
        val browse = runCatching { YouTubeLink.parse(url) }.getOrNull() as? YouTubeLink.Browse
        if (browse != null && (browse.browseId.startsWith("MPRE") || browse.browseId.startsWith("VL"))) {
            val kind = if (browse.browseId.startsWith("MPRE")) CatalogKind.ALBUM else CatalogKind.PLAYLIST
            return musicBrowse.openCollection(browse.browseId, url, kind)
        }
        return catalogExecution.run { catalog.open(url) }
    }
    override suspend fun homeShelves(): List<HomeShelf> = musicBrowse.homeShelves()
    override suspend fun resolve(track: CatalogItem) = streamExecution.run { streams.resolve(track) }
    override suspend fun resolveDownload(track: CatalogItem) = streamExecution.run { streams.resolveDownload(track) }
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
