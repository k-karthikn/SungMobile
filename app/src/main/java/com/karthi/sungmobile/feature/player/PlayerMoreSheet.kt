package com.karthi.sungmobile.feature.player

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.R
import com.karthi.sungmobile.app.PlayerActions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlayerMoreSheet(actions: PlayerActions, dismiss: () -> Unit,
    content: @Composable () -> Unit) {
    ModalBottomSheet(onDismissRequest = dismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Playback options", style = MaterialTheme.typography.headlineSmall)
            content()
            HorizontalDivider()
            OutlinedButton({ dismiss(); actions.stop() }, Modifier.fillMaxWidth().testTag("stop")) {
                Text(stringResource(R.string.stop))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
