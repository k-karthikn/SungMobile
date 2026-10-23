package com.karthi.sungmobile.feature.downloads

import androidx.compose.runtime.staticCompositionLocalOf
import com.karthi.sungmobile.core.model.CatalogItem

data class DownloadUi(val complete: Set<String> = emptySet(), val enqueue: (CatalogItem) -> Unit = {})
val LocalDownloadUi = staticCompositionLocalOf { DownloadUi() }
