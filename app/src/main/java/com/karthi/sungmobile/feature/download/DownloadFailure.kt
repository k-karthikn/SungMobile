package com.karthi.sungmobile.feature.download

import android.util.Log
import com.karthi.sungmobile.core.model.SourceException
import java.io.IOException

/** User-visible causes only; never expose signed stream URLs or provider exception text. */
internal fun downloadFailureMessage(error: Exception): String = when (error) {
    is AudioHttpException -> when (error.status) {
        403, 410 -> "Audio link expired. Try Download again."
        429 -> "Audio source is busy. Try again later."
        else -> "Audio server error (${error.status}). Try again."
    }
    is SourceException -> "Could not prepare this song. Try again or choose another."
    is SecurityException -> "Storage access was denied. Try again."
    is IOException -> "Download interrupted. Check your connection and try again."
    else -> "Could not save this song. Try again."
}

internal fun logDownloadFailure(error: Exception) {
    // Exception messages may contain signed URLs. Log only type and code locations.
    val frames = error.stackTrace.take(5).joinToString(" | ") {
        "${it.className}.${it.methodName}:${it.lineNumber}"
    }
    Log.e("SungAudioDownload", "${error.javaClass.name} at $frames; cause=${error.cause?.javaClass?.name}")
}
