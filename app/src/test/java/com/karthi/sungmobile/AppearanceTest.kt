package com.karthi.sungmobile

import android.content.Context
import android.provider.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
    @Test fun darkBlueSurfaceLadderKeepsContentAndControlsDistinct() = surfaceLadder(ThemeMode.DARK)
    @Test fun lightBlueSurfaceLadderKeepsContentAndControlsDistinct() = surfaceLadder(ThemeMode.LIGHT)

    @Test fun customAccentAndSchemeChangeTheRenderedPalette() {
        var seed by mutableStateOf(0xFF006C91.toInt())
        var style by mutableStateOf(ColorSchemeStyle.BALANCED)
        var primary = Color.Unspecified
        compose.setContent {
            SungTheme(ThemeMode.LIGHT, ColorMode.ACCENT, colorSchemeStyle = style, accentColor = seed) {
                val current = MaterialTheme.colorScheme.primary
                SideEffect { primary = current }
            }
        }
        var blue = Color.Unspecified
        compose.runOnIdle { blue = primary; seed = 0xFF985440.toInt() }
        var terracotta = Color.Unspecified
        compose.runOnIdle { terracotta = primary; style = ColorSchemeStyle.EXPRESSIVE }
        compose.runOnIdle {
            assertNotEquals(blue, terracotta)
            assertNotEquals(terracotta, primary)
        }
    }

    private fun contrast(mode: ThemeMode) {
        var pairs = emptyList<Pair<Color, Color>>()
        compose.setContent {
            SungTheme(mode) {
                val colors = MaterialTheme.colorScheme
                SideEffect {
                    pairs = with(colors) { listOf(onPrimary to primary, onPrimaryContainer to primaryContainer,
                        onSecondary to secondary, onSecondaryContainer to secondaryContainer,
                        onTertiary to tertiary, onTertiaryContainer to tertiaryContainer, onSurface to surface,
                        onSurfaceVariant to surfaceContainerHigh, onErrorContainer to errorContainer) }
                }
            }
        }
        compose.runOnIdle {
            assertEquals(9, pairs.size)
            pairs.forEach { (foreground, background) ->
                val first = foreground.luminance()
                val second = background.luminance()
                val ratio = (maxOf(first, second) + .05f) / (minOf(first, second) + .05f)
                assertTrue("Contrast $ratio is below 4.5:1", ratio >= 4.5f)
            }
        }
    }

    private fun surfaceLadder(mode: ThemeMode) {
        var levels = emptyList<Float>()
        compose.setContent {
            SungTheme(mode) {
                val colors = MaterialTheme.colorScheme
                SideEffect { levels = with(colors) { listOf(surface, surfaceContainerLow, surfaceContainer,
                    surfaceContainerHigh, surfaceContainerHighest).map(Color::luminance) } }
            }
        }
        compose.runOnIdle {
            assertEquals(5, levels.size)
            val ordered = if (mode == ThemeMode.DARK) levels.zipWithNext().all { (a, b) -> a < b }
                else levels.zipWithNext().all { (a, b) -> a > b }
            assertTrue("Surface roles should form a visible hierarchy", ordered)
        }
    }
}
