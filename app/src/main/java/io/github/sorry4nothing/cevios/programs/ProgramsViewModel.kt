package io.github.sorry4nothing.cevios.programs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sorry4nothing.cevios.chat.Profile
import io.github.sorry4nothing.cevios.contacts.ContactList
import io.github.sorry4nothing.cevios.net.ApiError
import io.github.sorry4nothing.cevios.net.ApiResult
import io.github.sorry4nothing.cevios.net.Background
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Zustand beim Speichern einer Antwort («ja» oder «nein»). `sentAt` merkt sich pro Kind (ID) oder für die eigene
 * Anmeldung ([LEADER_KEY]) die Uhrzeit des letzten Sendens: «Gesendet 12:10».
 */
data class RsvpState(
    val running: Boolean = false,
    val error: ApiError? = null,
    val sentAt: Map<String, String> = emptyMap(),
    /** Was gerade gespeichert wird: Schlüssel (Kind oder [LEADER_KEY]) und die gewählte Antwort. */
    val pendingKey: String? = null,
    val pendingAttending: Boolean? = null,
)

const val LEADER_KEY = "leitung"

/** Zustand beim Speichern von Ergänzungen oder Zuordnung durch Leitende. */
data class EditState(val running: Boolean = false, val error: ApiError? = null)

private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")

/** `null` heisst: wird geladen. Die Daten bleiben nur im Arbeitsspeicher. */
class ProgramsViewModel(private val api: ProgramsApi) : ViewModel() {
    private val _list = MutableStateFlow<ApiResult<ProgramList>?>(null)
    val list: StateFlow<ApiResult<ProgramList>?> = _list.asStateFlow()

    private val _detail = MutableStateFlow<ApiResult<Program>?>(null)
    val detail: StateFlow<ApiResult<Program>?> = _detail.asStateFlow()

    private val _rsvp = MutableStateFlow(RsvpState())
    val rsvp: StateFlow<RsvpState> = _rsvp.asStateFlow()

    private val _contacts = MutableStateFlow<ApiResult<ContactList>?>(null)
    val contacts: StateFlow<ApiResult<ContactList>?> = _contacts.asStateFlow()

    private val _registrations = MutableStateFlow<ApiResult<Registrations>?>(null)
    val registrations: StateFlow<ApiResult<Registrations>?> = _registrations.asStateFlow()

    private val _edit = MutableStateFlow(EditState())
    val edit: StateFlow<EditState> = _edit.asStateFlow()

    /** Für die Rechte bei der Zuordnung (Admin, Stufenleitung); `null`, solange es lädt. */
    private val _profile = MutableStateFlow<ApiResult<Profile>?>(null)
    val profile: StateFlow<ApiResult<Profile>?> = _profile.asStateFlow()

    private val _ageGroups = MutableStateFlow<ApiResult<AgeGroupList>?>(null)
    val ageGroups: StateFlow<ApiResult<AgeGroupList>?> = _ageGroups.asStateFlow()

    private var detailJob: Job? = null
    private var openedId: String? = null

    init {
        loadList()
        viewModelScope.launch { _profile.value = api.profile() }
    }

    fun loadList() {
        _list.value = null
        fetchList()
    }

    private fun fetchList() {
        viewModelScope.launch {
            val today = LocalDate.now(SWISS_ZONE)
            _list.value = api.list(today, today.plusMonths(WINDOW_MONTHS))
        }
    }

    /** Lädt die Übersicht der Anmeldungen neu; bis die Antwort da ist, zeigt der Bildschirm das Laden. */
    fun loadRegistrations(programId: String) {
        _registrations.value = null
        viewModelScope.launch { _registrations.value = api.registrations(programId) }
    }

    fun loadAgeGroups() {
        _ageGroups.value = null
        viewModelScope.launch { _ageGroups.value = api.ageGroups() }
    }

    fun resetEdit() {
        _edit.value = EditState()
    }

    fun saveAssignment(programId: String, request: AssignmentRequest, onSaved: () -> Unit) =
        saveEdit(programId, onSaved) { api.assign(programId, request) }

    /** Speichert, liest Detail und Liste danach neu und meldet den Erfolg. Bei einem Fehler bleibt der Bildschirm offen. */
    private fun saveEdit(programId: String, onSaved: () -> Unit, block: suspend () -> ApiResult<Unit>) {
        if (_edit.value.running) return
        _edit.value = EditState(running = true)
        viewModelScope.launch {
            when (val result = block()) {
                is ApiResult.Failure -> _edit.value = EditState(error = result.error)
                is ApiResult.Ok -> {
                    _edit.value = EditState()
                    val fresh = api.read(programId)
                    if (openedId == programId) _detail.value = fresh
                    fetchList()
                    onSaved()
                }
            }
        }
    }

    /**
     * Liest Programm und Kontakte im Takt still neu: Es gibt keinen Ladezustand, ein Fehler lässt den alten Stand stehen, und
     * während eine Antwort gespeichert wird, bleibt der Stand, den die Person gerade bestätigt.
     */
    fun refreshDetail() {
        val id = openedId ?: return
        viewModelScope.launch(Background()) {
            val program = api.read(id)
            val contacts = api.contacts(id)
            if (openedId != id || _rsvp.value.running) return@launch
            if (program is ApiResult.Ok) _detail.value = program
            if (contacts is ApiResult.Ok) _contacts.value = contacts
        }
    }

    fun loadDetail(id: String) {
        detailJob?.cancel()
        openedId = id
        _detail.value = null
        _rsvp.value = RsvpState()
        _contacts.value = null
        detailJob = viewModelScope.launch { _detail.value = api.read(id) }
        // Die Kontakte stören das Programm nicht: Ein Fehler dort zeigt nur keinen Abschnitt.
        viewModelScope.launch {
            val result = api.contacts(id)
            if (openedId == id) _contacts.value = result
        }
    }

    fun rsvpChild(programId: String, childId: String, attending: Boolean) =
        save(programId, childId, attending) { api.setAttendance(programId, childId, attending) }

    fun rsvpLeader(programId: String, attending: Boolean) =
        save(programId, LEADER_KEY, attending) { api.setLeaderRsvp(programId, attending) }

    /**
     * Speichert und liest das Programm danach neu: Der Server ist die einzige Wahrheit über den Stand. Bei einem Fehler
     * bleibt der alte Stand sichtbar, die Pillen springen also zurück.
     */
    private fun save(programId: String, key: String, attending: Boolean, block: suspend () -> ApiResult<Unit>) {
        if (_rsvp.value.running) return
        val existing = _rsvp.value.sentAt
        _rsvp.value = RsvpState(running = true, sentAt = existing, pendingKey = key, pendingAttending = attending)
        viewModelScope.launch {
            when (val result = block()) {
                is ApiResult.Failure -> _rsvp.value = RsvpState(error = result.error, sentAt = existing)
                is ApiResult.Ok -> {
                    _rsvp.value = RsvpState(sentAt = existing + (key to TIME_FORMAT.format(LocalTime.now(SWISS_ZONE))))
                    val fresh = api.read(programId)
                    if (openedId == programId) _detail.value = fresh
                    // Wer mit «nein» absagt, fehlt in den Kontakten: Antwortet eine SL, ändert sich die Karte sofort.
                    if (key == LEADER_KEY) {
                        val contacts = api.contacts(programId)
                        if (openedId == programId) _contacts.value = contacts
                    }
                }
            }
        }
    }

    private companion object {
        const val WINDOW_MONTHS = 6L
    }
}
