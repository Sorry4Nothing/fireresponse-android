package ch.fireresponse.app.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.fireresponse.app.R
import ch.fireresponse.app.net.ApiResult
import ch.fireresponse.app.ui.ErrorState
import ch.fireresponse.app.ui.Header
import ch.fireresponse.app.ui.HeaderButton
import ch.fireresponse.app.ui.LoadingSkeleton
import ch.fireresponse.app.ui.theme.Spacing

private val CARD = RoundedCornerShape(16.dp)

/**
 * Datenschutzerklärung nach dem Entwurf `L10-Datenschutz`: Zurück-Knopf, Überzeile, Titel, je Abschnitt eine Karte mit Überschrift
 * und Absätzen oder Liste, unten die Fassung. Der Text kommt vom Server (`GET /api/v1/datenschutz`), die App erfindet keinen.
 */
@Composable
fun PrivacyScreen(state: ApiResult<Privacy>?, backLabel: String, onBack: () -> Unit, onRetry: () -> Unit) {
    val title = stringResource(R.string.profile_privacy)
    val eyebrow = stringResource(R.string.department)
    val back = HeaderButton(R.drawable.ic_back, backLabel, onBack)
    when (state) {
        null -> Column(Modifier.fillMaxSize()) {
            Header(title = title, eyebrow = eyebrow, back = back)
            LoadingSkeleton(stringResource(R.string.privacy_loading))
        }
        is ApiResult.Failure -> Column(Modifier.fillMaxSize()) {
            Header(title = title, eyebrow = eyebrow, back = back)
            ErrorState(state.error, onRetry)
        }
        is ApiResult.Ok -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Header(title = title, eyebrow = eyebrow, back = back, focusOnTitle = true)
            Column(Modifier.padding(horizontal = Spacing.s5), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.value.sections.forEach { Section(it) }
                Text(
                    stringResource(R.string.privacy_version, state.value.version),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = Spacing.s4),
                )
            }
        }
    }
}

@Composable
private fun Section(section: PrivacySection) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val body = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp)
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, CARD)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CARD)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            section.title,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp, lineHeight = 24.sp),
            modifier = Modifier.semantics { heading() },
        )
        privacyBlocks(section.text).forEach { block ->
            when (block) {
                is PrivacyBlock.Paragraph -> Text(block.text, style = body, color = muted)
                is PrivacyBlock.Bullets -> Column(Modifier.padding(start = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    block.items.forEach { item ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("•", style = body, color = muted)
                            Text(item, style = body, color = muted, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
