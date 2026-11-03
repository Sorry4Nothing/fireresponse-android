package ch.fireresponse.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.StringRes
import androidx.core.content.edit
import androidx.core.net.toUri
import ch.fireresponse.app.MainActivity
import ch.fireresponse.app.R
import ch.fireresponse.app.appointments.Appointment
import ch.fireresponse.app.appointments.AppointmentsApi
import ch.fireresponse.app.appointments.SWISS_ZONE
import ch.fireresponse.app.appointments.Timing
import ch.fireresponse.app.appointments.listTimeText
import ch.fireresponse.app.appointments.registrationOpensOn
import ch.fireresponse.app.appointments.spokenDay
import ch.fireresponse.app.appointments.startDay
import ch.fireresponse.app.appointments.time
import ch.fireresponse.app.appointments.weekdayShort
import ch.fireresponse.app.net.ApiClient
import ch.fireresponse.app.net.ApiError
import ch.fireresponse.app.net.ApiResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/** Ein Broadcast darf nur kurz laufen: Antwortet der Server in dieser Zeit nicht, bleibt das Widget, wie es ist. */
private const val FETCH_TIMEOUT_MS = 8_000L

/** Das Fenster der Liste in der App (Monate ab heute). */
private const val WINDOW_MONTHS = 6L

/**
 * Widget «Nächste Übungen» (4 × 2 Felder): die nächsten zwei Übungen mit dem Stand der Antwort, nur zum Ansehen. Es aktualisiert
 * sich alle 30 Minuten (Systemvorgabe), wenn die App ihre Liste lädt (`MainActivity`) und beim Abmelden.
 */
class AppointmentWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withTimeout(FETCH_TIMEOUT_MS) { AppointmentWidget.refresh(context.applicationContext) }
            } catch (e: Exception) {
                AppointmentWidget.showOfflineIfNothingShown(context.applicationContext)
            } finally {
                pending.finish()
            }
        }
    }

    /** Das letzte Widget ist weg: Der Merker, dass etwas gezeigt wurde, gilt nicht mehr. */
    override fun onDisabled(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { clear() }
    }
}

private const val PREFS = "widget"
private const val SHOWN = "shown"

private val START = DateTimeFormatter.ofPattern("HH:mm")

/** Bei den Meldungen und Zeilen des Widgets bleiben Namen weg: Es steht auf dem Startbildschirm. */
object AppointmentWidget {
    /** Holt die Liste selbst (eigene Sitzung aus dem Keystore) und zeigt sie. */
    suspend fun refresh(context: Context) {
        val client = ApiClient.create(context)
        if (!client.hasSession()) return showSignedOut(context)
        val today = LocalDate.now(SWISS_ZONE)
        when (val result = AppointmentsApi(client).list(today, today.plusMonths(WINDOW_MONTHS))) {
            is ApiResult.Ok -> show(context, result.value.appointments)
            is ApiResult.Failure ->
                if (result.error == ApiError.NOT_SIGNED_IN) showSignedOut(context) else showOfflineIfNothingShown(context)
        }
    }

    /** Zeigt die nächsten zwei Übungen aus der Liste der API; ohne eine steht «Keine Übungen in Sicht». */
    fun show(context: Context, appointments: List<Appointment>) {
        val rows = upcoming(appointments, ZonedDateTime.now(SWISS_ZONE))
        if (rows.isEmpty()) message(context, R.string.widget_empty, shown = true) else push(context, rowsView(context, rows), shown = true)
    }

    fun showSignedOut(context: Context) = message(context, R.string.widget_signed_out, shown = false)

    /** Ohne Verbindung bleibt, was schon da ist; nur ein Widget, das noch nichts zeigte, sagt es. */
    fun showOfflineIfNothingShown(context: Context) {
        if (!context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(SHOWN, false)) {
            message(context, R.string.widget_offline, shown = false)
        }
    }

    private fun message(context: Context, @StringRes text: Int, shown: Boolean) {
        val views = RemoteViews(context.packageName, R.layout.widget_appointments)
        ROWS.forEach { views.setViewVisibility(it.row, View.GONE) }
        views.setViewVisibility(R.id.divider, View.GONE)
        views.setViewVisibility(R.id.hint, View.GONE)
        views.setViewVisibility(R.id.message, View.VISIBLE)
        views.setTextViewText(R.id.message, context.getString(text))
        val open = openIntent(context, null)
        views.setOnClickPendingIntent(R.id.message, open)
        views.setOnClickPendingIntent(R.id.widget_root, open)
        push(context, views, shown)
    }

    private fun push(context: Context, views: RemoteViews, shown: Boolean) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, AppointmentWidgetProvider::class.java))
        if (ids.isEmpty()) return
        manager.updateAppWidget(ids, views)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putBoolean(SHOWN, shown) }
    }

    /** Die Zeilen des Widgets; auch für die Vorschau in der Debug-Galerie. */
    internal fun rowsView(context: Context, appointments: List<Appointment>): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_appointments)
        views.setViewVisibility(R.id.message, View.GONE)
        views.setViewVisibility(R.id.divider, View.VISIBLE)
        // Mit nur einer Übung sagt ein ruhiger Satz darunter, dass nichts mehr folgt: So wirkt die Fläche nicht leer.
        views.setViewVisibility(R.id.hint, if (appointments.size == 1) View.VISIBLE else View.GONE)
        views.setOnClickPendingIntent(R.id.hint, openIntent(context, null))
        views.setOnClickPendingIntent(R.id.widget_root, openIntent(context, null))
        ROWS.forEachIndexed { index, row ->
            val appointment = appointments.getOrNull(index)
            if (appointment == null) {
                views.setViewVisibility(row.row, View.GONE)
            } else {
                fill(context, views, row, appointment)
            }
        }
        return views
    }

    private fun fill(context: Context, views: RemoteViews, row: Row, appointment: Appointment) {
        val day = appointment.startDay() ?: LocalDate.now(SWISS_ZONE)
        val time = listTimeText(appointment, context)
        val status = status(context, appointment)
        views.setViewVisibility(row.row, View.VISIBLE)
        views.setTextViewText(row.title, appointment.title)
        views.setTextViewText(row.weekday, weekdayShort(day))
        views.setTextViewText(row.date, "${day.dayOfMonth}.${day.monthValue}.")
        views.setTextViewText(row.time, startText(context, appointment))
        val statusVisibility = if (status == null) View.GONE else View.VISIBLE
        views.setViewVisibility(row.statusIcon, statusVisibility)
        views.setViewVisibility(row.statusText, statusVisibility)
        if (status != null) {
            views.setTextViewText(row.statusText, status.text)
            views.setTextColor(row.statusText, status.color)
            views.setImageViewResource(row.statusIcon, status.icon)
            views.setInt(row.statusIcon, "setColorFilter", status.color)
        }
        // Die Zeile ist ein Element mit einem ganzen Satz; die Teile darin sind für den Screenreader ausgeblendet.
        val spoken = listOfNotNull(spokenDay(day), time, appointment.title, status?.spoken).joinToString(", ")
        views.setContentDescription(row.row, "$spoken. ${context.getString(R.string.widget_open_appointment)}")
        views.setOnClickPendingIntent(row.row, openIntent(context, appointment.id))
    }

    private class Status(val text: String, val spoken: String, val icon: Int, val color: Int)

    /**
     * Der Stand mit Icon und kurzem Wort, ohne Namen: «Beantwortet», «Offen», «1 von 2» oder «ab 5.12.». Der Screenreader hört die
     * ganzen Wörter («Antwort offen», «1 von 2 beantwortet», «Anmeldung ab 5.12.»).
     */
    private fun status(context: Context, appointment: Appointment): Status? {
        val muted = context.getColor(R.color.widget_text_muted)
        appointment.registrationOpensOn(LocalDate.now(SWISS_ZONE))?.let {
            val from = "${it.dayOfMonth}.${it.monthValue}."
            return Status("ab $from", context.getString(R.string.registration_from, from), R.drawable.ic_clock, muted)
        }
        val answers = appointment.answers()
        return when {
            answers.total == 0 -> null
            answers.done -> context.getString(R.string.widget_answered).let { Status(it, it, R.drawable.ic_check, context.getColor(R.color.widget_success)) }
            answers.open -> Status(context.getString(R.string.widget_status_open), context.getString(R.string.widget_answer_open), R.drawable.ic_question, muted)
            else -> Status(
                context.getString(R.string.widget_status_partly, answers.given, answers.total),
                context.getString(R.string.widget_answered_partly, answers.given, answers.total),
                R.drawable.ic_question,
                muted,
            )
        }
    }

    /** Nur der Beginn («14:00»): So passt die Zeile ins schmale Widget; der Screenreader hört die ganze Zeit. */
    private fun startText(context: Context, appointment: Appointment): String = when (val time = appointment.time()) {
        is Timing.Timed -> START.format(time.start)
        is Timing.AllDay -> context.getString(R.string.time_list_all_day)
        null -> context.getString(R.string.time_pending)
    }

    /**
     * Öffnet die App, mit einer Übung auf demselben Weg wie ein Push (`typ`, `id`, siehe `PushTarget`). Ändern lässt sich dabei
     * nichts: Das Widget antwortet nie. Eine eigene Anforderung je Übung, damit die Intents nicht ineinander übergehen.
     */
    private fun openIntent(context: Context, appointmentId: String?): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (appointmentId != null) {
            intent.putExtra("typ", "programm").putExtra("id", appointmentId).data = "widget://appointment/$appointmentId".toUri()
        }
        return PendingIntent.getActivity(
            context,
            appointmentId?.hashCode() ?: 0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private class Row(val row: Int, val weekday: Int, val date: Int, val title: Int, val time: Int, val statusIcon: Int, val statusText: Int)

    private val ROWS = listOf(
        Row(R.id.row1, R.id.row1_weekday, R.id.row1_date, R.id.row1_title, R.id.row1_time, R.id.row1_status_icon, R.id.row1_status),
        Row(R.id.row2, R.id.row2_weekday, R.id.row2_date, R.id.row2_title, R.id.row2_time, R.id.row2_status_icon, R.id.row2_status),
    )
}
