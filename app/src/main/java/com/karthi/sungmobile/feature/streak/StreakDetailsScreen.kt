package com.karthi.sungmobile.feature.streak

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.designsystem.component.SungLoading
import com.karthi.sungmobile.core.designsystem.icon.SungIcon
import com.karthi.sungmobile.core.designsystem.icon.SungSymbol
import com.karthi.sungmobile.core.model.library.StreakProgress
import java.time.LocalDate

@Composable
internal fun StreakDetailsScreen(state: StreakUiState, onBack: () -> Unit, onRetry: () -> Unit,
    modifier: Modifier = Modifier, today: LocalDate = LocalDate.now()) {
    LazyColumn(modifier.fillMaxSize().testTag("streak_details"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item("heading") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalIconButton(onBack, shapes = IconButtonDefaults.shapes(), modifier = Modifier.size(48.dp)) {
                    SungIcon(SungSymbol.Back, "Back to Home")
                }
                Text("Listening streak", style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold)
            }
        }
        when {
            state.loading -> item("loading") { SungLoading(label = "Loading your streak…") }
            state.error -> item("error") { StreakError(onRetry) }
            state.progress.privateMode -> item("private") { PrivateStreakNotice() }
            else -> {
                item("hero") { StreakHero(state.progress) }
                item("metrics") { StreakMetrics(state.progress) }
                item("calendar") { StreakCalendar(state.progress, Modifier.testTag("streak_calendar"), today) }
            }
        }
    }
}

@Composable
private fun StreakHero(progress: StreakProgress) {
    val palette = emberPalette(progress.currentDays)
    Surface(modifier = Modifier.fillMaxWidth().testTag("streak_hero"), shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.size(92.dp).background(palette.outer.copy(alpha = .22f), CircleShape),
                    contentAlignment = Alignment.Center) {
                    CompositionLocalProvider(LocalContentColor provides palette.body) {
                        SungIcon(SungSymbol.Fire, null, Modifier.size(58.dp))
                    }
                    Box(Modifier.size(15.dp).offset(y = 13.dp).background(palette.core, CircleShape))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (progress.currentDays == 0) "Start your streak" else "You are on fire",
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("${progress.currentDays}", Modifier.testTag("streak_current_count"),
                        style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = palette.body)
                    Text(if (progress.currentDays == 1) "day in a row" else "days in a row",
                        style = MaterialTheme.typography.titleMedium)
                }
            }
            Text(if (progress.currentDays == 0) "A qualified play today lights your first ember."
                else "Play one qualified song today to keep this ember lit.",
                Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StreakMetrics(progress: StreakProgress) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StreakMetric(progress.currentDays.toString(), "Current", emberColor(progress.currentDays), Modifier.weight(1f))
        StreakMetric(progress.longestDays.toString(), "Best", MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
        StreakMetric(progress.activeDays.size.toString(), "Active days", MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
    }
}

@Composable
private fun StreakMetric(value: String, label: String, color: Color, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun PrivateStreakNotice() {
    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SungIcon(SungSymbol.Fire, null, Modifier.size(36.dp))
            Text("Your streak is private", style = MaterialTheme.typography.titleLarge)
            Text("Turn off Private mode in History & privacy to view and grow your listening streak.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StreakError(onRetry: () -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.errorContainer) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Could not load your streak.", Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer)
            TextButton(onRetry) { Text("Retry") }
        }
    }
}
