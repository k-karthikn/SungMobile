package com.karthi.sungmobile

import android.app.Application
import com.karthi.sungmobile.core.designsystem.artwork.configureSungImages

class SungApplication : Application() {
    val container by lazy { com.karthi.sungmobile.app.AppContainer(this) }
    override fun onCreate() {
        super.onCreate()
        configureSungImages()
    }
}
