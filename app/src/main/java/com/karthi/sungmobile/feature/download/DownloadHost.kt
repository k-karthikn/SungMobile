package com.karthi.sungmobile.feature.download

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.karthi.sungmobile.core.model.CatalogItem

data class DownloadUi(val start: (CatalogItem) -> Unit = {})
val LocalDownloadUi = compositionLocalOf { DownloadUi() }

@Composable
fun DownloadHost(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<CatalogItem?>(null) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val track = pending
        pending = null
        if (granted && track != null) Toast.makeText(context, DownloadRequests.begin(context, track), Toast.LENGTH_LONG).show()
        else if (!granted) Toast.makeText(context,
            "Allow notifications to start and monitor downloads.", Toast.LENGTH_LONG).show()
    }
    val ui = remember(context, notificationPermission) { DownloadUi { track ->
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            pending = track
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else Toast.makeText(context, DownloadRequests.begin(context, track), Toast.LENGTH_LONG).show()
    } }
    androidx.compose.runtime.CompositionLocalProvider(LocalDownloadUi provides ui, content = content)
}
