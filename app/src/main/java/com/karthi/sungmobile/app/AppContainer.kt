package com.karthi.sungmobile.app

import android.content.Context
import com.karthi.sungmobile.core.data.queue.RoomQueueStore
import com.karthi.sungmobile.core.data.settings.SettingsRepository
import com.karthi.sungmobile.core.youtube.YouTubeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Application-lifetime dependencies. The playback service creates and releases its own engine. */
class AppContainer(context: Context) {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    var playbackShutdown: kotlinx.coroutines.Job? = null
        internal set
    val catalog by lazy { YouTubeSource() }
    val queueStore by lazy { RoomQueueStore(context) }
    val settings by lazy { SettingsRepository(context) }
    val appearance by lazy { AppearanceStore(settings, applicationScope) }
}
