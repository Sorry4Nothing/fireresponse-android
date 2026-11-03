package ch.fireresponse.app.profile

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Eine Einheit des Kontos mit der Rolle der Person darin: «kommandant», «stellvertretung» oder «leitung». */
@Serializable
data class ProfileSquad(
    val id: String,
    val name: String,
    @SerialName("typ") val type: String = "jf",
    @SerialName("bezirk") val district: String = "",
    @SerialName("rolle") val role: String,
    /** Das eigene Abzeichen in dieser Einheit; `null` ohne Rang. */
    @SerialName("abzeichen") val badge: Badge? = null,
)

/** Eine aktive teilnehmende Person, für die das Konto antwortet; `kind` ist «eltern» oder «selbst». */
@Serializable
data class ProfileParticipant(
    val id: String,
    @SerialName("vorname") val firstName: String = "",
    @SerialName("nachname") val lastName: String = "",
    @SerialName("einheitId") val squadId: String,
    @SerialName("art") val kind: String = "eltern",
    @SerialName("abzeichen") val badge: Badge? = null,
)

/**
 * Das Rang-Abzeichen: `abbreviation` (Kürzel, höchstens 20 Zeichen) ist immer da, dazu entweder `stars` (0 bis 5) oder `imageUrl`;
 * hat der Rang weder Sterne noch Bild, sind beide `null`. Kein Objekt heisst kein Rang.
 */
@Serializable
data class Badge(
    @SerialName("kuerzel") val abbreviation: String = "",
    @SerialName("sterne") val stars: Int? = null,
    @SerialName("bildUrl") val imageUrl: String? = null,
) {
    /** Die Sterne, die `RankBadge` zeigen kann (0 bis 2); darüber, und bei Bild-Abzeichen, lädt `RankMark` das Bild vom Server. */
    // shortcut: die Platzhalter-Bilder reichen nur bis 2 Sterne, bis das Design die Abzeichen 3 bis 5 liefert.
    val shownStars get() = stars?.takeIf { it in 0..2 }
}

/** Rollen des Kommandos. Auf Dateiebene: Ein privater `companion object` in einer `@Serializable`-Klasse versperrt dem Plug-in den Serializer. */
private val COMMAND_ROLES = setOf("kommandant", "stellvertretung")

/** Das eigene Profil (`GET /api/v1/ich`). */
@Serializable
data class Profile(
    val id: String,
    @SerialName("kantonsadmin") val admin: Boolean = false,
    @SerialName("telefon") val phone: String? = null,
    @SerialName("einheiten") val squads: List<ProfileSquad> = emptyList(),
    @SerialName("teilnehmende") val participants: List<ProfileParticipant> = emptyList(),
    val name: String = "",
    @SerialName("benutzername") val username: String? = null,
    @SerialName("mussPasswortAendern") val mustChangePassword: Boolean = false,
    /** Hat die Person der aktuellen Fassung der Einwilligung zugestimmt? Sonst sperrt die API die App-Routen. */
    @SerialName("einwilligung") val consented: Boolean = false,
    @SerialName("bildUrl") val imageUrl: String? = null,
    /** Die Push-Schalter beim Server: Sie sind standardmässig an. */
    @SerialName("pushProgramme") val pushAppointments: Boolean = true,
    /** Der Server liefert ihn nur Personen mit Einheitsrolle; `null`: kein Schalter anzeigen. */
    @SerialName("pushAnmeldungen") val pushRegistrations: Boolean? = null,
) {
    /** Das Konto gehört zu keiner Einheit: ohne Rolle und ohne Teilnehmende. Die Leitung muss es zuordnen. */
    val hasNoSquad get() = squads.isEmpty() && participants.isEmpty()

    /** Kommandant oder Stellvertretung der Einheit: das Kommando. */
    fun isCommand(squadId: String) = squads.any { it.id == squadId && it.role in COMMAND_ROLES }

    val isCommand get() = squads.any { it.role in COMMAND_ROLES }
    val isLeader get() = squads.any { it.role == "leitung" }
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
