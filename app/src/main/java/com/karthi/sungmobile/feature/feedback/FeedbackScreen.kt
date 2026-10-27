package com.karthi.sungmobile.feature.feedback

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.ActivityNotFoundException
import android.content.ComponentName
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
                val attachment = if (attachLog) runCatching {
                    withContext(Dispatchers.IO) { DiagnosticLog.export(context) }
                }.getOrElse {
                    error = "Could not prepare the diagnostic log. You can send feedback without it."
                    sending = false
                    return@launch
                } else null
                val chooser = runCatching { feedbackChooser(context, feedback.trim(), attachment) }
                    .getOrElse {
                        error = "Could not attach the diagnostic log. You can send feedback without it."
                        sending = false
                        return@launch
                    }
                try {
                    context.startActivity(chooser)
                } catch (_: ActivityNotFoundException) {
                    error = "No email app could open this message. Please email $SUPPORT_EMAIL."
                } catch (_: SecurityException) {
                    error = "The email app could not access the log. Try sending without it."
                } catch (_: Exception) {
                    error = "Could not open the email app. Please email $SUPPORT_EMAIL."
                } finally { sending = false }
            }
        }, enabled = feedback.isNotBlank() && !sending, modifier = Modifier.fillMaxWidth()) {
            Text(if (sending) "Preparing…" else "Open email app")
        }
    }
}

internal fun feedbackIntent(context: Context, message: String, attachment: java.io.File?): Intent =
    Intent(Intent.ACTION_SEND).apply {
        type = if (attachment == null) "message/rfc822" else "text/plain"
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

internal fun feedbackChooser(context: Context, message: String, attachment: java.io.File?): Intent {
    val send = feedbackIntent(context, message, attachment)
    return Intent.createChooser(send, "Send feedback").apply {
        putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS,
            arrayOf(ComponentName(context.packageName, "${context.packageName}.MainActivity")))
        if (attachment != null) {
            clipData = send.clipData
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
