package ch.fireresponse.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.PowerManager
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
import ch.fireresponse.app.images.RankImageApi
import ch.fireresponse.app.images.decodeBadge
import ch.fireresponse.app.images.squadLogoPath
import ch.fireresponse.app.net.ApiClient
import ch.fireresponse.app.net.ApiError
import ch.fireresponse.app.net.ApiResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

/** Ein Broadcast darf nur kurz laufen: Antwortet der Server in dieser Zeit nicht, bleibt das Widget, wie es ist. */
private const val FETCH_TIMEOUT_MS = 8_000L

/** Das Fenster der Liste in der App (Monate ab heute). */
private const val WINDOW_MONTHS = 6L

/** Die Logos haben nur kurz Zeit; danach steht das Kürzel da und die Übungen sind trotzdem aktuell. */
private const val LOGO_TIMEOUT_MS = 3_000L

/** Bis zu zwei Personen zeigt das Widget als Chip mit Namen, ab drei als Kreise (höchstens so viele; die Zahl «x von y» zählt alle). */
private const val NAME_CHIPS = 2
private const val MAX_DOTS = 5

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

/** Die Zeilen zeigen die Vornamen der eigenen Kinder, wie im Entwurf; die Meldungen (Abmeldung, Verbindung) nennen nichts. */
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
    suspend fun show(context: Context, appointments: List<Appointment>) {
        val rows = upcoming(appointments, ZonedDateTime.now(SWISS_ZONE))
        if (rows.isEmpty()) {
            message(context, R.string.widget_empty, shown = true)
        } else {
            push(context, rowsView(context, rows, logos(context, rows.map { it.squad.id }.distinct())), shown = true)
        }
    }

    /** Gezeichnete Logos und die Einheiten, die keines haben: nur im Arbeitsspeicher, solange der Prozess lebt. */
    private val logoFound = ConcurrentHashMap<String, Bitmap>()
    private val logoNone = ConcurrentHashMap.newKeySet<String>()

    /**
     * Die Logos der Einheiten, soweit es sie gibt: Sie ersetzen das Kürzel. Lädt ein Logo nicht (Netz, Zeit), bleibt das Kürzel und
     * die Übungen erscheinen trotzdem; ein 404 merkt sich der Prozess, damit nicht jede Aktualisierung neu fragt.
     */
    private suspend fun logos(context: Context, squadIds: List<String>): Map<String, Bitmap> {
        val missing = squadIds.filter { !logoFound.containsKey(it) && it !in logoNone }
        if (missing.isNotEmpty()) {
            withTimeoutOrNull(LOGO_TIMEOUT_MS) {
                val api = RankImageApi(ApiClient.create(context))
                missing.forEach { id ->
                    when (val result = api.load(squadLogoPath(id))) {
                        is ApiResult.Ok -> decodeBadge(result.value)?.let { logoFound[id] = it } ?: logoNone.add(id)
                        is ApiResult.Failure -> if (result.error == ApiError.NOT_FOUND) logoNone.add(id)
                    }
                }
            }
        }
        return squadIds.mapNotNull { id -> logoFound[id]?.let { id to it } }.toMap()
    }

    fun showSignedOut(context: Context) = message(context, R.string.widget_signed_out, shown = false)

    /** Ohne Verbindung bleibt, was schon da ist; nur ein Widget, das noch nichts zeigte, sagt es. */
    fun showOfflineIfNothingShown(context: Context) {
        if (!context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(SHOWN, false)) {
            // Im Energiesparmodus sperrt das System dem Widget im Hintergrund das Netz: Das ist keine fehlende Verbindung.
            val powerSave = context.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true
            message(context, if (powerSave) R.string.widget_power_save else R.string.widget_offline, shown = false)
        }
    }

    /**
     * Beim Start der App (Vordergrund: das Netz ist erlaubt, auch im Energiesparmodus) holt das Widget seine Übungen, wenn eines liegt.
     * Das gilt auch vor dem Entsperren: Es zeigt nicht mehr, als der Startbildschirm ohnehin zeigt.
     */
    suspend fun refreshIfPlaced(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        if (manager.getAppWidgetIds(ComponentName(context, AppointmentWidgetProvider::class.java)).isNotEmpty()) refresh(context)
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
    internal fun rowsView(context: Context, appointments: List<Appointment>, logos: Map<String, Bitmap> = emptyMap()): RemoteViews {
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
                fill(context, views, row, appointment, logos[appointment.squad.id])
            }
        }
        return views
    }

    private fun fill(context: Context, views: RemoteViews, row: Row, appointment: Appointment, logo: Bitmap?) {
        val today = LocalDate.now(SWISS_ZONE)
        val day = appointment.startDay() ?: today
        val squad = appointment.squad
        views.setViewVisibility(row.row, View.VISIBLE)
        views.setTextViewText(row.title, appointment.title)
        // Das Logo ersetzt das Kürzel der Einheit, wenn es eines gibt.
        views.setViewVisibility(row.badge, if (logo == null) View.VISIBLE else View.GONE)
        views.setViewVisibility(row.logo, if (logo == null) View.GONE else View.VISIBLE)
        views.setTextViewText(row.badge, squad.type.uppercase())
        if (logo != null) views.setImageViewBitmap(row.logo, logo)
        views.setTextViewText(row.`when`, "${weekdayShort(day)} ${day.dayOfMonth}.${day.monthValue}. · ${startText(context, appointment)}")

        val opensOn = appointment.registrationOpensOn(today)
        val people = if (opensOn == null) appointment.people(context.getString(R.string.message_you)) else emptyList()
        val answers = appointment.answers()
        val count = when {
            opensOn != null -> "ab ${opensOn.dayOfMonth}.${opensOn.monthValue}."
            answers.total > 0 -> context.getString(R.string.widget_count, answers.given, answers.total)
            else -> null
        }
        views.setViewVisibility(row.count, if (count == null) View.GONE else View.VISIBLE)
        count?.let { views.setTextViewText(row.count, it) }
        chips(context, views, row, people)

        // Die Zeile ist ein Element mit einem ganzen Satz; die Teile darin sind für den Screenreader ausgeblendet.
        val spoken = listOfNotNull(
            spokenDay(day),
            listTimeText(appointment, context),
            appointment.title,
            context.getString(R.string.widget_unit, squad.name),
            opensOn?.let { context.getString(R.string.registration_from, "${it.dayOfMonth}.${it.monthValue}.") },
            people.takeIf { it.isNotEmpty() }?.joinToString(", ") { "${it.name}: ${context.getString(look(it.stand).spoken)}" },
        ).joinToString(", ")
        views.setContentDescription(row.row, "$spoken. ${context.getString(R.string.widget_open_appointment)}")
        views.setOnClickPendingIntent(row.row, openIntent(context, appointment.id))
    }

    /** Je Person ein Chip mit Namen (bis zwei) oder ein Kreis mit Initiale und Zeichen für den Stand (ab drei). */
    private fun chips(context: Context, views: RemoteViews, row: Row, people: List<Person>) {
        views.removeAllViews(row.chips)
        views.setViewVisibility(row.chips, if (people.isEmpty()) View.GONE else View.VISIBLE)
        val names = people.size <= NAME_CHIPS
        people.take(MAX_DOTS).forEach { person ->
            val look = look(person.stand)
            val fg = context.getColor(look.fg)
            val chip = RemoteViews(context.packageName, if (names) R.layout.widget_chip_name else R.layout.widget_chip_dot)
            if (names) {
                chip.setInt(R.id.chip_root, "setBackgroundResource", look.chip)
                chip.setImageViewResource(R.id.chip_icon, look.icon)
                chip.setInt(R.id.chip_icon, "setColorFilter", fg)
                chip.setTextViewText(R.id.chip_text, person.name)
                chip.setTextColor(R.id.chip_text, fg)
            } else {
                chip.setInt(R.id.dot_initial, "setBackgroundResource", look.dot)
                chip.setTextViewText(R.id.dot_initial, person.name.take(1).uppercase())
                chip.setTextColor(R.id.dot_initial, fg)
                chip.setInt(R.id.dot_badge, "setBackgroundResource", look.badge)
                chip.setImageViewResource(R.id.dot_badge, look.icon)
                chip.setInt(R.id.dot_badge, "setColorFilter", context.getColor(R.color.widget_background))
            }
            views.addView(row.chips, chip)
        }
    }

    /** Alles, was zu einem Stand gehört: Flächen, Farbe, Zeichen und das Wort für den Screenreader. */
    private class Look(val chip: Int, val dot: Int, val badge: Int, val fg: Int, val icon: Int, val spoken: Int)

    private fun look(stand: Stand) = when (stand) {
        Stand.YES -> Look(R.drawable.widget_chip_yes, R.drawable.widget_dot_yes, R.drawable.widget_badge_yes, R.color.widget_yes_fg, R.drawable.ic_check, R.string.widget_person_yes)
        Stand.NO -> Look(R.drawable.widget_chip_no, R.drawable.widget_dot_no, R.drawable.widget_badge_no, R.color.widget_no_fg, R.drawable.ic_cross, R.string.widget_person_no)
        Stand.OPEN -> Look(R.drawable.widget_chip_open, R.drawable.widget_dot_open, R.drawable.widget_badge_open, R.color.widget_open_fg, R.drawable.ic_question, R.string.widget_person_open)
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

    private class Row(val row: Int, val title: Int, val badge: Int, val logo: Int, val `when`: Int, val count: Int, val chips: Int)

    private val ROWS = listOf(
        Row(R.id.row1, R.id.row1_title, R.id.row1_badge, R.id.row1_logo, R.id.row1_when, R.id.row1_count, R.id.row1_chips),
        Row(R.id.row2, R.id.row2_title, R.id.row2_badge, R.id.row2_logo, R.id.row2_when, R.id.row2_count, R.id.row2_chips),
    )
}
