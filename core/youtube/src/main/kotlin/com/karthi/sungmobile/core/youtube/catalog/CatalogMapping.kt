package com.karthi.sungmobile.core.youtube.catalog

import com.karthi.sungmobile.core.model.*
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamInfo

internal fun InfoItem.toCatalogItem(requestedKind: CatalogKind): CatalogItem? = when (this) {
    is StreamInfoItem -> CatalogItem(url, CatalogKind.SONG, name, uploaderName.orEmpty(), url,
        thumbnails.maxByOrNull { it.height }?.url, duration.takeIf { it > 0 }?.times(1000))
    is PlaylistInfoItem -> CatalogItem(url,
        if (requestedKind == CatalogKind.ALBUM) CatalogKind.ALBUM else CatalogKind.PLAYLIST,
        name, uploaderName.orEmpty(), url, thumbnails.maxByOrNull { it.height }?.url)
    is ChannelInfoItem -> CatalogItem(url, CatalogKind.ARTIST, name, "YouTube artist / channel", url,
        thumbnails.maxByOrNull { it.height }?.url)
    else -> null
}

internal fun StreamInfo.toSong() = CatalogItem(url, CatalogKind.SONG, name, uploaderName.orEmpty(), url,
    thumbnails.maxByOrNull { it.height }?.url, duration.takeIf { it > 0 }?.times(1000))
