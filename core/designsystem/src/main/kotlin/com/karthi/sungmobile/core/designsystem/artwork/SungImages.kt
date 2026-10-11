package com.karthi.sungmobile.core.designsystem.artwork

import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import okio.Path.Companion.toOkioPath

/** One bounded cache for the whole app. No per-screen loaders or full-size bitmap transforms. */
fun configureSungImages() {
    SingletonImageLoader.setSafe { context -> createImageLoader(context) }
}

private fun createImageLoader(context: Context): ImageLoader = ImageLoader.Builder(context)
    .memoryCache { MemoryCache.Builder().maxSizeBytes(32L * 1024 * 1024).build() }
    .diskCache {
        DiskCache.Builder().directory(context.cacheDir.resolve("artwork").toOkioPath()).maxSizeBytes(96L * 1024 * 1024).build()
    }
    .build()
