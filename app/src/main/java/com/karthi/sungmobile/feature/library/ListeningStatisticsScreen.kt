package com.karthi.sungmobile.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karthi.sungmobile.core.designsystem.artwork.ArtworkMask
import com.karthi.sungmobile.core.designsystem.artwork.SungArtwork
import com.karthi.sungmobile.core.designsystem.component.SungLoading
import com.karthi.sungmobile.core.designsystem.icon.SungIcon
import com.karthi.sungmobile.core.designsystem.icon.SungSymbol
import com.karthi.sungmobile.core.model.library.ListeningEvent
import com.karthi.sungmobile.core.model.library.ListeningStats
import com.karthi.sungmobile.core.model.library.TopSong
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.roundToInt

/** A read-only dashboard. Its controls change only the statistics view. */
@Composable
internal fun ListeningStatisticsScreen(state: AdvancedLibraryState, onBack: () -> Unit,
    onRetry: () -> Unit, onLoadMore: () -> Unit, modifier: Modifier = Modifier) {
    var days by rememberSaveable { mutableIntStateOf(7) }
    var allTop by rememberSaveable { mutableStateOf(false) }
    var allRecent by rememberSaveable { mutableStateOf(false) }
    val stats = state.stats
    LazyColumn(modifier.fillMaxSize().testTag("listening_statistics"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item("heading") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onBack) { SungIcon(SungSymbol.Back, "Back to Library") }
                Text("Listening statistics", Modifier.weight(1f).padding(start = 8.dp),
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            }
        }
        state.message?.let { message -> item("message") {
            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(message, Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer)
                    TextButton(onRetry) { Text("Retry") }
                }
            }
        } }
        if (state.loadingStats) item("loading") { SungLoading(label = "Loading listening statistics…") }
        else if (stats.privateMode) item("private") {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(28.dp)) {
                Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SungIcon(SungSymbol.Equalizer, null, Modifier.size(36.dp))
                    Text("Statistics are hidden", style = MaterialTheme.typography.titleLarge)
                    Text("Private mode is on. Turn it off in History & privacy to see your saved listening activity.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            item("summary") { StatisticsHero(stats) }
            item("metrics") { StatisticsMetrics(stats) }
            item("activity") { ListeningActivity(stats, days) { days = it } }
            item("top_songs") { TopSongsSection(stats.topSongs, allTop) { allTop = !allTop } }
            item("recent") { RecentListeningSection(stats.recentEvents, allRecent, state.statsMore,
                state.loadingMore, { allRecent = !allRecent }, onLoadMore) }
        }
    }
}
@Composable
private fun StatisticsHero(stats: ListeningStats) {
    Surface(modifier = Modifier.testTag("stats_total"), shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primaryContainer) {
        Box(Modifier.fillMaxWidth()) {
            Box(Modifier.align(Alignment.TopEnd).offset(x = 52.dp, y = (-78).dp)
                .size(186.dp).clip(StatisticsFlowerShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = .12f)))
            Box(Modifier.align(Alignment.BottomEnd).offset(x = (-56).dp, y = 37.dp)
                .size(98.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = .08f)))
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Total listening time", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(listeningTime(stats.listenedMs), style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("${stats.qualifiedPlays} qualified plays", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Box(Modifier.size(104.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(98.dp).clip(StatisticsFlowerShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = .20f)))
                    Surface(Modifier.size(82.dp),
                        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 20.dp,
                            bottomEnd = 30.dp, bottomStart = 20.dp),
                        color = MaterialTheme.colorScheme.primary) {
                        Box(contentAlignment = Alignment.Center) {
                            SungIcon(SungSymbol.Equalizer, null, Modifier.size(48.dp))
                        }
                    }
                }
            }
            Text("Only qualified listening counts. Clearing listening history resets these statistics.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

@Composable
private fun StatisticsMetrics(stats: ListeningStats) {
    val scheme = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MetricCard(stats.qualifiedPlays.toString(), "Qualified plays", SungSymbol.Equalizer,
            scheme.primaryContainer, scheme.onPrimaryContainer, scheme.primary,
            StatisticsFlowerShape, Modifier.weight(1f))
        MetricCard(stats.uniqueSongs.toString(), "Unique songs", SungSymbol.Disc,
            scheme.secondaryContainer, scheme.onSecondaryContainer, scheme.secondary,
            RoundedCornerShape(topStart = 13.dp, topEnd = 9.dp, bottomEnd = 13.dp, bottomStart = 9.dp),
            Modifier.weight(1f))
        MetricCard(if (stats.qualifiedPlays == 0L) "0s" else averageTime(stats.listenedMs / stats.qualifiedPlays),
            "Average per play", SungSymbol.Clock,
            scheme.tertiaryContainer, scheme.onTertiaryContainer, scheme.tertiary,
            CircleShape, Modifier.weight(1f))
    }
}

private val StatisticsFlowerShape = GenericShape { size, _ ->
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val radius = minOf(size.width, size.height) * .49f
    repeat(65) { step ->
        val angle = step * 2.0 * PI / 64.0
        val petalRadius = radius * (.91 + .09 * cos(8.0 * angle)).toFloat()
        val x = centerX + petalRadius * cos(angle).toFloat()
        val y = centerY + petalRadius * sin(angle).toFloat()
        if (step == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

@Composable
private fun MetricCard(value: String, label: String, icon: SungSymbol, container: Color, ink: Color,
    iconColor: Color, iconShape: Shape,
    modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = container) {
        Row(Modifier.heightIn(min = 54.dp).padding(horizontal = 7.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Surface(Modifier.size(30.dp), shape = iconShape,
                color = iconColor.copy(alpha = .20f), contentColor = iconColor) {
                Box(contentAlignment = Alignment.Center) {
                    SungIcon(icon, null, Modifier.size(19.dp))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                    color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp,
                    lineHeight = 11.sp), color = ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ListeningActivity(stats: ListeningStats, days: Int, changeDays: (Int) -> Unit) {
    val activity = activityWindow(stats.dailyActivity, days, LocalDate.now())
    val peak = activity.maxOfOrNull { it.listenedMs }?.coerceAtLeast(1L) ?: 1L
    val largeText = LocalDensity.current.fontScale >= 1.4f
    val locale = LocalLocale.current.platformLocale
    Surface(modifier = Modifier.testTag("stats_activity"), shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (largeText) {
                Text("Listening activity", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold)
                ActivityPeriodChips(days, changeDays)
            } else {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Listening activity", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                    ActivityPeriodChips(days, changeDays)
                }
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(listeningTime(activity.sumOf { it.listenedMs }),
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("stats_activity_total"))
                Text("in the last $days days", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (days == 7) 8.dp else 3.dp),
                verticalAlignment = Alignment.Bottom) {
                activity.forEach { day ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Box(Modifier.fillMaxWidth().height(128.dp), contentAlignment = Alignment.BottomCenter) {
                            val barHeight = if (day.listenedMs == 0L) 5.dp
                                else (12 + 84f * day.listenedMs / peak).roundToInt().dp
                            Box(Modifier.fillMaxWidth().height(barHeight)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (day.listenedMs > 0) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceContainerHighest))
                            if (day.listenedMs > 0L && days == 7 && !largeText) Text(chartTime(day.listenedMs),
                                Modifier.align(Alignment.BottomCenter).offset(y = -(barHeight + 4.dp)),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                        if (days == 7) Text(if (largeText) LocalDate.parse(day.day).dayOfMonth.toString()
                            else dayLabel(day.day, locale), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        if (days == 7) Text(chartTime(day.listenedMs, largeText), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
            }
            if (days == 30) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(dayLabel(activity.first().day, locale), style = MaterialTheme.typography.labelSmall)
                Text(dayLabel(activity.last().day, locale), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun ActivityPeriodChips(days: Int, changeDays: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(days == 7, { changeDays(7) }, { Text("7 days") },
            modifier = Modifier.testTag("stats_7_days"), shape = RoundedCornerShape(12.dp))
        FilterChip(days == 30, { changeDays(30) }, { Text("30 days") },
            modifier = Modifier.testTag("stats_30_days"), shape = RoundedCornerShape(12.dp))
    }
}

@Composable
private fun TopSongsSection(songs: List<TopSong>, all: Boolean, toggle: () -> Unit) {
    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionHeader("Top songs", if (songs.size > 5) (if (all) "Show less" else "See all") else null, toggle)
            if (songs.isEmpty()) Text("Your top songs will appear after qualified listening.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            else {
                songs.take(3).forEachIndexed { rank, song ->
                    RankedSongHighlight(rank + 1, song, songs.first().plays)
                }
                songs.drop(3).take(if (all) Int.MAX_VALUE else 2).forEachIndexed { index, song ->
                    RankedSongRow(index + 4, song, songs.first().plays)
                }
            }
        }
    }
}

@Composable
private fun RankedSongHighlight(rank: Int, song: TopSong, maxPlays: Int) {
    val scheme = MaterialTheme.colorScheme
    val container = when (rank) { 1 -> scheme.primaryContainer; 2 -> scheme.secondaryContainer; else -> scheme.tertiaryContainer }
    val ink = when (rank) { 1 -> scheme.onPrimaryContainer; 2 -> scheme.onSecondaryContainer; else -> scheme.onTertiaryContainer }
    val accent = when (rank) { 1 -> scheme.primary; 2 -> scheme.secondary; else -> scheme.tertiary }
    val cardShape = when (rank) {
        1 -> RoundedCornerShape(topStart = 30.dp, topEnd = 16.dp, bottomEnd = 30.dp, bottomStart = 16.dp)
        2 -> RoundedCornerShape(topStart = 16.dp, topEnd = 30.dp, bottomEnd = 16.dp, bottomStart = 30.dp)
        else -> RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp, bottomEnd = 14.dp, bottomStart = 26.dp)
    }
    val badgeShape = when (rank) {
        1 -> StatisticsFlowerShape
        2 -> RoundedCornerShape(14.dp)
        else -> CircleShape
    }
    Surface(modifier = Modifier.fillMaxWidth().testTag("stats_top_$rank"), shape = cardShape,
        color = container) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(Modifier.size(42.dp), shape = badgeShape,
                color = accent.copy(alpha = .20f), contentColor = ink) {
                Box(contentAlignment = Alignment.Center) {
                    Text(rank.toString(), style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold)
                }
            }
            SungArtwork(song.track.artworkUrl, Modifier.size(62.dp).clip(RoundedCornerShape(15.dp)),
                mask = ArtworkMask.SQUARE)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(song.track.title, style = MaterialTheme.typography.titleMedium, color = ink,
                    fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${song.plays} ${if (song.plays == 1) "play" else "plays"} · ${listeningTime(song.listenedMs)}",
                    style = MaterialTheme.typography.bodySmall, color = ink.copy(alpha = .82f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                RankProgress(song.plays.toFloat() / maxPlays.coerceAtLeast(1), accent)
            }
        }
    }
}

@Composable
private fun RankedSongRow(rank: Int, song: TopSong, maxPlays: Int) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(rank.toString(), Modifier.width(20.dp), style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        SungArtwork(song.track.artworkUrl, Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)),
            mask = ArtworkMask.SQUARE)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(song.track.title, style = MaterialTheme.typography.bodyMedium,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${song.plays} ${if (song.plays == 1) "play" else "plays"} · ${listeningTime(song.listenedMs)}",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            RankProgress(song.plays.toFloat() / maxPlays.coerceAtLeast(1), MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun RankProgress(progress: Float, color: Color) {
    Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(color.copy(alpha = .15f))) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight()
            .clip(CircleShape).background(color))
    }
}

@Composable
private fun RecentListeningSection(events: List<ListeningEvent>, all: Boolean, hasMore: Boolean,
    loadingMore: Boolean, toggle: () -> Unit, loadMore: () -> Unit) {
    val shown = if (all) events else events.take(5)
    val zone = ZoneId.systemDefault()
    val locale = LocalLocale.current.platformLocale
    val grouped = shown.groupBy { Instant.ofEpochMilli(it.lastPlayedAt).atZone(zone).toLocalDate() }
    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader("Recently played", if (events.size > 5) (if (all) "Show less" else "See all") else null, toggle)
            if (events.isEmpty()) Text("Your recent qualified plays will appear here.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            grouped.forEach { (day, dayEvents) ->
                Text("${day.format(DateTimeFormatter.ofPattern("d MMM yyyy", locale))} · ${dayEvents.size} ${if (dayEvents.size == 1) "play" else "plays"}",
                    style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                dayEvents.forEach { event ->
                    Row(Modifier.fillMaxWidth().padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SungArtwork(event.track.artworkUrl, Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)),
                            mask = ArtworkMask.SQUARE)
                        Column(Modifier.weight(1f)) {
                            Text(event.track.title, style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${timeLabel(event.lastPlayedAt, locale)} · ${listeningTime(event.listenedMs)} listened",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            if (hasMore && all) OutlinedButton(loadMore, enabled = !loadingMore,
                modifier = Modifier.fillMaxWidth().testTag("stats_load_more")) {
                Text(if (loadingMore) "Loading…" else "Load older listening")
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, action: String?, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold)
        if (action != null) TextButton(onAction) { Text(action) }
    }
}
