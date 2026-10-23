package com.karthi.sungmobile.feature.library

import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.CatalogSource
import com.karthi.sungmobile.core.model.SourceException
import com.karthi.sungmobile.core.model.SourceFailure
import kotlinx.coroutines.CancellationException

internal enum class AvailabilityOutcome { AVAILABLE, UNAVAILABLE, UNKNOWN }

/** A network or provider failure is never evidence that a saved song should be deleted. */
internal suspend fun probeAvailability(source: CatalogSource, track: CatalogItem): AvailabilityOutcome = try {
    source.open(track.url)
    AvailabilityOutcome.AVAILABLE
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: SourceException) {
    if (error.failure == SourceFailure.UNAVAILABLE) AvailabilityOutcome.UNAVAILABLE else AvailabilityOutcome.UNKNOWN
} catch (_: Exception) {
    AvailabilityOutcome.UNKNOWN
}
