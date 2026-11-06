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

/**
 * Chips mit Namen oder Kreise mit Initiale? Die Zeile hat neben Kürzel, Zeit und «x von y» Platz für gut zehn Buchstaben in den
 * Chips (zwei Personen) oder vierzehn (eine Person). Sind die Namen länger, würden sie die Zeit aus der Zeile drängen: Dann
 * stehen wie ab drei Personen die Kreise da. Der Screenreader hört immer die ganzen Namen.
 */
fun showNames(people: List<Person>): Boolean = when (people.size) {
    1 -> people.single().name.length <= 14
    2 -> people.sumOf { it.name.length } <= 10
    else -> false
}

enum class Stand { YES, NO, OPEN }

/** Eine Person mit ihrem Stand; `name` ist der Vorname oder, bei der eigenen Anmeldung, `ownLabel`. */
data class Person(val name: String, val stand: Stand)

/** Die eigenen teilnehmenden Personen, dann die eigene Anmeldung; leer, wenn die Übung keine Antwort verlangt. */
fun Appointment.people(ownLabel: String): List<Person> {
    if (!registrable) return emptyList()
    val stand = { status: String? -> when (status) { "ja" -> Stand.YES; "nein" -> Stand.NO; else -> Stand.OPEN } }
    return participants.map { Person(it.firstName, stand(it.status)) } + listOfNotNull(ownRegistration?.let { Person(ownLabel, stand(it)) })
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
