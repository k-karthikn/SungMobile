package com.karthi.sungmobile.feature.updates

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.karthi.sungmobile.updates.AppUpdates
import com.karthi.sungmobile.core.designsystem.component.SungSettingsGroup
import com.karthi.sungmobile.core.designsystem.component.SungSettingsToggle
import kotlinx.coroutines.launch

@Composable
fun UpdateSettingsSection() {
    val context = LocalContext.current
    val updater = remember { AppUpdates.get(context) }
    val state by updater.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var notificationsAllowed by remember {
        mutableStateOf(Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationsAllowed = it
    }
    SungSettingsGroup("App updates") {
        val installed = remember { com.karthi.sungmobile.updates.UpdateApk.installed(context) }
        Text("Version ${installed.versionName.orEmpty()}", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        SungSettingsToggle("Automatic updates", state.enabled, updater::setEnabled,
            "Download on Wi-Fi")
        if (state.message.isNotBlank() && state.message != "Updates have not been checked yet.") {
            Text(state.message, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FilledTonalButton({ scope.launch { updater.check(automatic = false) } }, Modifier.fillMaxWidth(), enabled = !state.busy) {
            Text(if (state.busy) "Checking…" else "Check for updates")
        }
        if (state.ready) Button({ scope.launch { updater.install(context) } }, Modifier.fillMaxWidth(), enabled = !state.busy) { Text("Install update") }
        if (Build.VERSION.SDK_INT >= 33 && !notificationsAllowed) TextButton({ permission.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
            Text("Enable notifications")
        }
    }
}

@Composable
fun UpdateReadyBanner(openSettings: () -> Unit) {
    val context = LocalContext.current
    val updater = remember { AppUpdates.get(context) }
    val state by updater.state.collectAsStateWithLifecycle()
    if (state.ready) Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("An app update is ready", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            TextButton(openSettings) { Text("Review") }
        }
    }
}
