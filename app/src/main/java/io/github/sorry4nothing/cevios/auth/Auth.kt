package io.github.sorry4nothing.cevios.auth

import androidx.annotation.StringRes
import io.github.sorry4nothing.cevios.R
import io.github.sorry4nothing.cevios.net.ApiError
import io.github.sorry4nothing.cevios.net.ApiResult
import kotlinx.serialization.Serializable

@Serializable
data class SignInRequest(val username: String, val password: String)

/** Antwort der Anmeldung. `twoFactorRedirect`: Das Backend verlangt den zweiten Faktor; eine Sitzung gibt es dann noch nicht. */
@Serializable
data class SignInResponse(val twoFactorRedirect: Boolean = false)

@Serializable
data class AuthUser(val name: String = "", val mussPasswortAendern: Boolean = false)

/** `GET /api/auth/get-session`: Sitzung und Person; ohne Sitzung antwortet der Server `null`. */
@Serializable
data class AuthSession(val user: AuthUser)

@Serializable
data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)

/** Die Regeln des Servers für ein neues Passwort, damit die App schon vor dem Senden zeigt, was fehlt. */
object PasswordRules {
    const val MIN_LENGTH = 12
    const val MAX_LENGTH = 128

    fun longEnough(password: String) = password.length >= MIN_LENGTH

    fun different(current: String, new: String) = current.isNotEmpty() && new != current

    fun valid(current: String, new: String) = longEnough(new) && different(current, new)

    /** `current` ist `null`, wenn die App das Startpasswort vom Login kennt: Dann prüft der Server, ob es sich unterscheidet. */
    fun canSave(current: String?, new: String) = if (current == null) longEnough(new) else valid(current, new)

    /** Text zu einem Fehlschlag von `change-password`. Der Server-Text ist englisch und wird nie angezeigt. */
    @StringRes
    fun errorText(failure: ApiResult.Failure): Int = when (failure.code) {
        "INVALID_PASSWORD" -> R.string.password_err_current
        "PASSWORT_UNVERAENDERT" -> R.string.password_err_same
        "PASSWORD_COMPROMISED" -> R.string.password_err_leaked
        "PASSWORD_TOO_SHORT" -> R.string.password_err_short
        "PASSWORD_TOO_LONG" -> R.string.password_err_long
        else -> failure.error.text
    }
}

/** Text zu einem Fehlschlag der Anmeldung. Das Backend unterscheidet die Gründe nicht (immer 401), die App auch nicht. */
@StringRes
fun signInErrorText(failure: ApiResult.Failure): Int =
    if (failure.error == ApiError.NOT_SIGNED_IN) R.string.login_failed else failure.error.text

/** Wartezeit für die Anzeige: bis 90 Sekunden in Sekunden, darüber in Minuten (aufgerundet). */
data class WaitTime(val amount: Int, val minutes: Boolean)

fun waitTime(seconds: Int): WaitTime =
    if (seconds <= 90) WaitTime(seconds.coerceAtLeast(1), minutes = false) else WaitTime((seconds + 59) / 60, minutes = true)
