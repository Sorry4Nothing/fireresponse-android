package io.github.sorry4nothing.cevios.chat

import io.github.sorry4nothing.cevios.programs.SWISS_ZONE
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")

/** Eine Zeile im Verlauf: eine Nachricht oder ein Tagestrenner. */
sealed interface ConversationItem {
    data class Message(val message: ChatMessage) : ConversationItem

    data class DayDivider(val date: LocalDate) : ConversationItem
}

/** Wie ein Tagestrenner heisst: «Heute», «Gestern» oder das Datum. Der Text kommt aus den Ressourcen. */
sealed interface DayLabel {
    object Today : DayLabel

    object Yesterday : DayLabel

    data class On(val date: LocalDate) : DayLabel
}

fun dayLabel(day: LocalDate, today: LocalDate): DayLabel = when (day) {
    today -> DayLabel.Today
    today.minusDays(1) -> DayLabel.Yesterday
    else -> DayLabel.On(day)
}

/** Der Tag einer Zeit des Servers in Schweizer Zeit; `null`, wenn sie kein Datum ist. */
fun dayOf(createdAt: String): LocalDate? = try {
    Instant.parse(createdAt).atZone(SWISS_ZONE).toLocalDate()
} catch (e: DateTimeParseException) {
    null
}

/** «14:20» in Schweizer Zeit; leer, wenn die Zeit des Servers kein Datum ist. */
fun timeOf(createdAt: String): String = try {
    TIME_FORMAT.format(Instant.parse(createdAt).atZone(SWISS_ZONE))
} catch (e: DateTimeParseException) {
    ""
}

fun ChatMessage.day(): LocalDate? = dayOf(createdAt)

fun ChatMessage.timeText(): String = timeOf(createdAt)

/**
 * Fügt Tagestrenner ein. `messages` und Ergebnis sind neueste zuerst, wie der Verlauf (von unten nach oben gezeichnet):
 * Der Trenner steht deshalb nach der letzten, also ältesten Nachricht seines Tages und erscheint darüber. Nachrichten ohne
 * lesbares Datum bekommen keinen Trenner.
 */
fun withDayDividers(messages: List<ChatMessage>): List<ConversationItem> = buildList {
    messages.forEachIndexed { index, message ->
        add(ConversationItem.Message(message))
        val day = message.day()
        if (day != null && messages.getOrNull(index + 1)?.day() != day) add(ConversationItem.DayDivider(day))
    }
}

/** Anfangsbuchstaben des ersten und des letzten Wortes des Namens, etwa «VN» für «Vorname Nachname». */
fun initials(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when (words.size) {
        0 -> ""
        1 -> words[0].take(1)
        else -> words.first().take(1) + words.last().take(1)
    }.uppercase()
}
