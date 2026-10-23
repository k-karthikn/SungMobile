package com.karthi.sungmobile.feature.updates

import android.Manifest
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import kotlinx.coroutines.launch

@Composable
fun UpdateSettingsSection() {
    val context = LocalContext.current
    val updater = remember { AppUpdates.get(context) }
    val state by updater.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    SungSettingsGroup("App updates") {
        val installed = remember { com.karthi.sungmobile.updates.UpdateApk.installed(context) }
        Text("Installed: ${installed.versionName.orEmpty()} (code ${com.karthi.sungmobile.updates.UpdateApk.code(installed)})", style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth().toggleable(state.enabled, role = Role.Switch,
            onValueChange = updater::setEnabled), verticalAlignment = Alignment.CenterVertically) {
            Text("Automatic updates", Modifier.weight(1f))
            Switch(state.enabled, onCheckedChange = null)
        }
        Text("Automatically check and download stable GitHub releases on unmetered networks, usually Wi-Fi. Android asks you to confirm installation. Checks run about every 12 hours when the system allows.", style = MaterialTheme.typography.bodyMedium)
        Text(state.message, style = MaterialTheme.typography.bodySmall)
        FilledTonalButton({ scope.launch { updater.check(automatic = false) } }, Modifier.fillMaxWidth(), enabled = !state.busy) {
            Text(if (state.busy) "Checking / downloading…" else "Check and download now")
        }
        Text("Manual checks may use mobile data. Turning automatic updates off stops scheduled checks and downloads.", style = MaterialTheme.typography.bodySmall)
        if (state.ready) Button({ scope.launch { updater.install(context) } }, Modifier.fillMaxWidth(), enabled = !state.busy) { Text("Install update") }
        if (Build.VERSION.SDK_INT >= 33) TextButton({ permission.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
            Text("Enable update notifications")
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
