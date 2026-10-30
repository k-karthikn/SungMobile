package com.karthi.sungmobile

import android.app.Application
import com.karthi.sungmobile.core.designsystem.artwork.configureSungImages
import com.karthi.sungmobile.feature.feedback.DiagnosticLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class SungApplication : Application() {
    val container by lazy { com.karthi.sungmobile.app.AppContainer(this) }
    override fun onCreate() {
        super.onCreate()
        DiagnosticLog.install(this)
        configureSungImages()
        com.karthi.sungmobile.updates.AppUpdates.get(this).start()
        container.applicationScope.launch {
            try {
                container.backup.recoverPendingRestore()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                DiagnosticLog.recordError("backup_restore_recovery", error)
            }
        }
    }
}
