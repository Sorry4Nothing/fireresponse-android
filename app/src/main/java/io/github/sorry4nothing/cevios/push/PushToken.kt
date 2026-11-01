package io.github.sorry4nothing.cevios.push

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object PushToken {
    /**
     * Das FCM-Token dieses Geräts, oder `null` ohne Firebase-Konfiguration (CI und Build ohne `google-services.json`) und
     * wenn Google keines liefert. Erst nach dem Anmelden aufrufen: Vorher holt die App kein Token.
     */
    suspend fun fetch(context: Context): String? {
        if (FirebaseApp.getApps(context).isEmpty()) return null
        val messaging = FirebaseMessaging.getInstance()
        // Damit Firebase eine Erneuerung des Tokens meldet (PushService.onNewToken).
        messaging.isAutoInitEnabled = true
        return suspendCancellableCoroutine { continuation ->
            messaging.token.addOnCompleteListener { task -> continuation.resume(if (task.isSuccessful) task.result else null) }
        }
    }
}
