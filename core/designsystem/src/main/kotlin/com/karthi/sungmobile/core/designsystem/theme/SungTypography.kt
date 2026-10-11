package com.karthi.sungmobile.core.designsystem.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.karthi.sungmobile.core.designsystem.R

// Android's fallback chain supplies glyphs missing from the bundled Latin font.
@OptIn(ExperimentalTextApi::class)
private val GoogleSansFlex = FontFamily((100..900 step 100).map { weight ->
    Font(R.font.google_sans_flex, FontWeight(weight),
        variationSettings = FontVariation.Settings(
            FontVariation.weight(weight), FontVariation.Setting("ROND", 30f)))
})

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal val SungTypography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = GoogleSansFlex),
        displayMedium = base.displayMedium.copy(fontFamily = GoogleSansFlex),
        displaySmall = base.displaySmall.copy(fontFamily = GoogleSansFlex),
        headlineLarge = base.headlineLarge.copy(fontFamily = GoogleSansFlex),
        headlineMedium = base.headlineMedium.copy(fontFamily = GoogleSansFlex),
        headlineSmall = base.headlineSmall.copy(fontFamily = GoogleSansFlex),
        titleLarge = base.titleLarge.copy(fontFamily = GoogleSansFlex),
        titleMedium = base.titleMedium.copy(fontFamily = GoogleSansFlex),
        titleSmall = base.titleSmall.copy(fontFamily = GoogleSansFlex),
        bodyLarge = base.bodyLarge.copy(fontFamily = GoogleSansFlex),
        bodyMedium = base.bodyMedium.copy(fontFamily = GoogleSansFlex),
        bodySmall = base.bodySmall.copy(fontFamily = GoogleSansFlex),
        labelLarge = base.labelLarge.copy(fontFamily = GoogleSansFlex),
        labelMedium = base.labelMedium.copy(fontFamily = GoogleSansFlex),
        labelSmall = base.labelSmall.copy(fontFamily = GoogleSansFlex),
        displayLargeEmphasized = base.displayLargeEmphasized.copy(fontFamily = GoogleSansFlex),
        displayMediumEmphasized = base.displayMediumEmphasized.copy(fontFamily = GoogleSansFlex),
        displaySmallEmphasized = base.displaySmallEmphasized.copy(fontFamily = GoogleSansFlex),
        headlineLargeEmphasized = base.headlineLargeEmphasized.copy(fontFamily = GoogleSansFlex),
        headlineMediumEmphasized = base.headlineMediumEmphasized.copy(fontFamily = GoogleSansFlex),
        headlineSmallEmphasized = base.headlineSmallEmphasized.copy(fontFamily = GoogleSansFlex),
        titleLargeEmphasized = base.titleLargeEmphasized.copy(fontFamily = GoogleSansFlex),
        titleMediumEmphasized = base.titleMediumEmphasized.copy(fontFamily = GoogleSansFlex),
        titleSmallEmphasized = base.titleSmallEmphasized.copy(fontFamily = GoogleSansFlex),
        bodyLargeEmphasized = base.bodyLargeEmphasized.copy(fontFamily = GoogleSansFlex),
        bodyMediumEmphasized = base.bodyMediumEmphasized.copy(fontFamily = GoogleSansFlex),
        bodySmallEmphasized = base.bodySmallEmphasized.copy(fontFamily = GoogleSansFlex),
        labelLargeEmphasized = base.labelLargeEmphasized.copy(fontFamily = GoogleSansFlex),
        labelMediumEmphasized = base.labelMediumEmphasized.copy(fontFamily = GoogleSansFlex),
        labelSmallEmphasized = base.labelSmallEmphasized.copy(fontFamily = GoogleSansFlex),
    )
}
