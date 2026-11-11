package com.karthi.sungmobile.feature.streak

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.designsystem.icon.SungIcon
import com.karthi.sungmobile.core.designsystem.icon.SungSymbol
import com.karthi.sungmobile.core.model.library.StreakProgress
import com.karthi.sungmobile.core.model.library.streakLengthOn
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
internal fun StreakCalendar(progress: StreakProgress, modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now()) {
    val currentMonth = YearMonth.from(today)
    var viewedYear by rememberSaveable(today.year, today.monthValue) { mutableIntStateOf(currentMonth.year) }
    var viewedMonth by rememberSaveable(today.year, today.monthValue) { mutableIntStateOf(currentMonth.monthValue) }
    val viewed = YearMonth.of(viewedYear, viewedMonth)
    val locale = LocalLocale.current.platformLocale
    val monthLabel = viewed.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))

    fun showMonth(month: YearMonth) {
        val safe = if (month.isAfter(currentMonth)) currentMonth else month
        viewedYear = safe.year
        viewedMonth = safe.monthValue
    }

    Surface(modifier, shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Your streak calendar", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold)
                Text("Every lit day is a qualified play. Its ember follows the length of that run.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { showMonth(viewed.minusMonths(1)) },
                    modifier = Modifier.size(44.dp).testTag("streak_month_previous")
                        .semantics { contentDescription = "Previous month" }) {
                    SungIcon(SungSymbol.Back, null)
                }
                Surface(onClick = { showMonth(currentMonth) }, shape = RoundedCornerShape(12.dp),
                    color = Color.Transparent, modifier = Modifier.weight(1f).testTag("streak_month_reset")
                        .semantics(mergeDescendants = true) {
                            contentDescription = "$monthLabel. Return to current month"
                        }) {
                    Text(monthLabel, Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                        .testTag("streak_calendar_month"), style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
                IconButton(onClick = { showMonth(viewed.plusMonths(1)) }, enabled = viewed < currentMonth,
                    modifier = Modifier.size(44.dp).testTag("streak_month_next")
                        .semantics { contentDescription = "Next month" }) {
                    SungIcon(SungSymbol.Chevron, null)
                }
            }
            AnimatedContent(targetState = viewed, label = "streak calendar month",
                transitionSpec = {
                    val direction = if (targetState > initialState) 1 else -1
                    (slideInHorizontally(tween(180)) { direction * it / 4 } + fadeIn(tween(180))) togetherWith
                        (slideOutHorizontally(tween(180)) { -direction * it / 4 } + fadeOut(tween(180)))
                },
                modifier = Modifier.fillMaxWidth().testTag("streak_calendar_month_content")
                    .pointerInput(viewed, currentMonth) {
                        var drag = 0f
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { change, amount ->
                                drag += amount
                                change.consume()
                            },
                            onDragEnd = {
                                if (drag < -60.dp.toPx() && viewed < currentMonth) showMonth(viewed.plusMonths(1))
                                else if (drag > 60.dp.toPx()) showMonth(viewed.minusMonths(1))
                                drag = 0f
                            },
                            onDragCancel = { drag = 0f },
                        )
                    }) { month ->
                MonthGrid(month, progress, today)
            }
        }
    }
}

@Composable
private fun MonthGrid(month: YearMonth, progress: StreakProgress, today: LocalDate) {
    val locale = LocalLocale.current.platformLocale
    val first = month.atDay(1)
    val cells = buildList<LocalDate?> {
        repeat(first.dayOfWeek.value - 1) { add(null) }
        repeat(month.lengthOfMonth()) { add(month.atDay(it + 1)) }
        while (size % 7 != 0) add(null)
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { label ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(label, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                week.forEach { date -> CalendarDay(date, progress, today) }
            }
        }
    }
}

@Composable
private fun RowScope.CalendarDay(date: LocalDate?, progress: StreakProgress, today: LocalDate) {
    val active = date != null && date in progress.activeDays
    val isToday = date == today
    val streakDay = if (date == null) 0 else streakLengthOn(date, progress.activeDays)
    val fill = if (active) emberColor(streakDay) else Color.Transparent
    val ink = if (fill.luminance() > .45f) Color.Black else Color.White
    val description = when {
        date == null -> ""
        active && isToday -> "${date.dayOfMonth}, today, active, streak day $streakDay"
        active -> "${date.dayOfMonth} active, streak day $streakDay"
        isToday -> "${date.dayOfMonth}, today, no qualified listening"
        else -> "${date.dayOfMonth}, no qualified listening"
    }
    Box(Modifier.weight(1f).aspectRatio(1f).semantics { contentDescription = description },
        contentAlignment = Alignment.Center) {
        if (date != null) {
            Box(Modifier.fillMaxSize().padding(2.dp)
                .background(if (active) fill else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = .36f), CircleShape)
                .then(if (isToday) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier),
                contentAlignment = Alignment.Center) {
                Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.labelSmall,
                    color = if (active) ink else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isToday || active) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}
