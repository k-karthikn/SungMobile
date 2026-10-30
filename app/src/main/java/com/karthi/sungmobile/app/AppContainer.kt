package com.karthi.sungmobile.app

import android.content.Context
import com.karthi.sungmobile.core.data.backup.BackupRepository
import com.karthi.sungmobile.core.data.convenience.ConvenienceRepository
import com.karthi.sungmobile.core.data.db.DatabaseProvider
import com.karthi.sungmobile.core.data.discovery.RoomHomeRepository
import com.karthi.sungmobile.core.data.history.RoomHistoryRepository
import com.karthi.sungmobile.core.data.library.RoomAdvancedLibraryRepository
import com.karthi.sungmobile.core.data.library.RoomLibraryRepository
import com.karthi.sungmobile.core.data.local.LocalAudioRepository
import com.karthi.sungmobile.core.data.lyrics.LyricsCache
import com.karthi.sungmobile.core.data.lyrics.LocalLyricsReader
import com.karthi.sungmobile.core.data.queue.RoomQueueStore
import com.karthi.sungmobile.core.data.recommendation.RoomRecommendationProfileSource
import com.karthi.sungmobile.core.data.settings.SettingsRepository
import com.karthi.sungmobile.core.youtube.YouTubeSource
import com.karthi.sungmobile.core.youtube.lyrics.YouTubeLyricsGateway
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob

/** Application-lifetime dependencies. The playback service creates and releases its own engine. */
class AppContainer(context: Context) {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    var playbackShutdown: Job? = null
        internal set
    val catalog by lazy { YouTubeSource() }
    val database by lazy { DatabaseProvider.open(context) }
    val queueStore by lazy { RoomQueueStore(database) }
    val library by lazy { RoomLibraryRepository(database) }
    val advancedLibrary by lazy { RoomAdvancedLibraryRepository(database) }
    val history by lazy { RoomHistoryRepository(database) }
    val home by lazy { RoomHomeRepository(database) }
    val recommendationProfile by lazy { RoomRecommendationProfileSource(database) }
    val settings by lazy { SettingsRepository(context) }
    val backup by lazy { BackupRepository(database, settings) }
    val appearance by lazy { AppearanceStore(settings, applicationScope) }
    val lyricsGateway by lazy { YouTubeLyricsGateway() }
    val lyricsCache by lazy { LyricsCache(context) }
    val localLyrics by lazy {
        LocalLyricsReader(context.applicationContext) { localAudio.addedFolders() }
    }
    val convenience by lazy { ConvenienceRepository(context) }
    val localAudio by lazy { LocalAudioRepository(context.applicationContext) }
}
