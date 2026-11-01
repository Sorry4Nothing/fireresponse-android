package io.github.sorry4nothing.cevios.images

import androidx.compose.runtime.compositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sorry4nothing.cevios.net.ApiResult
import io.github.sorry4nothing.cevios.net.Background
import io.github.sorry4nothing.cevios.programs.AgeGroupList
import io.github.sorry4nothing.cevios.programs.ProgramsApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Stufen-ID zu Logo-Pfad, nur für Stufen mit eigenem Logo. Chat, Profil und Programme tragen den Pfad nicht, nur `GET /api/v1/stufen`. */
fun stageLogoPaths(list: AgeGroupList): Map<String, String> =
    list.ageGroups.mapNotNull { group -> group.logoUrl?.let { group.id to it } }.toMap()

/**
 * Die Logo-Pfade der Stufen, einmal pro Sitzung geladen. Schlägt das Laden fehl, bleibt die Karte leer und alle Stufen zeigen das
 * Cevi-Logo. shortcut: kein erneuter Versuch in der Sitzung, nachrüsten, wenn das im Feld stört.
 */
class StageLogos(api: ProgramsApi) : ViewModel() {
    private val _paths = MutableStateFlow<Map<String, String>>(emptyMap())
    val paths: StateFlow<Map<String, String>> = _paths.asStateFlow()

    init {
        viewModelScope.launch(Background()) {
            (api.ageGroups() as? ApiResult.Ok)?.let { _paths.value = stageLogoPaths(it.value) }
        }
    }
}

/** Die Logo-Pfade der Stufen der angemeldeten Person; leer ausserhalb einer Sitzung. */
val LocalStageLogos = compositionLocalOf<Map<String, String>> { emptyMap() }
