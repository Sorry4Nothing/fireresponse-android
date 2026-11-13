package ch.fireresponse.app.appointments

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.fireresponse.app.R
import ch.fireresponse.app.ui.Avatar
import ch.fireresponse.app.ui.ChipKind
import ch.fireresponse.app.ui.OutlinedPillButton
import ch.fireresponse.app.ui.PrimaryButton
import ch.fireresponse.app.ui.StatusChip
import ch.fireresponse.app.ui.theme.fire

/** «Lea M.»: Vorname und Anfangsbuchstabe des Nachnamens, wie die Plätze es zeigen. */
fun shortName(fullName: String): String {
    val words = fullName.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return if (words.size < 2) fullName.trim() else "${words.first()} ${words.last().first()}."
}

/** Der Text des Etiketts: «Noch 2 Plätze frei» oder «Voll». */
@Composable
fun placesLabel(places: LeaderPlaces): String =
    if (places.full) stringResource(R.string.places_full) else pluralStringResource(R.plurals.places_free, places.free, places.free)

/** Was der Screenreader sagt: bei «Voll» dazu, dass die Warteliste offen ist. */
@Composable
fun placesSpoken(places: LeaderPlaces): String =
    if (places.full) stringResource(R.string.places_full_spoken) else placesLabel(places)

/** Das Etikett «Noch 2 Plätze frei» oder «Voll» (mit Schloss) für Karte und Detail. */
@Composable
fun PlacesChip(places: LeaderPlaces, modifier: Modifier = Modifier) {
    StatusChip(if (places.full) ChipKind.FULL else ChipKind.FREE, placesLabel(places), modifier)
}

private const val PLACES_PER_ROW = 4

/**
 * Die Plätze der Leitenden als Karte: «2 von 4 belegt», das Etikett und darunter jeder Platz als Kreis, belegt mit Profilbild und
 * Name («Lea M.»), frei gestrichelt. `taken` sind die Leitenden mit Zusage aus der Übersicht der Anmeldungen; `null` oder zu wenige,
 * solange sie nicht geladen sind: Dann steht ein belegter Kreis ohne Namen da.
 */
@Composable
fun PlacesCard(places: LeaderPlaces, taken: List<LeaderRegistration>?, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val takenText = stringResource(R.string.places_taken, places.taken, places.max)
    val names = taken.orEmpty().take(places.taken)
    val slots = places.max.coerceAtLeast(0)
    val takenSlots = places.taken.coerceIn(0, slots)
    val title = stringResource(R.string.places_title)
    val freeWord = stringResource(R.string.places_slot_free_spoken)
    val spoken = buildString {
        append(title).append(", ").append(takenText).append(", ").append(placesSpoken(places)).append(". ")
        append((0 until takenSlots).joinToString(", ") { names.getOrNull(it)?.name ?: title })
        if (slots > takenSlots) append(", ").append((takenSlots until slots).joinToString(", ") { freeWord })
    }
    Column(
        modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainerLow, MaterialTheme.shapes.medium)
            .border(1.dp, scheme.outlineVariant, MaterialTheme.shapes.medium)
            .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 14.dp)
            .semantics(mergeDescendants = true) { contentDescription = spoken },
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(painterResource(R.drawable.ic_group), contentDescription = null, tint = MaterialTheme.fire.accent)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                Text(takenText, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold))
            }
            PlacesChip(places)
        }
        (0 until slots).chunked(PLACES_PER_ROW).forEach { row ->
            Row(Modifier.fillMaxWidth().clearAndSetSemantics {}, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { index ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (index < takenSlots) {
                            val person = names.getOrNull(index)
                            Avatar(person?.name ?: "", size = 56.dp, imageUrl = person?.imageUrl, initialSize = 18.sp)
                            Text(
                                person?.let { shortName(it.name) } ?: "",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold),
                                textAlign = TextAlign.Center,
                            )
                        } else {
                            val dash = scheme.outline
                            Box(
                                Modifier.size(56.dp).drawBehind {
                                    drawCircle(dash, style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))))
                                },
                            )
                            Text(
                                stringResource(R.string.places_slot_free),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
                                color = scheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
                // Eine unvollständige letzte Reihe hält die Spalten gleich breit.
                repeat(PLACES_PER_ROW - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

/**
 * Die eigene Anmeldung bei Leiter-Plätzen, wenn nicht die Pillen Ja/Nein gelten: Ist der Termin voll, steht «Voll» und der Knopf
 * «Auf die Warteliste setzen»; wer wartet, sieht «Du stehst auf der Warteliste», seine Nummer und «Von der Warteliste nehmen».
 * Ist die Anmeldung zu (`open` falsch), steht nur der Stand da, ohne Knopf.
 */
@Composable
fun WaitlistBlock(onWaitlist: Boolean, position: Int?, open: Boolean, running: Boolean, onJoin: () -> Unit, onLeave: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val title = stringResource(if (onWaitlist) R.string.waitlist_on else R.string.places_full)
    val hint = stringResource(R.string.waitlist_hint)
    val positionText = position?.let { stringResource(R.string.waitlist_position, it) }
    val spoken = if (onWaitlist) {
        listOfNotNull(title, positionText, hint).joinToString(". ")
    } else {
        listOf(stringResource(R.string.places_full_spoken), hint).joinToString(". ")
    }
    Column(
        Modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainerHigh, MaterialTheme.shapes.medium)
            .border(1.dp, scheme.outlineVariant, MaterialTheme.shapes.medium)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.semantics(mergeDescendants = true) { contentDescription = spoken }, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    painterResource(if (onWaitlist) R.drawable.ic_hourglass else R.drawable.ic_lock),
                    contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(16.dp),
                )
                Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
            }
            if (onWaitlist && positionText != null) {
                Text(positionText, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
            }
            Text(hint, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
        }
        if (open) {
            if (onWaitlist) {
                OutlinedPillButton(stringResource(R.string.waitlist_leave), onLeave, Modifier.fillMaxWidth(), enabled = !running)
            } else {
                PrimaryButton(stringResource(R.string.waitlist_join), onJoin, Modifier.fillMaxWidth(), enabled = !running)
            }
        }
    }
}

/** Unter der eigenen Zusage bei Leiter-Plätzen: «Dein Platz ist dabei» und was die Absage bewirkt. */
@Composable
fun OwnPlaceHint(places: LeaderPlaces) {
    Column(Modifier.fillMaxWidth().padding(start = 46.dp).semantics(mergeDescendants = true) {}) {
        Text(stringResource(R.string.place_yours), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
        Text(
            stringResource(if (places.waiting > 0) R.string.place_yours_waiting else R.string.place_yours_nobody),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
