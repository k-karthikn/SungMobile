package com.karthi.sungmobile

import android.app.Application
import com.karthi.sungmobile.core.designsystem.artwork.configureSungImages
import com.karthi.sungmobile.feature.feedback.DiagnosticLog

class SungApplication : Application() {
    val container by lazy { com.karthi.sungmobile.app.AppContainer(this) }
    override fun onCreate() {
        super.onCreate()
        DiagnosticLog.install(this)
        configureSungImages()
        com.karthi.sungmobile.updates.AppUpdates.get(this).start()
    }
}
