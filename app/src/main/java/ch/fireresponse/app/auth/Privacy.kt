package ch.fireresponse.app.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.fireresponse.app.net.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Ein Abschnitt der Datenschutzerklärung; `text` ist Klartext, Absätze und Listenpunkte trennt eine Leerzeile. */
@Serializable
data class PrivacySection(@SerialName("titel") val title: String, val text: String)

/** `GET /api/v1/datenschutz`: Fassung und Abschnitte in der Reihenfolge der Seite des Servers. */
@Serializable
data class Privacy(val version: Int, @SerialName("abschnitte") val sections: List<PrivacySection>)

/** Ein Absatz oder eine Liste innerhalb eines Abschnitts. */
sealed interface PrivacyBlock {
    data class Paragraph(val text: String) : PrivacyBlock

    data class Bullets(val items: List<String>) : PrivacyBlock
}

private val BLANK_LINE = Regex("\\n\\s*\\n")
private const val BULLET = "– "

/** Zerlegt den Klartext eines Abschnitts: Absätze durch Leerzeilen getrennt, Listenpunkte beginnen mit «– » und bilden zusammen eine Liste. */
fun privacyBlocks(text: String): List<PrivacyBlock> {
    val blocks = mutableListOf<PrivacyBlock>()
    for (part in text.trim().split(BLANK_LINE).map { it.trim() }.filter { it.isNotEmpty() }) {
        val last = blocks.lastOrNull()
        if (part.startsWith(BULLET)) {
            val item = part.removePrefix(BULLET).trim()
            if (last is PrivacyBlock.Bullets) blocks[blocks.lastIndex] = PrivacyBlock.Bullets(last.items + item) else blocks += PrivacyBlock.Bullets(listOf(item))
        } else {
            blocks += PrivacyBlock.Paragraph(part)
        }
    }
    return blocks
}

/** Der Text der Datenschutzerklärung; `null` heisst: wird geladen. Er ist öffentlich und liegt nur im Arbeitsspeicher. */
class PrivacyViewModel(private val api: AuthApi) : ViewModel() {
    private val _state = MutableStateFlow<ApiResult<Privacy>?>(null)
    val state: StateFlow<ApiResult<Privacy>?> = _state.asStateFlow()

    fun load() {
        _state.value = null
        viewModelScope.launch { _state.value = api.privacy() }
    }
}
