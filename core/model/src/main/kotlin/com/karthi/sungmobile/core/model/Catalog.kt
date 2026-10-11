package com.karthi.sungmobile.core.model

enum class CatalogKind { SONG, ALBUM, ARTIST, PLAYLIST }

data class CatalogItem(
    val id: String,
    val kind: CatalogKind,
    val title: String,
    val subtitle: String,
    val url: String,
    val artworkUrl: String? = null,
    val durationMs: Long? = null,
)

/** An opaque, session-local continuation. Provider objects never reach the UI. */
@JvmInline
value class PageToken(val value: String)

sealed interface CatalogNotice {
    data class ArtistBrowseUnavailable(val artistName: String) : CatalogNotice
}

data class CatalogPage(
    val title: String,
    val items: List<CatalogItem>,
    val next: PageToken? = null,
    val partial: Boolean = false,
    val notice: CatalogNotice? = null,
)

interface CatalogSource {
    suspend fun search(query: String, kind: CatalogKind): CatalogPage
    suspend fun nextPage(token: PageToken): CatalogPage
    suspend fun open(url: String): CatalogPage
}

interface StreamResolver {
    suspend fun resolve(track: CatalogItem): ResolvedStream
}

/** Signed URLs are transient: never persist or log this object. */
class ResolvedStream(
    val track: CatalogItem,
    val url: String,
    val mimeType: String?,
    val headers: Map<String, String>,
    val bitrate: Int,
    val expiresAtMs: Long?,
) {
    fun isFresh(nowMs: Long): Boolean = expiresAtMs?.let { it > nowMs + 120_000 } ?: true
    override fun toString(): String = "ResolvedStream(track=${track.id}, bitrate=$bitrate)"
}

enum class SourceFailure {
    INVALID_LINK, NETWORK, UNAVAILABLE, AUTHENTICATION_REQUIRED, RATE_LIMITED,
    UNSUPPORTED, EXTRACTION, EXPIRED_PAGE,
}

class SourceException(val failure: SourceFailure, cause: Throwable? = null) :
    Exception(failure.name, cause)
