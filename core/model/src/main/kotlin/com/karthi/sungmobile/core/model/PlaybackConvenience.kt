package com.karthi.sungmobile.core.model

data class PlaybackSpeed(val tenths: Int = 10, val preservePitch: Boolean = true) {
    init { require(tenths in 5..20) }
    val multiplier: Float get() = tenths / 10f
}

enum class SleepMode { DURATION, END_TRACK, END_QUEUE }

data class SleepTimerStatus(
    val mode: SleepMode,
    val remainingMs: Long = 0,
    val fade: Boolean = false,
)

data class SavedSession(
    val id: String,
    val name: String,
    val savedAtMs: Long,
    val snapshot: QueueSnapshot,
    val speed: PlaybackSpeed,
)

data class ResumePoint(val videoId: String, val positionMs: Long, val updatedAtMs: Long)
