package com.karthi.sungmobile.core.data.settings

import android.content.Context
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.sungSettings by preferencesDataStore("sung_settings",
    produceMigrations = { context -> listOf(SharedPreferencesMigration(context, "appearance")) })

data class AppearanceSettings(val theme: String = "SYSTEM", val colors: String = "BLUE", val reduceMotion: Boolean = false)

/** One DataStore per application; imports the Stage 1 preferences on first read. */
class SettingsRepository(context: Context) {
    private val store = context.applicationContext.sungSettings
    private val theme = stringPreferencesKey("theme")
    private val colors = stringPreferencesKey("colors")
    private val motion = booleanPreferencesKey("reduce_motion")
    val appearance = store.data.map { AppearanceSettings(it[theme] ?: "SYSTEM", it[colors] ?: "BLUE", it[motion] ?: false) }

    suspend fun setAppearance(value: AppearanceSettings) {
        store.edit { it[theme] = value.theme; it[colors] = value.colors; it[motion] = value.reduceMotion }
    }
}
