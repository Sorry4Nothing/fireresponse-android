package ch.fireresponse.app.maintenance

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.fireresponse.app.R
import ch.fireresponse.app.appointments.SWISS_ZONE
import ch.fireresponse.app.net.ApiError
import ch.fireresponse.app.net.ApiResult
import ch.fireresponse.app.net.MaintenanceApi
import ch.fireresponse.app.net.MaintenanceStatus
import ch.fireresponse.app.net.maintenanceEnd
import ch.fireresponse.app.ui.PrimaryButton
import ch.fireresponse.app.ui.StatePage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Was der Wartungsbildschirm zeigt: den Stand des Servers (`null`, solange er nicht geladen ist) und das Ergebnis des letzten Prüfens. */
data class MaintenanceState(
    val status: MaintenanceStatus? = null,
    val checking: Boolean = false,
    /** Die Prüfung ergab: noch in Wartung. */
    val stillActive: Boolean = false,
    /** Die Prüfung kam nicht an. */
    val offline: Boolean = false,
)

/**
 * Der Stand der Wartung. Er wird einmal geladen, wenn der Bildschirm erscheint, und sonst nur, wenn die Person «Erneut prüfen» tippt:
 * keine Dauerabfrage. `onOver` läuft, sobald der Server `aktiv: false` meldet.
 */
class MaintenanceViewModel(private val api: MaintenanceApi, private val onOver: () -> Unit) : ViewModel() {
    private val _state = MutableStateFlow(MaintenanceState())
    val state: StateFlow<MaintenanceState> = _state.asStateFlow()

    /** Holt Text und Ende; ein Fehler lässt den Standardtext stehen. */
    fun load() {
        viewModelScope.launch {
            (api.status() as? ApiResult.Ok)?.let { result -> _state.value = _state.value.copy(status = result.value) }
        }
    }

    fun check() {
        if (_state.value.checking) return
        _state.value = _state.value.copy(checking = true, stillActive = false, offline = false)
        viewModelScope.launch {
            when (val result = api.status()) {
                is ApiResult.Ok ->
                    if (result.value.aktiv) _state.value = MaintenanceState(result.value, stillActive = true)
                    else {
                        _state.value = MaintenanceState(result.value)
                        onOver()
                    }
                is ApiResult.Failure -> _state.value = _state.value.copy(checking = false, offline = true)
            }
        }
    }
}

/**
 * Der Satz des Bildschirms: Text der Verwaltung (sonst der Standardtext), das voraussichtliche Ende und der Hinweis, dass nichts
 * verloren geht. Reine Funktion, damit sie sich ohne Oberfläche prüfen lässt.
 */
fun maintenanceText(standard: String, untilFormat: String, saved: String, status: MaintenanceStatus?, today: LocalDate): String {
    val base = status?.text?.takeIf { it.isNotBlank() } ?: standard
    val end = maintenanceEnd(status?.ende, today)?.let { untilFormat.format(it) }
    return listOfNotNull(base, end, saved).joinToString(" ")
}

/** Ganzer Bildschirm über der App, solange der Server in Wartung ist (Design S10/T10). */
@Composable
fun MaintenanceScreen(state: MaintenanceState, onCheck: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.safeDrawingPadding()) {
            StatePage(
                R.drawable.ic_wrench,
                stringResource(R.string.maintenance_title),
                maintenanceText(
                    stringResource(R.string.maintenance_default),
                    stringResource(R.string.maintenance_until),
                    stringResource(R.string.maintenance_saved),
                    state.status,
                    LocalDate.now(SWISS_ZONE),
                ),
            ) {
                PrimaryButton(
                    stringResource(R.string.maintenance_check), onCheck, icon = R.drawable.ic_refresh,
                    loading = state.checking, loadingText = stringResource(R.string.working),
                )
                val hint = when {
                    state.stillActive -> stringResource(R.string.maintenance_still)
                    state.offline -> stringResource(ApiError.NETWORK.text)
                    else -> null
                }
                hint?.let {
                    Text(
                        it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
        }
    }
}
