package com.karthi.sungmobile.core.designsystem.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.karthi.sungmobile.core.designsystem.theme.LocalSungMotionEnabled
import com.karthi.sungmobile.core.designsystem.theme.SungSpacing

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SungButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit) {
    Button(onClick, modifier = modifier.heightIn(min = SungSpacing.touchTarget), enabled = enabled,
        shapes = ButtonDefaults.shapes(), content = content)
}

@Composable
fun SectionHeading(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier.semantics { heading() }, style = MaterialTheme.typography.headlineSmall)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SungLoading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        if (LocalSungMotionEnabled.current) {
            LoadingIndicator(
                Modifier.size(SungSpacing.touchTarget)
            )
        }
    }
}
