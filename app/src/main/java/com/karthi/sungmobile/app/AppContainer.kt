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
    val database by lazy { com.karthi.sungmobile.core.data.db.DatabaseProvider.open(context) }
    val queueStore by lazy { RoomQueueStore(database) }
    val library by lazy { com.karthi.sungmobile.core.data.library.RoomLibraryRepository(database) }
    val history by lazy { com.karthi.sungmobile.core.data.history.RoomHistoryRepository(database) }
    val home by lazy { com.karthi.sungmobile.core.data.discovery.RoomHomeRepository(database) }
    val settings by lazy { SettingsRepository(context) }
    val appearance by lazy { AppearanceStore(settings, applicationScope) }
}
