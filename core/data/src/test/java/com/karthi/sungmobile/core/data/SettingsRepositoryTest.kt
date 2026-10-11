package com.karthi.sungmobile.core.data

import android.content.Context
import com.karthi.sungmobile.core.data.settings.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SettingsRepositoryTest {
    @Test fun importsStageOnePreferencesThenPersistsNewChoices() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("appearance", Context.MODE_PRIVATE).edit()
            .putString("theme", "DARK").putString("colors", "WALLPAPER").putBoolean("reduce_motion", true).commit()
        val repository = SettingsRepository(context)
        assertEquals(AppearanceSettings("DARK", "WALLPAPER", true), repository.appearance.first())
        val updated = AppearanceSettings("LIGHT", "BLUE", false)
        repository.setAppearance(updated)
        assertEquals(updated, SettingsRepository(context).appearance.first())
        assertFalse(context.getSharedPreferences("appearance", Context.MODE_PRIVATE).contains("theme"))
    }
}
