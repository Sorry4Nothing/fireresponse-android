package io.github.sorry4nothing.cevios.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.sorry4nothing.cevios.R
import io.github.sorry4nothing.cevios.net.ApiError
import io.github.sorry4nothing.cevios.ui.theme.Spacing
import io.github.sorry4nothing.cevios.ui.theme.Pill
import io.github.sorry4nothing.cevios.ui.theme.cevi
import kotlinx.coroutines.delay

/** Welche ganzseitige Fehlerseite ein [ApiError] bekommt (Design: Zustand-Screens). */
enum class ErrorPage {
    OFFLINE, NOT_FOUND, NO_ACCESS, SESSION, OUTDATED, ERROR;

    companion object {
        fun from(error: ApiError) = when (error) {
            ApiError.NETWORK -> OFFLINE
            ApiError.NOT_FOUND -> NOT_FOUND
            ApiError.FORBIDDEN -> NO_ACCESS
            ApiError.NOT_SIGNED_IN -> SESSION
            ApiError.APP_OUTDATED -> OUTDATED
            ApiError.TOO_MANY_REQUESTS, ApiError.REGISTRATION_CLOSED, ApiError.PROGRAM_STARTED, ApiError.DIRECT_CHAT_UNAVAILABLE,
            ApiError.MESSAGE_TOO_FAST, ApiError.UNKNOWN -> ERROR
        }
    }
}

/**
 * Ganzseitiger Zustand nach dem Muster des Designs: rundes Icon, Titel, ein Satz und höchstens eine Hauptaktion.
 * Fehler werden angesagt (`liveRegion`).
 */
@Composable
fun StatePage(
    @DrawableRes icon: Int,
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    circle: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    iconColor: Color = MaterialTheme.cevi.accent,
    error: Boolean = false,
    actions: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier.fillMaxSize().padding(start = 32.dp, end = 32.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.s3, Alignment.CenterVertically),
    ) {
        Box(Modifier.size(88.dp).background(circle, Pill), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), contentDescription = null, tint = iconColor, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(Spacing.s2))
        Column(
            Modifier.semantics { liveRegion = if (error) LiveRegionMode.Assertive else LiveRegionMode.Polite },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.s3),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp, lineHeight = 30.sp),
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text,
                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 300.dp),
            )
        }
        actions()
    }
}

/**
 * Die Fehlerseite zu einem [ApiError]. Eine Aktion, die es nicht gibt, fehlt auch als Knopf: «Anmelden» erscheint nur, wo
 * `onSignIn` gesetzt ist (nach abgelaufener Sitzung), «Zu den Programmen» nur, wo `onToPrograms` gesetzt ist.
 */
@Composable
fun ErrorState(
    error: ApiError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onToPrograms: (() -> Unit)? = null,
    onSignIn: (() -> Unit)? = null,
    @StringRes notFoundText: Int = R.string.state_404_text,
) {
    val scheme = MaterialTheme.colorScheme
    when (ErrorPage.from(error)) {
        ErrorPage.OFFLINE -> StatePage(
            R.drawable.ic_offline, stringResource(R.string.state_offline_title), stringResource(R.string.state_offline_text),
            modifier, error = true,
        ) { PrimaryButton(stringResource(R.string.retry), onRetry, icon = R.drawable.ic_refresh) }

        ErrorPage.NOT_FOUND -> StatePage(
            R.drawable.ic_not_found, stringResource(R.string.state_404_title), stringResource(notFoundText), modifier,
        ) { onToPrograms?.let { PrimaryButton(stringResource(R.string.to_programs), it) } }

        ErrorPage.NO_ACCESS -> StatePage(
            R.drawable.ic_lock, stringResource(R.string.state_access_title), stringResource(R.string.state_access_text), modifier,
        ) { onToPrograms?.let { PrimaryButton(stringResource(R.string.to_programs), it) } }

        ErrorPage.SESSION -> StatePage(
            R.drawable.ic_clock, stringResource(R.string.state_session_title), stringResource(R.string.state_session_text), modifier,
        ) { onSignIn?.let { PrimaryButton(stringResource(R.string.login_submit), it) } }

        ErrorPage.OUTDATED -> StatePage(
            R.drawable.ic_warning, stringResource(R.string.state_error_title), stringResource(error.text), modifier,
            circle = scheme.errorContainer, iconColor = scheme.onErrorContainer, error = true,
        )

        ErrorPage.ERROR -> StatePage(
            R.drawable.ic_warning, stringResource(R.string.state_error_title), stringResource(R.string.state_error_text), modifier,
            circle = scheme.errorContainer, iconColor = scheme.onErrorContainer, error = true,
        ) {
            PrimaryButton(stringResource(R.string.retry), onRetry, icon = R.drawable.ic_refresh)
            onToPrograms?.let { TextActionButton(stringResource(R.string.to_programs), it) }
        }
    }
}

@Composable
fun EmptyState(@DrawableRes icon: Int, title: String, text: String, onRefresh: () -> Unit, modifier: Modifier = Modifier) {
    StatePage(icon, title, text, modifier) {
        PrimaryButton(stringResource(R.string.refresh), onRefresh, icon = R.drawable.ic_refresh)
    }
}

/** Ein Balken des Skeletts: grau, ohne Animation (das Design legt keinen Schimmer fest). */
@Composable
private fun Bar(barWidth: Int, barHeight: Int, modifier: Modifier = Modifier) {
    Box(modifier.width(barWidth.dp).height(barHeight.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(8.dp)))
}

/**
 * Skelett-Karten statt Inhalt (Laden). Der Screenreader hört «wird geladen» erst nach einer Sekunde: kurze Ladezeiten
 * sollen nicht dazwischenreden.
 */
@Composable
fun LoadingSkeleton(description: String, modifier: Modifier = Modifier) {
    var announced by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(1000)
        announced = true
    }
    Column(
        modifier.fillMaxWidth().padding(start = Spacing.s3, end = Spacing.s5).then(
            if (announced) {
                Modifier.semantics {
                    contentDescription = description
                    liveRegion = LiveRegionMode.Polite
                }
            } else {
                Modifier.semantics { contentDescription = "" }
            },
        ),
    ) {
        val days = listOf(listOf(0, 1), listOf(2), listOf(3))
        days.forEachIndexed { index, cards ->
            val isLast = index == days.lastIndex
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                Column(Modifier.width(52.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Bar(22, 10)
                    Spacer(Modifier.height(6.dp))
                    Bar(30, 26)
                    if (!isLast) {
                        Spacer(Modifier.height(6.dp))
                        Box(Modifier.width(2.dp).weight(1f).background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(1.dp)))
                    }
                }
                Column(
                    Modifier.weight(1f).padding(bottom = if (isLast) 0.dp else 18.dp),
                    verticalArrangement = Arrangement.spacedBy(Spacing.s2),
                ) {
                    cards.forEach { SkeletonCard(it) }
                }
            }
        }
    }
}

@Composable
private fun SkeletonCard(number: Int) {
    val widths = listOf(listOf(110, 200, 90), listOf(110, 150, 120), listOf(110, 180, 100), listOf(110, 170, 80))[number % 4]
    Column(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.medium).padding(Spacing.s4),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Bar(widths[0], 12)
        Bar(widths[1], 18)
        Bar(widths[2], 12)
    }
}
