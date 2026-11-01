package io.github.sorry4nothing.cevios.contacts

import io.github.sorry4nothing.cevios.chat.Profile
import io.github.sorry4nothing.cevios.chat.displayName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Eine Kontaktperson (Stufenleitung). `phone` ist nur für Eltern mit einem aktiven Kind in der Stufe gefüllt, sonst `null`. */
@Serializable
data class Contact(
    val id: String,
    val name: String,
    @SerialName("telefon") val phone: String? = null,
    @SerialName("bildUrl") val imageUrl: String? = null,
    /** Cevi-Name (Spitzname); `null` oder leer ohne Angabe. */
    val ceviName: String? = null,
) {
    /** Titel der Karte: der Cevi-Name, ohne ihn der volle Name. */
    val title get() = displayName(name, ceviName)

    /** Der volle Name darunter; nur, wenn der Titel ein Cevi-Name ist. */
    val fullName get() = name.takeIf { title != name }
}

@Serializable
data class ContactList(@SerialName("kontakte") val contacts: List<Contact>)

/** Body von `PATCH /api/v1/ich` für die eigene Nummer; leerer Text entfernt sie. */
@Serializable
data class PhoneRequest(@SerialName("telefon") val phone: String)

/** Was die App zur Telefonnummer prüft. Die Grenzen sind die der API; der Server prüft trotzdem. */
object PhoneRules {
    const val MIN_LENGTH = 6
    const val MAX_LENGTH = 30
    private val ALLOWED = Regex("[0-9+()/. \\-]+")

    /** Ziffern und `+ ( ) / . -` sowie Leerzeichen, 6 bis 30 Zeichen nach dem Trimmen, mindestens eine Ziffer. */
    fun valid(text: String): Boolean {
        val number = text.trim()
        return number.length in MIN_LENGTH..MAX_LENGTH && ALLOWED.matches(number) && number.any(Char::isDigit)
    }

    /** `tel:`-Adresse nur mit Ziffern und einem führenden Plus: So wählt die Telefon-App ohne Sonderzeichen. */
    fun dialUri(number: String): String = "tel:" + number.filter { it.isDigit() || it == '+' }

    /** Eine Nummer eintragen darf nur eine Stufenleitung. */
    fun maySet(profile: Profile?): Boolean = profile != null && profile.roles.any { it.type == "stufenleitung" }

    /** Der Bildschirm ist offen für eine Stufenleitung und für jede Person, die noch eine Nummer hinterlegt hat (Entfernen). */
    fun mayEdit(profile: Profile?): Boolean = maySet(profile) || !profile?.phone.isNullOrBlank()
}
