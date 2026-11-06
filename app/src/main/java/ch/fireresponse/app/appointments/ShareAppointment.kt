package ch.fireresponse.app.appointments

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import ch.fireresponse.app.R
import ch.fireresponse.app.ui.OutlinedPillButton

/**
 * Der Text, den «Übung teilen» weitergibt: Titel, Tag und Zeit, Name der Einheit und, wenn es eines gibt, das Aufgebot. Nie
 * Namen von Kindern, Antworten oder die Kontaktperson: Das wären Daten anderer Personen, die der Empfänger nicht sehen soll.
 */
fun appointmentShareText(title: String, whenText: String?, squadName: String, callUp: String?, callUpLabel: String): String =
    listOfNotNull(
        title,
        whenText,
        squadName,
        callUp?.takeIf { it.isNotBlank() }?.let { "$callUpLabel: $it" },
    ).joinToString("\n")

/** Öffnet die Teilen-Auswahl des Geräts mit dem Text der Übung. */
@Composable
fun ShareSection(appointment: Appointment) {
    val context = LocalContext.current
    val timeText = listTimeText(appointment, context)
    val text = appointmentShareText(
        title = appointment.title,
        whenText = appointment.startDay()?.let { "${spokenDay(it)}, $timeText" },
        squadName = appointment.squad.name,
        callUp = appointment.callUp,
        callUpLabel = stringResource(R.string.fact_call_up),
    )
    val chooser = stringResource(R.string.share_chooser)
    OutlinedPillButton(
        stringResource(R.string.share_action),
        {
            val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_SUBJECT, appointment.title).putExtra(Intent.EXTRA_TEXT, text)
            try {
                context.startActivity(Intent.createChooser(send, chooser))
            } catch (e: ActivityNotFoundException) {
                // Keine App, die Text annimmt: Der Knopf tut dann nichts.
            }
        },
        Modifier.fillMaxWidth(),
        icon = R.drawable.ic_share,
    )
}
