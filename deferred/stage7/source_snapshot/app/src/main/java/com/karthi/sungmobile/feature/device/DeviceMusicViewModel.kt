package com.karthi.sungmobile.feature.device

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karthi.sungmobile.core.data.device.DeviceMusicRepository
import com.karthi.sungmobile.core.data.settings.DeviceAudioSettings
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DeviceMusicViewModel(private val context: Context, private val repository: DeviceMusicRepository) : ViewModel() {
    val state = repository.state
    private var changed: Job? = null
    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) { autoScan() }
    }

    init {
        context.contentResolver.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, observer)
        viewModelScope.launch { repository.refresh(); autoScan() }
    }

    fun scan() { viewModelScope.launch { repository.scan() } }
    fun autoScan() {
        if (!state.value.settings.autoScan) return
        changed?.cancel()
        changed = viewModelScope.launch { delay(700); repository.scan() }
    }
    fun addFolder(uri: Uri, exclude: Boolean) { viewModelScope.launch {
        repository.addFolder(uri, uri.lastPathSegment?.substringAfterLast(':') ?: "Selected folder", exclude)
        repository.scan()
    } }
    fun removeFolder(uri: String) { viewModelScope.launch { repository.removeFolder(uri); repository.scan() } }
    fun update(transform: (DeviceAudioSettings) -> DeviceAudioSettings) { viewModelScope.launch {
        val before = state.value.settings
        val after = transform(before)
        repository.setSettings(after)
        if (before.scanMusic != after.scanMusic) repository.scan()
    } }
    fun refresh() { viewModelScope.launch { repository.refresh() } }
    fun hasPermission() = repository.hasAudioPermission()

    override fun onCleared() {
        context.contentResolver.unregisterContentObserver(observer)
    }
}
