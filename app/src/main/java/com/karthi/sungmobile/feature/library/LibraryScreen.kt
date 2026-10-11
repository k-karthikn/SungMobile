package com.karthi.sungmobile.feature.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.karthi.sungmobile.R
import com.karthi.sungmobile.core.designsystem.component.*
import com.karthi.sungmobile.core.designsystem.icon.*

@Composable
fun LibraryScreen(onSearch: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)) {
        SectionHeading(stringResource(R.string.library))
        Card {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SungIcon(SungSymbol.Library, modifier = Modifier.size(48.dp))
                Text(stringResource(R.string.library_empty_title), style = MaterialTheme.typography.headlineMedium)
                Text(stringResource(R.string.library_empty_body), style = MaterialTheme.typography.bodyLarge)
                SungButton(onSearch) { Text(stringResource(R.string.find_music)) }
            }
        }
    }
}
