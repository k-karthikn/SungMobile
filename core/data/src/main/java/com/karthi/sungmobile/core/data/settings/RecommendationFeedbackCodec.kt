package com.karthi.sungmobile.core.data.settings

import com.karthi.sungmobile.core.model.discovery.RecommendationFeedback

/** A small, bounded DataStore representation of local song feedback. */
internal object RecommendationFeedbackCodec {
    private val songKey = Regex("youtube:[A-Za-z0-9_-]{11}")

    fun validKey(value: String): Boolean = songKey.matches(value)

    fun decode(raw: String): Map<String, RecommendationFeedback> = raw.lineSequence()
        .mapNotNull { line ->
            val key = line.substringBeforeLast(':', "")
            val action = runCatching { RecommendationFeedback.valueOf(line.substringAfterLast(':')) }.getOrNull()
            if (validKey(key) && action != null) key to action else null
        }.toList().takeLast(200).toMap()

    fun encode(values: Map<String, RecommendationFeedback>): String = values.entries
        .filter { validKey(it.key) }.takeLast(200)
        .joinToString("\n") { "${it.key}:${it.value.name}" }
}
