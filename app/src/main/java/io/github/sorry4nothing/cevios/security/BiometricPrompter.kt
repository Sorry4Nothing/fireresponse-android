package io.github.sorry4nothing.cevios.security

import android.security.keystore.KeyPermanentlyInvalidatedException
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import io.github.sorry4nothing.cevios.R
import java.security.GeneralSecurityException
import javax.crypto.Cipher

/** Wie eine Abfrage der Biometrie ausging. */
enum class LockOutcome {
    /** Die Person hat sich ausgewiesen. */
    Success,

    /** Abgebrochen (Knopf, zurück, App im Hintergrund): Nichts ist schiefgegangen, die App bleibt gesperrt. */
    Declined,

    /** Der Schlüssel gilt nicht mehr (neue Biometrie): Die Person meldet sich mit dem Passwort an. */
    Invalidated,

    /** Das Entsperren hat nicht geklappt (zu viele Versuche, Gerätefehler). */
    Failed,
}

/** Der Fehlercode von `BiometricPrompt` als Ausgang: Abbruch durch die Person ist kein Fehler. */
internal fun outcomeOfError(code: Int): LockOutcome = when (code) {
    BiometricPrompt.ERROR_NEGATIVE_BUTTON, BiometricPrompt.ERROR_USER_CANCELED, BiometricPrompt.ERROR_CANCELED -> LockOutcome.Declined
    else -> LockOutcome.Failed
}

/** Zeigt die Abfrage von Fingerabdruck oder Gesicht; braucht die Activity, darum nicht im ViewModel. */
class BiometricPrompter(private val activity: FragmentActivity, private val lock: AppLock) {
    /** Entsperrt die App: Der Schlüssel muss gelten und die Biometrie ihn freigeben. */
    fun unlock(onOutcome: (LockOutcome) -> Unit) {
        val cipher = try {
            lock.newCipher()
        } catch (e: KeyPermanentlyInvalidatedException) {
            return onOutcome(LockOutcome.Invalidated)
        } catch (e: GeneralSecurityException) {
            return onOutcome(LockOutcome.Failed)
        }
        prompt(cipher, R.string.lock_prompt_title, onOutcome)
    }

    /** Schaltet das Entsperren ein: neuer Schlüssel, einmal ausweisen; erst dann gilt es als eingeschaltet. */
    fun enable(onOutcome: (LockOutcome) -> Unit) {
        val cipher = try {
            lock.createKey()
            lock.newCipher()
        } catch (e: GeneralSecurityException) {
            lock.disable()
            return onOutcome(LockOutcome.Failed)
        }
        prompt(cipher, R.string.lock_enable_title) { outcome ->
            if (outcome == LockOutcome.Success) lock.markEnabled() else lock.disable()
            onOutcome(outcome)
        }
    }

    private fun prompt(cipher: Cipher, title: Int, onOutcome: (LockOutcome) -> Unit) {
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                // Erst der benutzte Schlüssel beweist es: Schlägt dieser Schritt fehl, gilt das Entsperren als nicht geklappt.
                val proved = try {
                    result.cryptoObject?.cipher?.doFinal(PROOF) != null
                } catch (e: GeneralSecurityException) {
                    false
                }
                onOutcome(if (proved) LockOutcome.Success else LockOutcome.Failed)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onOutcome(outcomeOfError(errorCode))
        }
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(title))
            .setNegativeButtonText(activity.getString(R.string.lock_cancel))
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setConfirmationRequired(false)
            .build()
        BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback)
            .authenticate(info, BiometricPrompt.CryptoObject(cipher))
    }

    private companion object {
        val PROOF = byteArrayOf(1)
    }
}
