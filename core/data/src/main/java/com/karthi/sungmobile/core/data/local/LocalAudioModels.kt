package com.karthi.sungmobile.core.data.local

import android.net.Uri
import com.karthi.sungmobile.core.model.CatalogItem
import java.util.Locale

const val SUNG_MUSIC_PATH = "Music/SungMobile/"

data class LocalSong(
    val track: CatalogItem,
    val album: String?,
    val identity: String,
    val sidecarArt: Uri? = null,
)

data class LocalScan(
    val songs: List<LocalSong>,
    val needsAudioPermission: Boolean,
    val unreadableFolders: Int = 0,
)

internal val LOCAL_IMAGE_EXTENSIONS = listOf("jpg", "jpeg", "png", "webp", "gif")

/** Match Sung's track-specific cover first, followed by common album-cover names. */
internal fun localCover(filename: String, images: Map<String, Uri>): Uri? {
    val stem = filename.substringBeforeLast('.', filename).lowercase(Locale.ROOT)
    return (listOf(stem, "cover", "folder", "front", "artwork")).firstNotNullOfOrNull { base ->
        LOCAL_IMAGE_EXTENSIONS.firstNotNullOfOrNull { extension -> images["$base.$extension"] }
    }
}

internal fun filenameTitle(name: String): String = name.substringBeforeLast('.', name).ifBlank { "Untitled" }
