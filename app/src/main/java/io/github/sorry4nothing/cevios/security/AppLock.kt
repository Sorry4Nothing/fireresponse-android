package io.github.sorry4nothing.cevios.security

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/** Was das Anmelden vom Entsperren der App wissen muss; so bleibt es ohne Android-Keystore prüfbar. */
interface LockState {
    val enabled: Boolean
    fun disable()
}

/** So lange darf die App im Hintergrund sein, bevor sie sich wieder sperrt (kurze Wechsel wie Bildauswahl oder Berechtigungen nicht). */
const val RELOCK_AFTER_MS = 30_000L

/** Ob die App nach `awayMs` Millisekunden im Hintergrund wieder gesperrt sein soll; `awayMs` ist `null`, wenn sie nicht im Hintergrund war. */
fun shouldRelock(awayMs: Long?): Boolean = awayMs != null && awayMs >= RELOCK_AFTER_MS

/**
 * Entsperren der App mit Fingerabdruck oder Gesicht. Die Sitzung bleibt, wie sie ist (verschlüsselt im Keystore); die App
 * zeigt nach dem Start aber nichts und fragt den Server nichts, bis die Person sich ausgewiesen hat.
 *
 * Der Ausweis ist ein Schlüssel im Android Keystore, der nur nach einer starken Biometrie benutzbar ist, bei jeder Benutzung
 * neu (`BiometricPrompt` mit `CryptoObject`): Die App kann ihn nicht durch ein blosses «ja» ersetzen. Kommt ein Finger oder
 * Gesicht hinzu, wird der Schlüssel ungültig; dann folgt die Anmeldung mit Passwort.
 * Das ist eine Sperre der App, keine zusätzliche Verschlüsselung der Sitzung: Das Sitzungs-Cookie ändert sich bei Antworten des
 * Servers, und das Neuverschlüsseln dürfte nicht jedes Mal nach einem Finger fragen.
 */
class AppLock(context: Context) : LockState {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("sicherheit", Context.MODE_PRIVATE)

    /** Hat das Gerät eine starke Biometrie, die eingerichtet ist? Sonst bietet die App nichts an. */
    fun isAvailable(): Boolean =
        BiometricManager.from(appContext).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS

    override val enabled: Boolean
        get() = prefs.getBoolean(ENABLED, false) && keyStore().containsAlias(ALIAS)

    /** Legt einen neuen Schlüssel an; ein alter ist damit weg. Eingeschaltet ist es erst mit [markEnabled]. */
    @Throws(GeneralSecurityException::class)
    fun createKey() {
        disable()
        val builder = KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
        } else {
            @Suppress("DEPRECATION")
            builder.setUserAuthenticationValidityDurationSeconds(-1)
        }
        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).apply { init(builder.build()) }.generateKey()
    }

    fun markEnabled() = prefs.edit().putBoolean(ENABLED, true).apply()

    /**
     * Ein Cipher für eine Prüfung; erst `BiometricPrompt` macht ihn benutzbar. Wirft [KeyPermanentlyInvalidatedException], wenn
     * die Biometrie sich geändert hat oder der Schlüssel fehlt.
     */
    @Throws(GeneralSecurityException::class)
    fun newCipher(): Cipher {
        val key = keyStore().getKey(ALIAS, null) as? SecretKey ?: throw KeyPermanentlyInvalidatedException()
        return Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key) }
    }

    override fun disable() {
        prefs.edit().remove(ENABLED).apply()
        keyStore().takeIf { it.containsAlias(ALIAS) }?.deleteEntry(ALIAS)
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "cevi_entsperren"
        const val ENABLED = "entsperren"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
