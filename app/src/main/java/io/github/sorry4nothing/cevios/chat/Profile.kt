package io.github.sorry4nothing.cevios.chat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileRole(@SerialName("stufeId") val ageGroupId: String, @SerialName("typ") val type: String)

@Serializable
data class ProfileAgeGroup(val id: String, val name: String, @SerialName("kuerzel") val abbreviation: String = "")

@Serializable
data class ProfileChild(
    val id: String,
    @SerialName("stufeId") val ageGroupId: String,
    @SerialName("vorname") val firstName: String = "",
)

/** Das eigene Profil (`GET /api/v1/ich`). Die Felder der Anzeige sind für Chat und Programme egal. */
@Serializable
data class Profile(
    val id: String,
    val admin: Boolean = false,
    @SerialName("telefon") val phone: String? = null,
    @SerialName("rollen") val roles: List<ProfileRole> = emptyList(),
    @SerialName("stufen") val ageGroups: List<ProfileAgeGroup> = emptyList(),
    @SerialName("kinder") val children: List<ProfileChild> = emptyList(),
    val name: String = "",
    @SerialName("benutzername") val username: String? = null,
    @SerialName("mussPasswortAendern") val mustChangePassword: Boolean = false,
    /** Wann die Person die Datenschutzerklärung zur Kenntnis genommen hat; `null`: noch nicht. */
    @SerialName("datenschutzGelesenAm") val privacyReadAt: String? = null,
    @SerialName("bildUrl") val imageUrl: String? = null,
    /** Die Push-Schalter beim Server: Sie sind standardmässig an. */
    @SerialName("pushChat") val pushChat: Boolean = true,
    @SerialName("pushProgramme") val pushPrograms: Boolean = true,
    /** Der Server liefert ihn nur Personen mit Stufenrolle; `null`: kein Schalter anzeigen. */
    @SerialName("pushAnmeldungen") val pushRegistrations: Boolean? = null,
    /** Cevi-Name (Spitzname); `null` oder leer ohne Angabe. */
    val ceviName: String? = null,
) {
    fun isGroupLeader(ageGroupId: String) = roles.any { it.type == "stufenleitung" && it.ageGroupId == ageGroupId }
}

/** `parentUserId` nur für Stufenleitungen; ein Elternteil lässt es weg (und es bleibt dann im JSON weg). */
@Serializable
data class DirectChatRequest(@SerialName("stufeId") val ageGroupId: String, @SerialName("elternUserId") val parentUserId: String? = null)

@Serializable
data class ChatIdResponse(val id: String)

/** Was man mit einer Nachricht tun kann (langes Drücken und Screenreader-Aktionen). */
enum class MessageAction { OPEN_DIRECT_CHAT }

/**
 * Wann die App eine Aktion anbietet: «Einzelchat öffnen» leitet sie aus dem Profil ab. Die Entscheidung trifft trotzdem der
 * Server: Eine Aktion, die er ablehnt, zeigt die App als Fehler.
 */
object ChatRules {
    /** Eine Stufenleitung öffnet den Einzelchat mit einem Elternteil über dessen Nachricht im Gruppenchat. */
    fun mayOpenDirectChat(chat: Chat?, message: ChatMessage, profile: Profile?) =
        chat != null && profile != null && chat.isGroup && !message.own &&
            profile.isGroupLeader(chat.ageGroup.id) && !message.author.groupLeader

    /** Alle Aktionen, die zu einer Nachricht erlaubt sind, in der Reihenfolge des Dialogs. Leer heisst: keine Aktion. */
    fun actions(chat: Chat?, message: ChatMessage, profile: Profile?): List<MessageAction> = buildList {
        if (mayOpenDirectChat(chat, message, profile)) add(MessageAction.OPEN_DIRECT_CHAT)
    }

    /**
     * Stufen, in denen ein Elternteil einen Einzelchat starten kann: eigene Kinder in der Stufe, noch kein Einzelchat
     * dort, und in der eigenen Stufenleitung-Stufe nimmt man nur als Stufenleitung teil.
     */
    fun ageGroupsForDirectChat(profile: Profile?, chats: List<Chat>): List<ProfileAgeGroup> {
        if (profile == null) return emptyList()
        return profile.ageGroups.filter { ageGroup ->
            profile.children.any { it.ageGroupId == ageGroup.id } &&
                !profile.isGroupLeader(ageGroup.id) &&
                chats.none { !it.isGroup && it.ageGroup.id == ageGroup.id }
        }
    }
}
