package com.karthi.sungmobile.feature.convenience

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.core.model.ResumePoint
import com.karthi.sungmobile.core.model.lyrics.lyricsVideoId
import com.karthi.sungmobile.core.playback.PlaybackState
import kotlin.math.abs

@Composable
fun PlayerResumeControls(state: PlaybackState, resume: ResumePoint?,
    onResume: () -> Unit, onStartOver: () -> Unit) {
    if (resume == null || state.track?.lyricsVideoId() != resume.videoId ||
        abs(state.engine.positionMs - resume.positionMs) <= 5_000) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Saved position: ${formatResumeTime(resume.positionMs)}",
            style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onResume, enabled = state.engine.seekable && !state.resolving) { Text("Resume") }
            TextButton(onStartOver) { Text("Start over") }
        }
    }
}

private fun formatResumeTime(ms: Long): String {
    val total = ms / 1_000
    return "%d:%02d".format(total / 60, total % 60)
}
