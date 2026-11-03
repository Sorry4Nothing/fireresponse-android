package ch.fireresponse.app.widget

import ch.fireresponse.app.appointments.Appointment
import ch.fireresponse.app.appointments.SWISS_ZONE
import ch.fireresponse.app.appointments.Timing
import ch.fireresponse.app.appointments.time
import java.time.ZonedDateTime

/** So viele Übungen zeigt das Widget (4 Felder breit, 2 hoch). */
const val WIDGET_ROWS = 2

/** Die nächsten Übungen: nicht abgesagt, nicht vorbei, nach Beginn geordnet. Ohne lesbare Zeit zählt eine Übung nicht. */
fun upcoming(appointments: List<Appointment>, now: ZonedDateTime, count: Int = WIDGET_ROWS): List<Appointment> =
    appointments.asSequence()
        .filter { !it.cancelled }
        .mapNotNull { appointment -> appointment.time()?.let { appointment to it } }
        .filter { (_, time) ->
            when (time) {
                is Timing.Timed -> !time.end.isBefore(now)
                is Timing.AllDay -> !time.until.isBefore(now.toLocalDate())
            }
        }
        .sortedBy { (_, time) ->
            when (time) {
                is Timing.Timed -> time.start.toInstant()
                is Timing.AllDay -> time.from.atStartOfDay(SWISS_ZONE).toInstant()
            }
        }
        .map { it.first }
        .take(count)
        .toList()

/**
 * Wie viele der eigenen Antworten da sind, ohne Namen: das Widget steht auf dem Startbildschirm. Zählt die eigenen teilnehmenden
 * Personen und, bei Kommando und Leitung, die eigene Anmeldung. `total` ist 0, wenn die Übung keine Antwort verlangt.
 */
data class Answers(val given: Int, val total: Int) {
    val open get() = total > 0 && given == 0
    val done get() = total > 0 && given == total
}

fun Appointment.answers(): Answers {
    if (!registrable) return Answers(0, 0)
    val answered = { status: String? -> status == "ja" || status == "nein" }
    val own = ownRegistration
    return Answers(
        given = participants.count { answered(it.status) } + if (own != null && answered(own)) 1 else 0,
        total = participants.size + if (own != null) 1 else 0,
    )
}
