package com.karthi.sungmobile.app

import com.karthi.sungmobile.core.data.settings.AppearanceSettings
import com.karthi.sungmobile.core.data.settings.SettingsRepository
import com.karthi.sungmobile.core.designsystem.theme.ColorMode
import com.karthi.sungmobile.core.designsystem.theme.ThemeMode
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

data class Appearance(val theme: ThemeMode = ThemeMode.SYSTEM, val colors: ColorMode = ColorMode.BLUE,
    val reduceMotion: Boolean = false)

/** Maps persisted settings to UI types; writes remain ordered even during rapid selections. */
class AppearanceStore(private val repository: SettingsRepository, scope: CoroutineScope) {
    private val mutableState = MutableStateFlow(Appearance())
    val state = mutableState.asStateFlow()
    private val mutableError = MutableStateFlow(false)
    val error = mutableError.asStateFlow()
    private val changes = Channel<Appearance>(Channel.CONFLATED)
    init {
        scope.launch {
            repository.appearance.catch { mutableError.value = true }.collect { saved ->
                mutableState.value = Appearance(ThemeMode.entries.find { it.name == saved.theme } ?: ThemeMode.SYSTEM,
                    ColorMode.entries.find { it.name == saved.colors } ?: ColorMode.BLUE, saved.reduceMotion)
            }
        }
        scope.launch {
            for (value in changes) {
                try {
                    repository.setAppearance(AppearanceSettings(value.theme.name, value.colors.name, value.reduceMotion))
                    mutableState.value = value
                    mutableError.value = false
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { mutableError.value = true }
            }
        }
    }
    fun update(value: Appearance) { changes.trySend(value) }
}
