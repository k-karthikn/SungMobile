package com.karthi.sungmobile.feature.local

import androidx.core.content.FileProvider

/** Dedicated provider identity so local cover URIs cannot collide with other app files. */
class LocalArtworkFileProvider : FileProvider()
