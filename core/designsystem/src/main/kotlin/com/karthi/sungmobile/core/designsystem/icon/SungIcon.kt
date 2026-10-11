package com.karthi.sungmobile.core.designsystem.icon

import androidx.annotation.DrawableRes
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.karthi.sungmobile.core.designsystem.R

enum class SungSymbol(@param:DrawableRes val resource: Int) {
    Home(R.drawable.ic_home), HomeOutline(R.drawable.ic_home_outline),
    Search(R.drawable.ic_search), Library(R.drawable.ic_library), LibraryOutline(R.drawable.ic_library_outline),
    Play(R.drawable.ic_play), Pause(R.drawable.ic_pause), Settings(R.drawable.ic_settings),
    Back(R.drawable.ic_back), Chevron(R.drawable.ic_chevron), Disc(R.drawable.ic_disc),
    Close(R.drawable.ic_close), Plus(R.drawable.ic_plus), Volume(R.drawable.ic_volume),
}

/** Null description for decorative icons whose parent already has a label. */
@Composable
fun SungIcon(symbol: SungSymbol, description: String? = null, modifier: Modifier = Modifier) {
    Icon(painterResource(symbol.resource), description, modifier)
}
