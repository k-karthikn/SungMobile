package com.karthi.sungmobile.feature.downloads

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karthi.sungmobile.core.data.device.DownloadRepository
import com.karthi.sungmobile.core.data.device.DeviceMusicRepository
import com.karthi.sungmobile.core.data.settings.SettingsRepository
import com.karthi.sungmobile.core.model.CatalogItem
import com.karthi.sungmobile.core.model.CatalogKind
import com.karthi.sungmobile.core.model.YouTubeLink
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DownloadViewModel(private val context: Context, private val repository: DownloadRepository,
    private val settings: SettingsRepository, private val deviceMusic: DeviceMusicRepository) : ViewModel() {
    val state = repository.state
    init { viewModelScope.launch {
        repository.refresh()
        state.value.items.filter { it.state in setOf("QUEUED", "RUNNING", "PAUSED") }
            .forEach { DownloadScheduler.enqueue(context, it.videoId) }
    } }

    fun enqueue(track: CatalogItem) { viewModelScope.launch {
        runCatching { repository.queue(track, settings.deviceAudio.first().downloadTreeUri) }
            .onSuccess { DownloadScheduler.enqueue(context, it) }
    } }

    fun enqueueAll(tracks: List<CatalogItem>) { viewModelScope.launch {
        val destination = settings.deviceAudio.first().downloadTreeUri
        tracks.filter { it.kind == CatalogKind.SONG }.distinctBy { it.url }.take(500).forEach { track ->
            runCatching { repository.queue(track, destination) }.onSuccess { DownloadScheduler.enqueue(context, it) }
        }
    } }

    fun cancel(id: String) { viewModelScope.launch {
        DownloadScheduler.cancel(context, id)
        repository.update(id, "CANCELED", error = "Canceled")
        repository.row(id)?.uri?.let { uri ->
            runCatching { context.contentResolver.delete(android.net.Uri.parse(uri), null, null) }
        }
    } }

    fun retry(id: String) { viewModelScope.launch {
        val row = repository.row(id) ?: return@launch
        val url = YouTubeLink.Song(id).url
        repository.queue(CatalogItem(url, CatalogKind.SONG, row.title, row.artist, url),
            settings.deviceAudio.first().downloadTreeUri)
        DownloadScheduler.enqueue(context, id)
    } }

    fun remove(id: String) { viewModelScope.launch {
        DownloadScheduler.cancel(context, id)
        repository.remove(id)
        deviceMusic.scan()
    } }
}
