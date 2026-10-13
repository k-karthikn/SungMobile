package com.karthi.sungmobile.feature.library

import androidx.compose.runtime.*
import androidx.compose.material3.*
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.library.libraryKeyOrNull

/** Shared entry points, supplied once by the application host. No repository lives in a row. */
data class LibraryUi(val liked: Set<String> = emptySet(), val toggle: (CatalogItem) -> Unit = {},
    val add: (List<CatalogItem>) -> Unit = {}, val saveQueue: () -> Unit = {})
val LocalLibraryUi = staticCompositionLocalOf { LibraryUi() }

@Composable
fun LibraryMenuItems(track: CatalogItem, dismiss: () -> Unit) {
    val library = LocalLibraryUi.current
    DropdownMenuItem(text = { Text(if (track.libraryKeyOrNull() in library.liked) "Unlike" else "Like") },
        onClick = { library.toggle(track); dismiss() })
    DropdownMenuItem(text = { Text("Add to playlist") }, onClick = { library.add(listOf(track)); dismiss() })
}

@Composable
fun TrackLibraryButtons(track: CatalogItem) {
    val library = LocalLibraryUi.current
    TextButton({ library.toggle(track) }) { Text(if (track.libraryKeyOrNull() in library.liked) "Unlike" else "Like") }
    TextButton({ library.add(listOf(track)) }) { Text("Add to playlist") }
}
