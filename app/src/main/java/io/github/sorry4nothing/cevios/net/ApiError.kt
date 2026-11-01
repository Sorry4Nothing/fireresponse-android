package io.github.sorry4nothing.cevios.net

import androidx.annotation.StringRes
import io.github.sorry4nothing.cevios.R

/** Einheitliche Fehler der API mit dem Text, den die App anzeigt. Der Server-Text wird nie angezeigt. */
enum class ApiError(@StringRes val text: Int) {
    NOT_SIGNED_IN(R.string.error_not_signed_in),
    FORBIDDEN(R.string.error_forbidden),
    NOT_FOUND(R.string.error_not_found),
    APP_OUTDATED(R.string.error_app_outdated),
    TOO_MANY_REQUESTS(R.string.error_too_many_requests),
    REGISTRATION_CLOSED(R.string.error_registration_closed),

    /** 409 `PROGRAMM_BEGONNEN`: Die Leitung kann sich nach dem Beginn nicht mehr ändern (abgesagte Programme gehen noch). */
    PROGRAM_STARTED(R.string.error_program_started),

    /** 409 `ANWESENHEIT_GESCHLOSSEN`: Erfassen geht ab Beginn des Programms und bis 14 Tage danach. */

    /** 404 beim Einzelchat: Der Server sagt nicht, ob die Stufe, das Elternteil oder das Recht fehlt. */
    DIRECT_CHAT_UNAVAILABLE(R.string.error_direct_chat_unavailable),

    /** 429 beim Senden einer Nachricht: höchstens 20 pro Minute. */
    MESSAGE_TOO_FAST(R.string.error_message_too_fast),
    NETWORK(R.string.error_network),
    UNKNOWN(R.string.error_unknown);

    companion object {
        /** `code` ist der Fehlercode des Servers (bei 409 und 400); er unterscheidet dort die Fälle. */
        fun from(status: Int, code: String? = null): ApiError = when (status) {
            401 -> NOT_SIGNED_IN
            403 -> FORBIDDEN
            404 -> NOT_FOUND
            409 -> when (code) {
                "PROGRAMM_BEGONNEN" -> PROGRAM_STARTED
                else -> REGISTRATION_CLOSED // sonst PROGRAMM_GESCHLOSSEN
            }
            426 -> APP_OUTDATED
            429 -> TOO_MANY_REQUESTS
            else -> UNKNOWN
        }

        fun fromStatus(status: Int): ApiError = from(status)
    }
}

sealed interface ApiResult<out T> {
    data class Ok<out T>(val value: T) : ApiResult<T>

    /**
     * `code` ist der Fehlercode des Servers und nur bei 400 und 409 gefüllt (z. B. `PASSWORD_COMPROMISED`).
     * `retryAfterSeconds` nennt bei 429 die Wartezeit aus dem Header `X-Retry-After`, wenn der Server sie schickt.
     */
    data class Failure(val error: ApiError, val code: String? = null, val retryAfterSeconds: Int? = null) : ApiResult<Nothing>
}

/** Gibt der Fehler an dieser Stelle einen genaueren Text: aus `from` wird `to`, alles andere bleibt (auch `code` und Wartezeit). */
fun <T> ApiResult<T>.replacingError(from: ApiError, to: ApiError): ApiResult<T> =
    if (this is ApiResult.Failure && error == from) copy(error = to) else this
