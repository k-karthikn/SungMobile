package com.karthi.sungmobile.feature.feedback

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.karthi.sungmobile.core.designsystem.icon.SungIcon
import com.karthi.sungmobile.core.designsystem.icon.SungSymbol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val SUPPORT_EMAIL = "sungmobile.support@gmail.com"

@Composable
fun FeedbackScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var feedback by rememberSaveable { mutableStateOf("") }
    var attachLog by rememberSaveable { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack) { SungIcon(SungSymbol.Back, "Back") }
            Text("Feedback & report", style = MaterialTheme.typography.headlineSmall)
        }
        Text("Tell us what happened or what could be better.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(feedback, { feedback = it.take(4_000) }, Modifier.fillMaxWidth().heightIn(min = 180.dp),
            label = { Text("Your message") }, minLines = 5, maxLines = 12)
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Checkbox(attachLog, { attachLog = it })
                    Text("Attach diagnostic log", style = MaterialTheme.typography.titleMedium)
                }
                Text("Optional. Contains app error types, crash code locations, app version and Android version. It does not include logcat, song titles or listening history.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = {
            scope.launch {
                sending = true
                error = null
                try {
                    val attachment = if (attachLog) withContext(Dispatchers.IO) { DiagnosticLog.export(context) } else null
                    context.startActivity(Intent.createChooser(feedbackIntent(context, feedback.trim(), attachment),
                        "Send feedback"))
                } catch (_: Exception) {
                    error = "No app could open this message. Please email $SUPPORT_EMAIL."
                } finally { sending = false }
            }
        }, enabled = feedback.isNotBlank() && !sending, modifier = Modifier.fillMaxWidth()) {
            Text(if (sending) "Preparing…" else "Open email app")
        }
    }
}

internal fun feedbackIntent(context: Context, message: String, attachment: java.io.File?): Intent =
    Intent(Intent.ACTION_SEND).apply {
        type = "message/rfc822"
        putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
        putExtra(Intent.EXTRA_SUBJECT, "SungMobile feedback")
        putExtra(Intent.EXTRA_TEXT, message)
        attachment?.let { file ->
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.feedback", file)
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(context.contentResolver, "SungMobile diagnostic log", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
