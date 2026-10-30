package com.karthi.sungmobile.feature.library

import com.karthi.sungmobile.core.model.library.DailyListening
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun activityWindow(activity: List<DailyListening>, days: Int, today: LocalDate): List<DailyListening> {
    require(days == 7 || days == 30)
    val byDay = activity.associateBy(DailyListening::day)
    return List(days) { index ->
        val date = today.minusDays((days - 1 - index).toLong()).toString()
        byDay[date] ?: DailyListening(date, 0, 0)
    }
}

internal fun listeningTime(ms: Long): String {
    val minutes = ms.coerceAtLeast(0) / 60_000
    return if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m" else "${minutes}m"
}

internal fun averageTime(ms: Long): String {
    val seconds = ms.coerceAtLeast(0) / 1000
    return if (seconds >= 3600) listeningTime(ms)
        else if (seconds >= 60) "${seconds / 60}m ${seconds % 60}s" else "${seconds}s"
}

internal fun chartTime(ms: Long, compact: Boolean = false): String {
    val minutes = ms.coerceAtLeast(0) / 60_000
    return if (!compact && minutes >= 60) "${minutes / 60}h${minutes % 60}m" else "${minutes}m"
}

internal fun dayLabel(day: String, locale: Locale): String = runCatching {
    LocalDate.parse(day).format(DateTimeFormatter.ofPattern("d MMM", locale))
}.getOrDefault(day)

internal fun timeLabel(ms: Long, locale: Locale): String = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("h:mm a", locale))
