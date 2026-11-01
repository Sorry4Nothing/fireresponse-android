package io.github.sorry4nothing.cevios.push

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import io.github.sorry4nothing.cevios.MainActivity
import io.github.sorry4nothing.cevios.R

object Notifications {
    /** Die Kanäle, die der Server in jedem Push nennt: Ein Kanal, den das Gerät nicht kennt, zeigt den Push nicht. */
    const val CHAT_CHANNEL = "chat"
    const val PROGRAMS_CHANNEL = "programme"
    const val MESSAGES_CHANNEL = "mitteilungen"

    private const val PREFS = "push"
    private const val ASKED = "permission_asked"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(CHAT_CHANNEL, context.getString(R.string.channel_chat), NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(PROGRAMS_CHANNEL, context.getString(R.string.channel_programs), NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(MESSAGES_CHANNEL, context.getString(R.string.channel_messages), NotificationManager.IMPORTANCE_DEFAULT),
            ),
        )
    }

    private val CHANNELS = setOf(CHAT_CHANNEL, PROGRAMS_CHANNEL, MESSAGES_CHANNEL)

    /** Der Kanal aus dem Push, wenn die App ihn kennt, sonst «Mitteilungen». Der Inhalt kommt von aussen. */
    fun channelOf(channel: String?): String = channel?.takeIf { it in CHANNELS } ?: MESSAGES_CHANNEL

    /**
     * Zeigt einen Push selbst: Firebase tut es nur, solange die App im Hintergrund ist. Titel und Text liefert der Server (nie
     * Namen). Tippen öffnet die App mit `typ` und `id`, wie bei einer Mitteilung des Systems. Ohne Erlaubnis geschieht nichts.
     */
    @SuppressLint("MissingPermission") // geprüft: areNotificationsEnabled() schliesst die fehlende Erlaubnis ein
    fun show(context: Context, title: String?, body: String?, channel: String?, type: String?, id: String?) {
        if (title.isNullOrBlank() && body.isNullOrBlank()) return
        if (!enabled(context)) return
        createChannels(context)
        val open = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .apply {
                type?.let { putExtra("typ", it) }
                id?.let { putExtra("id", it) }
            }
        // Ein Chat ersetzt seine letzte Mitteilung; alles andere steht für sich.
        val notificationId = if (type != null && !id.isNullOrEmpty()) "$type$id".hashCode() else System.currentTimeMillis().toInt()
        val tap = PendingIntent.getActivity(context, notificationId, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(context, channelOf(channel))
            .setSmallIcon(R.drawable.ic_stat_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(tap)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    /** Wahr, wenn das Gerät der App Mitteilungen zeigen lässt (Erlaubnis ab Android 13 und Schalter in den Einstellungen). */
    fun enabled(context: Context): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** Wahr genau einmal: wenn die Erlaubnis fehlt (ab Android 13) und die App noch nie gefragt hat. */
    fun shouldAskOnce(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || enabled(context)) return false
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(ASKED, false)) return false
        prefs.edit { putBoolean(ASKED, true) }
        return true
    }
}

/** Der Chat, den die Person gerade offen hat; `null`, wenn keiner offen ist. Ein Push zu diesem Chat zeigt nichts. */
object OpenChat {
    @Volatile
    var id: String? = null

    /** Ob ein Push mit `typ` und `id` nichts zeigen soll, weil die Person genau diesen Chat liest. */
    fun hides(type: String?, pushId: String?, open: String? = id): Boolean = type == "chat" && pushId != null && pushId == open
}
