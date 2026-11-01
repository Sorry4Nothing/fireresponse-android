package io.github.sorry4nothing.cevios.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.github.sorry4nothing.cevios.net.ApiClient
import kotlinx.coroutines.runBlocking

/**
 * Firebase erneuert das Token: Wer angemeldet ist, hinterlegt das neue beim Server (ohne Sitzung antwortet er mit 401, dann
 * holt der nächste Login das Token). Eingehende Pushes zeigt das System selbst, solange die App im Hintergrund ist; ist sie offen,
 * ruft Firebase nur [onMessageReceived] auf, und die App zeigt die Mitteilung selbst (ausser der betroffene Chat ist offen).
 */
class PushService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val type = message.data["typ"]
        val id = message.data["id"]
        if (OpenChat.hides(type, id)) return
        val notification = message.notification
        Notifications.show(applicationContext, notification?.title, notification?.body, notification?.channelId, type, id)
    }

    override fun onNewToken(token: String) {
        runBlocking { PushApi(ApiClient.create(applicationContext)).registerToken(token) }
    }
}
