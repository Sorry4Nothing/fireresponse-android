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
    /** Die Antwort der Leitung «ja» kam trotz freiem Platz auf die Warteliste: der letzte Platz ging in der Zwischenzeit an eine andere Person. */
    val lastSeatTaken: Boolean = false,
    /** Die Nummer auf der Warteliste nach `lastSeatTaken`. */
    val waitlistPosition: Int? = null,
)

const val LEADER_KEY = "leitung"

private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")

/** `null` heisst: wird geladen. Die Daten bleiben nur im Arbeitsspeicher. */
class AppointmentsViewModel(private val api: AppointmentsApi) : ViewModel() {
    private val _list = MutableStateFlow<ApiResult<AppointmentList>?>(null)
    val list: StateFlow<ApiResult<AppointmentList>?> = _list.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    /** Wahr, solange die Liste auf Zug der Person neu lädt; die Karten bleiben dabei stehen. */
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _refreshError = MutableStateFlow<ApiError?>(null)
    /** Warum das Neuladen auf Zug nicht ging; die alte Liste bleibt. `null` nach einem Erfolg oder beim nächsten Versuch. */
    val refreshError: StateFlow<ApiError?> = _refreshError.asStateFlow()

    private val _detail = MutableStateFlow<ApiResult<Appointment>?>(null)
    val detail: StateFlow<ApiResult<Appointment>?> = _detail.asStateFlow()

    private val _rsvp = MutableStateFlow(RsvpState())
    val rsvp: StateFlow<RsvpState> = _rsvp.asStateFlow()

    private val _registrations = MutableStateFlow<ApiResult<Registrations>?>(null)
    val registrations: StateFlow<ApiResult<Registrations>?> = _registrations.asStateFlow()

    private val _movedUp = MutableStateFlow(false)

    /** Die Person stand auf der Warteliste und hat jetzt einen Platz (gesehen beim Neulesen); bleibt, bis sie den Hinweis schliesst. */
    val movedUp: StateFlow<Boolean> = _movedUp.asStateFlow()

    fun dismissMovedUp() {
        _movedUp.value = false
    }

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
        _refreshError.value = null
        fetchList()
    }

    /** Neuladen durch Ziehen: im Gegensatz zu [loadList] ohne Ladezustand, und ein Fehler lässt die Liste stehen und wird gemeldet. */
    fun pullRefresh() {
        if (_refreshing.value) return
        _refreshing.value = true
        _refreshError.value = null
        viewModelScope.launch {
            val today = LocalDate.now(SWISS_ZONE)
            when (val result = api.list(today, today.plusMonths(WINDOW_MONTHS))) {
                is ApiResult.Ok -> _list.value = result
                is ApiResult.Failure -> _refreshError.value = result.error
            }
            _refreshing.value = false
        }
    }

    private fun fetchList() {
        viewModelScope.launch {
            val today = LocalDate.now(SWISS_ZONE)
            _list.value = api.list(today, today.plusMonths(WINDOW_MONTHS))
        }
    }

    /** Die Karten der Liste zeigen die Antworten: Nach dem Speichern lädt sie still neu. Ein Fehler lässt den alten Stand stehen. */
    private fun refreshList() {
        viewModelScope.launch(Background()) {
            val today = LocalDate.now(SWISS_ZONE)
            (api.list(today, today.plusMonths(WINDOW_MONTHS)) as? ApiResult.Ok)?.let { _list.value = it }
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
            if (appointment is ApiResult.Ok) {
                // Nachgerückt: gestern auf der Warteliste, jetzt mit Platz.
                if ((_detail.value as? ApiResult.Ok)?.value?.onWaitlist == true && appointment.value.ownRegistration == "ja") _movedUp.value = true
                _detail.value = appointment
                refreshPlaces(appointment.value)
            }
        }
    }

    fun loadDetail(id: String) {
        detailJob?.cancel()
        openedId = id
        _detail.value = null
        _rsvp.value = RsvpState()
        _movedUp.value = false
        _registrations.value = null
        detailJob = viewModelScope.launch {
            val result = api.read(id)
            _detail.value = result
            (result as? ApiResult.Ok)?.let { refreshPlaces(it.value) }
        }
    }

    fun rsvpParticipant(appointmentId: String, participantId: String, attending: Boolean) =
        save(appointmentId, participantId, attending) { api.setAttendance(appointmentId, participantId, attending).withoutAnswer() }

    /** Die eigene Anmeldung der Leitung; war ein Platz frei und kommt «warteliste» zurück, sagt [RsvpState.lastSeatTaken] es. */
    fun rsvpLeader(appointmentId: String, attending: Boolean) {
        val hadFreePlace = (_detail.value as? ApiResult.Ok)?.value?.places?.let { !it.full && it.free > 0 } == true
        save(appointmentId, LEADER_KEY, attending, hadFreePlace) { api.setLeaderRsvp(appointmentId, attending) }
    }

    /**
     * Speichert und liest den Termin danach neu: Der Server ist die einzige Wahrheit über den Stand, auch über die
     * Kontaktperson (wer mit «nein» absagt, gibt sie an die Stellvertretung ab). Bei einem Fehler bleibt der alte Stand
     * sichtbar, die Pillen springen also zurück.
     */
    private fun save(appointmentId: String, key: String, attending: Boolean, hadFreePlace: Boolean = false, block: suspend () -> ApiResult<LeaderAnswer?>) {
        if (_rsvp.value.running) return
        val existing = _rsvp.value.sentAt
        _rsvp.value = RsvpState(running = true, sentAt = existing, pendingKey = key, pendingAttending = attending)
        viewModelScope.launch {
            when (val result = block()) {
                is ApiResult.Failure -> _rsvp.value = RsvpState(error = result.error, sentAt = existing)
                is ApiResult.Ok -> {
                    val answer = result.value
                    val raced = hadFreePlace && attending && answer?.status == "warteliste"
                    _rsvp.value = RsvpState(
                        sentAt = existing + (key to TIME_FORMAT.format(LocalTime.now(SWISS_ZONE))),
                        lastSeatTaken = raced,
                        waitlistPosition = answer?.position.takeIf { raced },
                    )
                    val fresh = api.read(appointmentId)
                    if (openedId == appointmentId) {
                        _detail.value = fresh
                        (fresh as? ApiResult.Ok)?.let { refreshPlaces(it.value) }
                    }
                    refreshList()
                }
            }
        }
    }

    /** Wer die Plätze belegt (Namen und Bilder), steht nur in der Übersicht der Anmeldungen: bei Leiter-Plätzen still nachladen. */
    private fun refreshPlaces(appointment: Appointment) {
        if (appointment.places == null) return
        viewModelScope.launch(Background()) {
            (api.registrations(appointment.id) as? ApiResult.Ok)?.let { if (openedId == appointment.id) _registrations.value = it }
        }
    }

    private fun ApiResult<Unit>.withoutAnswer(): ApiResult<LeaderAnswer?> = when (this) {
        is ApiResult.Ok -> ApiResult.Ok(null)
        is ApiResult.Failure -> this
    }

    private companion object {
        const val WINDOW_MONTHS = 6L
    }
}
