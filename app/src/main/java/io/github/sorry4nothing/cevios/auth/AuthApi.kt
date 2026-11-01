package io.github.sorry4nothing.cevios.auth

import io.github.sorry4nothing.cevios.chat.Profile
import io.github.sorry4nothing.cevios.net.ApiClient
import io.github.sorry4nothing.cevios.net.ApiResult

class AuthApi(private val api: ApiClient) {
    /** Die Sitzung und ihre Person, oder `null` ohne gültige Sitzung. Verlängert die Sitzung, wenn es fällig ist. */
    suspend fun session(): ApiResult<AuthSession?> = api.get("/api/auth/get-session")

    suspend fun signIn(username: String, password: String): ApiResult<SignInResponse> =
        api.post("/api/auth/sign-in/username", SignInRequest(username, password))

    /** Antwortet auch ohne Sitzung mit 200. Das Cookie löscht der Server per `Set-Cookie`. */
    suspend fun signOut(): ApiResult<Unit> = api.postWithoutBody("/api/auth/sign-out")

    /** Beendet auch alle anderen Sitzungen der Person; das neue Cookie dieses Geräts nimmt der Zugang selbst. */
    suspend fun changePassword(current: String, new: String): ApiResult<Unit> =
        api.postAction("/api/auth/change-password", ChangePasswordRequest(current, new))

    suspend fun acceptPrivacy(): ApiResult<Unit> = api.postWithoutBody("/api/v1/ich/datenschutz")

    /** Bleibt auch bei offenem Pflichtwechsel des Passworts erreichbar (alle anderen Routen antworten dann 403). */
    suspend fun profile(): ApiResult<Profile> = api.get("/api/v1/ich")

    /** Setzt das eigene Profilbild (JPEG, höchstens 5 MiB); ein vorhandenes wird ersetzt. */
    suspend fun uploadImage(jpeg: ByteArray): ApiResult<Unit> = api.putBytes("/api/v1/ich/bild", jpeg, "image/jpeg")

    /** Löscht das eigene Profilbild; ohne Bild ebenfalls 204. */
    suspend fun removeImage(): ApiResult<Unit> = api.delete("/api/v1/ich/bild")

    /** Adresse der Datenschutzerklärung auf dem Backend. */
    fun privacyUrl(): String = api.pageUrl("/datenschutz")

    /** Adresse der Support-Seite auf dem Backend. */
    fun supportUrl(): String = api.pageUrl("/support")

    /** Adresse der Seite, die erklärt, wie das Konto gelöscht wird (in der App gibt es kein «Konto löschen»). */
    fun deleteAccountUrl(): String = api.pageUrl("/konto-loeschen")
}
