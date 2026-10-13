package com.karthi.sungmobile.core.playback.history

import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.library.ListeningRecord

/** Counts elapsed playing time, never media-position deltas (seeking cannot earn a listen). */
class ListeningAccumulator(
    passId: String,
    track: CatalogItem,
    epoch: Long,
    startedAt: Long,
    monotonicMs: Long,
    checkpoint: ListeningRecord? = null,
) {
    var record: ListeningRecord = checkpoint ?: ListeningRecord("$passId:$epoch", passId, track, epoch, 0, startedAt, startedAt, false)
        private set
    private var lastTick = monotonicMs
    private var wasPlaying = false

    fun sample(playing: Boolean, durationMs: Long, monotonicMs: Long, wallMs: Long, track: CatalogItem): ListeningRecord {
        val elapsed = if (wasPlaying) (monotonicMs - lastTick).coerceAtLeast(0) else 0
        val listened = record.listenedMs + elapsed
        val threshold = durationMs.takeIf { it > 0 }?.let { minOf(30_000, (it / 2).coerceAtLeast(1)) } ?: 30_000
        record = record.copy(track = track, listenedMs = listened,
            lastPlayedAt = if (elapsed > 0) wallMs else record.lastPlayedAt,
            qualified = record.qualified || listened >= threshold)
        lastTick = monotonicMs
        wasPlaying = playing
        return record
    }
}
