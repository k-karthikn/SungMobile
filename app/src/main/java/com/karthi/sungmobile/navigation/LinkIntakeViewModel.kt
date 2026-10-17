package com.karthi.sungmobile.navigation

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import com.karthi.sungmobile.core.model.catalog.*
import kotlinx.coroutines.flow.*

data class LinkIntakeState(val choices: List<OpenMusicLink> = emptyList(), val error: String? = null)
class LinkIntakeViewModel(private val saved: SavedStateHandle) : ViewModel() {
    private val mutable = MutableStateFlow(LinkIntakeState())
    val state = mutable.asStateFlow()
    init { saved.get<String>("pending")?.let(::receive) }
    fun receive(intent: Intent) {
        val text = when (intent.action) {
            Intent.ACTION_SEND -> if (intent.type == "text/plain") intent.getStringExtra(Intent.EXTRA_TEXT) else null
            Intent.ACTION_VIEW -> intent.dataString
            else -> null
        } ?: return
        receive(text)
    }
    fun receive(text: String) {
        saved["pending"] = text.take(16_385)
        mutable.value = try { LinkIntakeState(choices = MusicLinks.extract(text)) }
        catch (_: Exception) { LinkIntakeState(error = "This text has no supported YouTube link. Check the address and try again.") }
    }
    fun dismiss() { saved.remove<String>("pending"); mutable.value = LinkIntakeState() }
}
