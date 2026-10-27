package com.karthi.sungmobile.feature.home

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.karthi.sungmobile.core.designsystem.component.SungSettingsGroup
import com.karthi.sungmobile.core.designsystem.component.SungSettingsToggle
import com.karthi.sungmobile.core.model.discovery.RecommendationPreferences

@Composable
fun RecommendationSettingsSection(value: RecommendationPreferences, onEnabled: (Boolean) -> Unit,
    clearFeedback: () -> Unit) {
    SungSettingsGroup("Home recommendations") {
        SungSettingsToggle("Personalized Home", value.enabled, onEnabled,
            "Related lookup uses liked or chosen song IDs. Ranking stays on your phone.")
        if (value.feedback.isNotEmpty()) {
            TextButton(clearFeedback, Modifier.fillMaxWidth()) { Text("Clear recommendation choices") }
        }
    }
}
