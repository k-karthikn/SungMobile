package com.karthi.sungmobile.core.data.settings

import com.karthi.sungmobile.core.model.PlaybackSpeed
import com.karthi.sungmobile.core.model.discovery.RecommendationFeedback

/** Settings that make sense on another device. Folder permissions and caches are excluded. */
data class PortableSettings(
    val appearance: AppearanceSettings,
    val lyrics: LyricsSettings,
    val speed: PlaybackSpeed,
    val smartShuffle: Boolean,
    val relatedAutoplay: Boolean,
    val lyricOffsets: Map<String, Int>,
    val trackTrims: Map<String, Int>,
    val recommendationsEnabled: Boolean = true,
    val recommendationFeedback: Map<String, RecommendationFeedback> = emptyMap(),
)

interface PortableSettingsStore {
    suspend fun exportPortable(): PortableSettings
    suspend fun importPortable(value: PortableSettings)
}
