package io.github.sorry4nothing.cevios.chat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatPerson(
    val id: String,
    val name: String,
    /** Nur bei Nachrichten gefüllt (`autor.stufenleitung`): Die Person ist Stufenleitung der Stufe des Chats. */
    @SerialName("stufenleitung") val groupLeader: Boolean = false,
    /** Pfad zum Profilbild (`GET /api/v1/personen/{id}/bild`); `null` ohne Bild. Nur bei Autoren von Nachrichten gefüllt. */
    @SerialName("bildUrl") val imageUrl: String? = null,
    /** Cevi-Name (Spitzname); `null` oder leer ohne Angabe. */
    val ceviName: String? = null,
) {
    /** Im Chat steht der Cevi-Name, ohne ihn der volle Name. */
    val displayName get() = displayName(name, ceviName)
}

/** Cevi-Name, wenn einer da ist (nicht leer), sonst der volle Name. */
fun displayName(name: String, ceviName: String?): String = ceviName?.trim()?.takeIf { it.isNotEmpty() } ?: name

@Serializable
data class ChatAgeGroup(val id: String, val name: String, @SerialName("kuerzel") val abbreviation: String)

/** Ein Chat der Person: `type` ist «stufe» (Gruppenchat) oder «direkt» (Einzelchat Eltern und Stufenleitung). */
@Serializable
data class Chat(
    val id: String,
    @SerialName("typ") val type: String,
    @SerialName("stufe") val ageGroup: ChatAgeGroup,
    @SerialName("eltern") val parent: ChatPerson? = null,
    @SerialName("stufenleitung") val groupLeaders: List<ChatPerson> = emptyList(),
    /** Nachrichten anderer, die die Person noch nicht gesehen hat (zählt der Server nach dem Gelesen-Stand). */
    @SerialName("ungelesen") val unread: Int = 0,
    /** Neueste für die Person sichtbare Nachricht, vom Server auf 120 Zeichen gekürzt; `null`, solange es keine gibt. */
    @SerialName("letzteNachricht") val lastMessage: LastMessage? = null,
) {
    val isGroup get() = type == "stufe"
}

@Serializable
data class ChatList(val chats: List<Chat>)

/** Vorschau der letzten Nachricht in der Chatliste. `own` heisst: Die angemeldete Person hat sie geschrieben. */
@Serializable
data class LastMessage(
    val text: String,
    @SerialName("erstelltAm") val createdAt: String,
    @SerialName("eigene") val own: Boolean,
    @SerialName("autor") val author: ChatPerson,
)

@Serializable
data class ProgramRef(val id: String, @SerialName("titel") val title: String)

/** `text` ist bei Nachrichten, die die App erwartet, immer gefüllt; ein fehlender Text zerstört den Chat nicht (`orEmpty()`). */
@Serializable
data class ChatMessage(
    val id: String,
    val text: String? = null,
    @SerialName("erstelltAm") val createdAt: String,
    @SerialName("autor") val author: ChatPerson,
    @SerialName("programm") val program: ProgramRef? = null,
    @SerialName("eigene") val own: Boolean = false,
)

@Serializable
data class MessagePage(@SerialName("nachrichten") val messages: List<ChatMessage>, @SerialName("naechster") val next: String? = null)

@Serializable
data class SendMessageRequest(val text: String)

@Serializable
data class MessageIdResponse(val id: String)


/**
 * Die geladenen Nachrichten eines Chats, neueste zuerst. Die IDs sind UUIDv7 in Kleinbuchstaben und
 * ordnen sich als Text nach der Zeit.
 */
data class Conversation(val messages: List<ChatMessage> = emptyList(), val next: String? = null) {
    /**
     * Arbeitet die neueste Seite ein, ohne bereits geladene ältere Nachrichten zu verlieren.
     * Kamen seit dem letzten Laden mehr neue Nachrichten, als eine Seite fasst, bleibt eine Lücke: Dann gilt nur
     * die neue Seite, die älteren holt «Ältere laden» nach.
     */
    fun withNewestPage(page: MessagePage): Conversation {
        val oldest = page.messages.lastOrNull()?.id ?: return Conversation()
        val newestKnown = messages.firstOrNull()?.id
        if (newestKnown == null || newestKnown < oldest) return Conversation(page.messages, page.next)
        val older = messages.filter { it.id < oldest }
        return Conversation(page.messages + older, if (older.isEmpty()) page.next else next)
    }

    /** Wie viele sichtbare Nachrichten anderer neuer sind als `knownNewestId` (für die Ansage «2 neue Nachrichten»). */
    fun newFromOthers(knownNewestId: String?): Int =
        if (knownNewestId == null) 0 else messages.count { it.id > knownNewestId && !it.own }

    /** Hängt eine ältere Seite an. */
    fun withOlder(page: MessagePage): Conversation {
        val oldest = messages.lastOrNull()?.id
        val fresh = if (oldest == null) page.messages else page.messages.filter { it.id < oldest }
        return Conversation(messages + fresh, page.next)
    }
}

/** Text im Zähler der Chatzeile: leer bei 0, ab 100 «99+». */
fun unreadBadgeText(count: Int): String? = when {
    count <= 0 -> null
    count > MAX_UNREAD_SHOWN -> "$MAX_UNREAD_SHOWN+"
    else -> count.toString()
}

private const val MAX_UNREAD_SHOWN = 99
