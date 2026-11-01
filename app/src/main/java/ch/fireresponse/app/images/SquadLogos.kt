package ch.fireresponse.app.images

import androidx.compose.runtime.compositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.fireresponse.app.net.ApiResult
import ch.fireresponse.app.net.Background
import ch.fireresponse.app.appointments.SquadList
import ch.fireresponse.app.appointments.AppointmentsApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Stufen-ID zu Logo-Pfad, nur für Stufen mit eigenem Logo. Chat, Profil und Programme tragen den Pfad nicht, nur `GET /api/v1/stufen`. */
fun squadLogoPaths(list: SquadList): Map<String, String> =
    list.squads.mapNotNull { group -> group.logoUrl?.let { group.id to it } }.toMap()

/**
 * Die Logo-Pfade der Stufen, einmal pro Sitzung geladen. Schlägt das Laden fehl, bleibt die Karte leer und alle Stufen zeigen das
 * Logo. shortcut: kein erneuter Versuch in der Sitzung, nachrüsten, wenn das im Feld stört.
 */
class SquadLogos(api: AppointmentsApi) : ViewModel() {
    private val _paths = MutableStateFlow<Map<String, String>>(emptyMap())
    val paths: StateFlow<Map<String, String>> = _paths.asStateFlow()

    init {
        viewModelScope.launch(Background()) {
            (api.squads() as? ApiResult.Ok)?.let { _paths.value = squadLogoPaths(it.value) }
        }
    }
}

/** Die Logo-Pfade der Stufen der angemeldeten Person; leer ausserhalb einer Sitzung. */
val LocalSquadLogos = compositionLocalOf<Map<String, String>> { emptyMap() }
