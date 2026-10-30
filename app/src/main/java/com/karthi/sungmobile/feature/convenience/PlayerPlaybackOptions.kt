package com.karthi.sungmobile.feature.convenience

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.model.PlaybackSpeed
import com.karthi.sungmobile.core.model.SleepMode
import com.karthi.sungmobile.core.playback.PlaybackState

@Composable
fun PlayerPlaybackOptions(
    state: PlaybackState,
    onSpeed: (PlaybackSpeed) -> Unit,
    onTimer: (SleepMode, Int, Boolean) -> Unit,
    onCancelTimer: () -> Unit,
) {
    var speedDialog by remember { mutableStateOf(false) }
    var timerDialog by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton({ speedDialog = true }, Modifier.weight(1f)) {
                Text("Speed ${state.speed.multiplier}×")
            }
            FilledTonalButton({ timerDialog = true }, Modifier.weight(1f)) {
                Text(if (state.sleepTimer == null) "Sleep timer" else "Timer active")
            }
        }
        state.sleepTimer?.let { timer ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val label = when (timer.mode) {
                    SleepMode.DURATION -> "Pauses in ${formatRemainingTime(timer.remainingMs)}"
                    SleepMode.END_TRACK -> "Pauses after this track"
                    SleepMode.END_QUEUE -> "Pauses after the queue"
                }
                Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                TextButton(onCancelTimer) { Text("Cancel") }
            }
        }
    }
    if (speedDialog) SpeedDialog(state.speed, onSpeed) { speedDialog = false }
    if (timerDialog) SleepTimerDialog(onTimer) { timerDialog = false }
}

@Composable
private fun SpeedDialog(current: PlaybackSpeed, onSpeed: (PlaybackSpeed) -> Unit, onDismiss: () -> Unit) {
    var tenths by remember(current) { mutableIntStateOf(current.tenths) }
    var pitch by remember(current) { mutableStateOf(current.preservePitch) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Playback speed") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("${tenths / 10f}×")
            Slider(tenths.toFloat(), { tenths = it.toInt().coerceIn(5, 20) }, valueRange = 5f..20f, steps = 14)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Preserve pitch", Modifier.weight(1f))
                Switch(pitch, { pitch = it })
            }
            TextButton({ tenths = 10; pitch = true }) { Text("Reset to 1.0×") }
        } },
        confirmButton = { TextButton({ onSpeed(PlaybackSpeed(tenths, pitch)); onDismiss() }) { Text("Apply") } },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } })
}

internal fun formatRemainingTime(remainingMs: Long): String {
    val seconds = (remainingMs.coerceAtLeast(0) + 999) / 1_000
    return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}
