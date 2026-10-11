package com.karthi.sungmobile

import android.content.Context
import android.provider.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.karthi.sungmobile.core.designsystem.theme.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppearanceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun systemDisabledAnimationsAlsoDisableExpressiveMotion() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        var motion = true
        compose.setContent {
            SungTheme {
                val enabled = LocalSungMotionEnabled.current
                SideEffect { motion = enabled }
            }
        }
        compose.runOnIdle { assertFalse(motion) }
    }

    @Test fun blueLightRolesMeetNormalTextContrast() = contrast(ThemeMode.LIGHT)
    @Test fun blueDarkRolesMeetNormalTextContrast() = contrast(ThemeMode.DARK)

    private fun contrast(mode: ThemeMode) {
        var pairs = emptyList<Pair<Color, Color>>()
        compose.setContent {
            SungTheme(mode) {
                val colors = MaterialTheme.colorScheme
                SideEffect {
                    pairs = with(colors) { listOf(onPrimary to primary, onPrimaryContainer to primaryContainer,
                        onSecondaryContainer to secondaryContainer, onSurface to surface,
                        onSurfaceVariant to surfaceContainerHigh, onErrorContainer to errorContainer) }
                }
            }
        }
        compose.runOnIdle {
            assertEquals(6, pairs.size)
            pairs.forEach { (foreground, background) ->
                val first = foreground.luminance()
                val second = background.luminance()
                val ratio = (maxOf(first, second) + .05f) / (minOf(first, second) + .05f)
                assertTrue("Contrast $ratio is below 4.5:1", ratio >= 4.5f)
            }
        }
    }
}
