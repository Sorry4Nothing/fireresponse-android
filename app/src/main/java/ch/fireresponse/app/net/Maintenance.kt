package ch.fireresponse.app.net

import ch.fireresponse.app.appointments.SWISS_ZONE
import ch.fireresponse.app.appointments.dayAndMonth
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/** Der öffentliche Stand der Wartung (`GET /api/v1/wartung`, antwortet immer 200). `text` und `ende` sind `null`, wenn das Panel sie nicht setzt. */
@Serializable
data class MaintenanceStatus(val aktiv: Boolean = false, val text: String? = null, val ende: String? = null, val seit: String? = null)

class MaintenanceApi(private val api: ApiClient) {
    suspend fun status(): ApiResult<MaintenanceStatus> = api.get("/api/v1/wartung")
}

private val CLOCK = DateTimeFormatter.ofPattern("HH:mm")

/**
 * «22:00 Uhr» für das Ende der Wartung, an einem anderen Tag als heute mit dem Datum davor («14. November, 22:00 Uhr»); `null`, wenn es
 * kein Ende gibt oder die Zeit nicht lesbar ist (der Server meldet ISO 8601 mit Zeitzone).
 */
fun maintenanceEnd(ende: String?, today: LocalDate): String? {
    val end = runCatching { OffsetDateTime.parse(ende ?: return null).atZoneSameInstant(SWISS_ZONE) }.getOrNull() ?: return null
    val time = "${CLOCK.format(end)} Uhr"
    return if (end.toLocalDate() == today) time else "${dayAndMonth(end.toLocalDate())}, $time"
}
