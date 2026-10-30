package com.karthi.sungmobile.feature.convenience

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.model.SleepMode

@Composable
internal fun SleepTimerDialog(
    onTimer: (SleepMode, Int, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var mode by remember { mutableStateOf(SleepMode.DURATION) }
    var minutesInput by remember { mutableStateOf("30") }
    var fade by remember { mutableStateOf(false) }
    val minutes = minutesInput.toIntOrNull()?.takeIf { it in 1..240 }
    val canStart = mode != SleepMode.DURATION || minutes != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep timer", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 500.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    "Choose a countdown or let the current listening session finish.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("After a set time", style = MaterialTheme.typography.titleSmall)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        listOf(15, 30, 60).forEach { preset ->
                            FilterChip(
                                selected = mode == SleepMode.DURATION && minutes == preset,
                                onClick = {
                                    mode = SleepMode.DURATION
                                    minutesInput = preset.toString()
                                },
                                label = { Text("$preset min") },
                            )
                        }
                    }
                    OutlinedTextField(
                        value = minutesInput,
                        onValueChange = { value ->
                            minutesInput = value.filter(Char::isDigit).take(3)
                            mode = SleepMode.DURATION
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Custom duration") },
                        supportingText = { Text("Enter 1 to 240 minutes") },
                        trailingIcon = { Text("min", Modifier.padding(end = 8.dp)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = minutesInput.isNotEmpty() && minutes == null,
                    )
                }

                HorizontalDivider()

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Or stop at", style = MaterialTheme.typography.titleSmall)
                    TimerModeOption(
                        selected = mode == SleepMode.END_TRACK,
                        title = "End of this track",
                        description = "Stop when the current song finishes.",
                        onClick = { mode = SleepMode.END_TRACK },
                    )
                    TimerModeOption(
                        selected = mode == SleepMode.END_QUEUE,
                        title = "End of the queue",
                        description = "Let queued songs finish, then stop.",
                        onClick = { mode = SleepMode.END_QUEUE },
                    )
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Fade out", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Lower the volume over the final 10 seconds.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = fade,
                        onCheckedChange = { fade = it },
                        enabled = mode == SleepMode.DURATION,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = canStart,
                onClick = {
                    val durationMinutes = if (mode == SleepMode.DURATION) minutes ?: 0 else 0
                    onTimer(mode, durationMinutes, fade && mode == SleepMode.DURATION)
                    onDismiss()
                },
            ) {
                Text(if (mode == SleepMode.DURATION) "Start timer" else "Set timer")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun TimerModeOption(
    selected: Boolean,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        shape = shape,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceContainerLow,
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = null)
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
