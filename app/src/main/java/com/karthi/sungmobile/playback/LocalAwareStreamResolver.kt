package com.karthi.sungmobile.playback

import android.net.Uri
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.ResolvedStream
import com.karthi.sungmobile.core.model.SourceException
import com.karthi.sungmobile.core.model.SourceFailure
import com.karthi.sungmobile.core.model.StreamResolver

/** Local media stays in Media3's content data source and never visits the YouTube extractor. */
internal class LocalAwareStreamResolver(private val online: StreamResolver) : StreamResolver {
    override suspend fun resolve(track: CatalogItem): ResolvedStream {
        if (!track.id.startsWith("local:")) return online.resolve(track)
        val uri = Uri.parse(track.url)
        if (uri.scheme != "content" || track.id != "local:$uri")
            throw SourceException(SourceFailure.INVALID_LINK)
        return ResolvedStream(track, uri.toString(), null, emptyMap(), 0, null)
    }
}
