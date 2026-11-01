package ch.fireresponse.app.profile

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileRole(@SerialName("stufeId") val squadId: String, @SerialName("typ") val type: String)

@Serializable
data class ProfileSquad(val id: String, val name: String, @SerialName("kuerzel") val abbreviation: String = "")

@Serializable
data class ProfileParticipant(
    val id: String,
    @SerialName("stufeId") val squadId: String,
    @SerialName("vorname") val firstName: String = "",
)

/** Das eigene Profil (`GET /api/v1/ich`). Die Felder der Anzeige sind für die Termine egal. */
@Serializable
data class Profile(
    val id: String,
    val admin: Boolean = false,
    @SerialName("telefon") val phone: String? = null,
    @SerialName("rollen") val roles: List<ProfileRole> = emptyList(),
    @SerialName("stufen") val squads: List<ProfileSquad> = emptyList(),
    @SerialName("kinder") val participants: List<ProfileParticipant> = emptyList(),
    val name: String = "",
    @SerialName("benutzername") val username: String? = null,
    @SerialName("mussPasswortAendern") val mustChangePassword: Boolean = false,
    /** Wann die Person die Datenschutzerklärung zur Kenntnis genommen hat; `null`: noch nicht. */
    @SerialName("datenschutzGelesenAm") val privacyReadAt: String? = null,
    @SerialName("bildUrl") val imageUrl: String? = null,
    /** Die Push-Schalter beim Server: Sie sind standardmässig an. */
    @SerialName("pushProgramme") val pushAppointments: Boolean = true,
    /** Der Server liefert ihn nur Personen mit Stufenrolle; `null`: kein Schalter anzeigen. */
    @SerialName("pushAnmeldungen") val pushRegistrations: Boolean? = null,
    /** Spitzname; `null` oder leer ohne Angabe. */
    val ceviName: String? = null,
) {
    fun isCommander(squadId: String) = roles.any { it.type == "stufenleitung" && it.squadId == squadId }
}

/** Spitzname, wenn einer da ist (nicht leer), sonst der volle Name. */
fun displayName(name: String, ceviName: String?): String = ceviName?.trim()?.takeIf { it.isNotEmpty() } ?: name

/** Anfangsbuchstaben des ersten und des letzten Wortes des Namens, etwa «VN» für «Vorname Nachname». */
fun initials(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when (words.size) {
        0 -> ""
        1 -> words[0].take(1)
        else -> words.first().take(1) + words.last().take(1)
    }.uppercase()
}
