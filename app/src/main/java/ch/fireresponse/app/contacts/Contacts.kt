package ch.fireresponse.app.contacts

import ch.fireresponse.app.profile.Badge
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Die Kontaktperson eines Termins: Kommandant/in, sonst die erste Stellvertretung ohne Absage (das entscheidet der Server).
 * `phone` ist nur für Eltern und Teilnehmende mit einer aktiven Person in der Einheit gefüllt, sonst `null`.
 */
@Serializable
data class Contact(
    val id: String,
    val name: String,
    @SerialName("rolle") val role: String = "kommandant",
    @SerialName("telefon") val phone: String? = null,
    @SerialName("bildUrl") val imageUrl: String? = null,
    @SerialName("abzeichen") val badge: Badge? = null,
)

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
}
