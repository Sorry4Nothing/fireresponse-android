package ch.fireresponse.app.appointments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.fireresponse.app.net.ApiError
import ch.fireresponse.app.net.ApiResult
import ch.fireresponse.app.net.Background
import ch.fireresponse.app.profile.Profile
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Zustand beim Speichern einer Antwort («ja» oder «nein»). `sentAt` merkt sich pro Teilnehmende (ID) oder für die eigene
 * Anmeldung ([LEADER_KEY]) die Uhrzeit des letzten Sendens: «Gesendet 12:10».
 */
data class RsvpState(
    val running: Boolean = false,
    val error: ApiError? = null,
    val sentAt: Map<String, String> = emptyMap(),
    /** Was gerade gespeichert wird: Schlüssel (Teilnehmende oder [LEADER_KEY]) und die gewählte Antwort. */
    val pendingKey: String? = null,
    val pendingAttending: Boolean? = null,
)

const val LEADER_KEY = "leitung"

private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")

/** `null` heisst: wird geladen. Die Daten bleiben nur im Arbeitsspeicher. */
class AppointmentsViewModel(private val api: AppointmentsApi) : ViewModel() {
    private val _list = MutableStateFlow<ApiResult<AppointmentList>?>(null)
    val list: StateFlow<ApiResult<AppointmentList>?> = _list.asStateFlow()

    private val _detail = MutableStateFlow<ApiResult<Appointment>?>(null)
    val detail: StateFlow<ApiResult<Appointment>?> = _detail.asStateFlow()

    private val _rsvp = MutableStateFlow(RsvpState())
    val rsvp: StateFlow<RsvpState> = _rsvp.asStateFlow()

    private val _registrations = MutableStateFlow<ApiResult<Registrations>?>(null)
    val registrations: StateFlow<ApiResult<Registrations>?> = _registrations.asStateFlow()

    /** Für die Rechte bei den Anmeldungen (Kantonsadmin, Kommando, Leitung); `null`, solange es lädt. */
    private val _profile = MutableStateFlow<ApiResult<Profile>?>(null)
    val profile: StateFlow<ApiResult<Profile>?> = _profile.asStateFlow()

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
    fun loadRegistrations(appointmentId: String) {
        _registrations.value = null
        viewModelScope.launch { _registrations.value = api.registrations(appointmentId) }
    }

    /**
     * Liest den Termin im Takt still neu: Es gibt keinen Ladezustand, ein Fehler lässt den alten Stand stehen, und
     * während eine Antwort gespeichert wird, bleibt der Stand, den die Person gerade bestätigt.
     */
    fun refreshDetail() {
        val id = openedId ?: return
        viewModelScope.launch(Background()) {
            val appointment = api.read(id)
            if (openedId != id || _rsvp.value.running) return@launch
            if (appointment is ApiResult.Ok) _detail.value = appointment
        }
    }

    fun loadDetail(id: String) {
        detailJob?.cancel()
        openedId = id
        _detail.value = null
        _rsvp.value = RsvpState()
        detailJob = viewModelScope.launch { _detail.value = api.read(id) }
    }

    fun rsvpParticipant(appointmentId: String, participantId: String, attending: Boolean) =
        save(appointmentId, participantId, attending) { api.setAttendance(appointmentId, participantId, attending) }

    fun rsvpLeader(appointmentId: String, attending: Boolean) =
        save(appointmentId, LEADER_KEY, attending) { api.setLeaderRsvp(appointmentId, attending) }

    /**
     * Speichert und liest den Termin danach neu: Der Server ist die einzige Wahrheit über den Stand, auch über die
     * Kontaktperson (wer mit «nein» absagt, gibt sie an die Stellvertretung ab). Bei einem Fehler bleibt der alte Stand
     * sichtbar, die Pillen springen also zurück.
     */
    private fun save(appointmentId: String, key: String, attending: Boolean, block: suspend () -> ApiResult<Unit>) {
        if (_rsvp.value.running) return
        val existing = _rsvp.value.sentAt
        _rsvp.value = RsvpState(running = true, sentAt = existing, pendingKey = key, pendingAttending = attending)
        viewModelScope.launch {
            when (val result = block()) {
                is ApiResult.Failure -> _rsvp.value = RsvpState(error = result.error, sentAt = existing)
                is ApiResult.Ok -> {
                    _rsvp.value = RsvpState(sentAt = existing + (key to TIME_FORMAT.format(LocalTime.now(SWISS_ZONE))))
                    val fresh = api.read(appointmentId)
                    if (openedId == appointmentId) _detail.value = fresh
                }
            }
        }
    }

    private companion object {
        const val WINDOW_MONTHS = 6L
    }
}
