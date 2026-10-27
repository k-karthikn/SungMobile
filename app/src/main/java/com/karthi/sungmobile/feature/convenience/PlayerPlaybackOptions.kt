package com.karthi.sungmobile.feature.convenience

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
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
    if (timerDialog) TimerDialog(onTimer) { timerDialog = false }
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

@Composable
private fun TimerDialog(onTimer: (SleepMode, Int, Boolean) -> Unit, onDismiss: () -> Unit) {
    var custom by remember { mutableStateOf("30") }
    var fade by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Sleep timer") },
        text = { Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(15, 30, 60).forEach { minutes ->
                TextButton({ onTimer(SleepMode.DURATION, minutes, fade); onDismiss() }) { Text("$minutes minutes") }
            }
            OutlinedTextField(custom, { custom = it.filter(Char::isDigit).take(3) },
                label = { Text("Custom minutes (1–240)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            TextButton({ custom.toIntOrNull()?.takeIf { it in 1..240 }?.let {
                onTimer(SleepMode.DURATION, it, fade); onDismiss()
            } }, enabled = custom.toIntOrNull()?.let { it in 1..240 } == true) { Text("Start custom timer") }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Fade for final 10 seconds", Modifier.weight(1f))
                Switch(fade, { fade = it })
            }
            HorizontalDivider()
            TextButton({ onTimer(SleepMode.END_TRACK, 0, false); onDismiss() }) { Text("End of this track") }
            TextButton({ onTimer(SleepMode.END_QUEUE, 0, false); onDismiss() }) { Text("End of queue") }
        } }, confirmButton = { TextButton(onDismiss) { Text("Close") } })
}

internal fun formatRemainingTime(remainingMs: Long): String {
    val seconds = (remainingMs.coerceAtLeast(0) + 999) / 1_000
    return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}
