package io.github.sorry4nothing.cevios.programs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.sorry4nothing.cevios.R
import io.github.sorry4nothing.cevios.chat.Profile
import io.github.sorry4nothing.cevios.net.ApiResult
import io.github.sorry4nothing.cevios.ui.ErrorState
import io.github.sorry4nothing.cevios.ui.Header
import io.github.sorry4nothing.cevios.ui.HeaderButton
import io.github.sorry4nothing.cevios.ui.LoadingSkeleton
import io.github.sorry4nothing.cevios.ui.NoticeBanner
import io.github.sorry4nothing.cevios.ui.NoticeKind
import io.github.sorry4nothing.cevios.ui.PrimaryButton
import io.github.sorry4nothing.cevios.ui.SlowHint
import io.github.sorry4nothing.cevios.ui.TextActionButton
import io.github.sorry4nothing.cevios.ui.focusRing
import io.github.sorry4nothing.cevios.ui.StageLogo
import io.github.sorry4nothing.cevios.ui.theme.Sizes
import io.github.sorry4nothing.cevios.ui.theme.Spacing

private val CARD = RoundedCornerShape(16.dp)

/** Stufen und Sichtbarkeit eines Programms setzen. Die App schickt den vollen neuen Stand, wie es die API verlangt. */
@Composable
fun AssignmentScreen(
    program: Program,
    ageGroups: ApiResult<AgeGroupList>?,
    profile: Profile?,
    edit: EditState,
    onSave: (AssignmentRequest) -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    val back = HeaderButton(R.drawable.ic_back, stringResource(R.string.back_to_program), onBack)
    when {
        ageGroups is ApiResult.Failure -> Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Header(title = null, back = back)
            ErrorState(ageGroups.error, onRetry, onToPrograms = onBack)
        }
        ageGroups !is ApiResult.Ok || profile == null -> Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Header(title = null, back = back)
            LoadingSkeleton(stringResource(R.string.assignment_loading))
        }
        else -> AssignmentForm(program, AssignmentRules.choices(ageGroups.value.ageGroups, profile), edit, onSave, onBack)
    }
}

@Composable
private fun AssignmentForm(
    program: Program,
    choices: List<ProgramAgeGroup>,
    edit: EditState,
    onSave: (AssignmentRequest) -> Unit,
    onBack: () -> Unit,
) {
    val fixed = AssignmentRules.fixed(program, choices)
    val mayChangeVisibility = AssignmentRules.mayChangeVisibility(fixed)
    val startIds = program.ageGroups.map { it.id }.filter { id -> choices.any { it.id == id } }
    // Als Text gespeichert: Eine Liste oder ein Enum überlebt das Drehen des Geräts nicht ohne eigenen Saver.
    var selectedIds by rememberSaveable { mutableStateOf(startIds.joinToString(",")) }
    var visibilityKey by rememberSaveable { mutableStateOf(program.visibility) }
    val selected = selectedIds.split(",").filter { it.isNotEmpty() }.toSet()
    val visibility = Visibility.entries.firstOrNull { it.api == visibilityKey } ?: Visibility.INTERN
    val valid = AssignmentRules.valid(selected, fixed, visibility)
    val unchanged = selected == startIds.toSet() && visibility.api == program.visibility

    FormPage(program.title, stringResource(R.string.assignment_title), onBack) {
        SectionTitle(stringResource(R.string.assignment_age_groups))
        if (choices.isEmpty()) NoticeBanner(NoticeKind.INFO, stringResource(R.string.assignment_no_choices))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            choices.forEach { group ->
                val checked = group.id in selected
                ChoiceRow(
                    title = group.name,
                    text = null,
                    stageId = group.id,
                    enabled = !edit.running,
                    control = { Checkbox(checked = checked, onCheckedChange = null) },
                    modifier = Modifier.toggleable(
                        value = checked,
                        enabled = !edit.running,
                        role = Role.Checkbox,
                        onValueChange = { on ->
                            selectedIds = (if (on) selected + group.id else selected - group.id).joinToString(",")
                        },
                    ),
                )
            }
        }
        if (fixed.isNotEmpty()) {
            NoticeBanner(NoticeKind.INFO, stringResource(R.string.assignment_fixed, fixed.joinToString(", ") { it.name }))
        }

        SectionTitle(stringResource(R.string.assignment_visibility))
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            Visibility.entries.forEach { option ->
                val isSelected = option == visibility
                ChoiceRow(
                    title = stringResource(option.title),
                    text = stringResource(option.text),
                    enabled = mayChangeVisibility && !edit.running,
                    control = { RadioButton(selected = isSelected, onClick = null) },
                    modifier = Modifier.selectable(
                        selected = isSelected,
                        enabled = mayChangeVisibility && !edit.running,
                        role = Role.RadioButton,
                        onClick = { visibilityKey = option.api },
                    ),
                )
            }
        }
        if (!valid) NoticeBanner(NoticeKind.WARNING, stringResource(R.string.assignment_needs_age_group))
        SaveBar(
            edit,
            enabled = valid,
            onSave = { if (unchanged) onBack() else onSave(AssignmentRules.request(selected, fixed, visibility)) },
            onCancel = onBack,
        )
    }
}

private val Visibility.title
    get() = when (this) {
        Visibility.INTERN -> R.string.visibility_intern
        Visibility.LEADERS -> R.string.visibility_leaders
        Visibility.PARENTS -> R.string.visibility_parents
    }

private val Visibility.text
    get() = when (this) {
        Visibility.INTERN -> R.string.visibility_intern_text
        Visibility.LEADERS -> R.string.visibility_leaders_text
        Visibility.PARENTS -> R.string.visibility_parents_text
    }

/** Eine wählbare Zeile in einer Karte: Name und Erklärung links, rechts Kästchen oder Auswahlkreis. */
@Composable
private fun ChoiceRow(
    title: String,
    text: String?,
    enabled: Boolean,
    stageId: String? = null,
    control: @Composable () -> Unit,
    modifier: Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.touchTarget)
            .focusRing(CARD)
            .clip(CARD)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CARD)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        control()
        stageId?.let { StageLogo(it, size = 40.dp) }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            text?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 6.dp).semantics { heading() })
}

/** Kopf mit Zurück und Titel, darunter der scrollende Inhalt; die Tastatur schiebt den Inhalt nach oben. */
@Composable
private fun FormPage(eyebrow: String, title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    val back = HeaderButton(R.drawable.ic_back, stringResource(R.string.back_to_program), onBack)
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState())) {
        Header(title = title, eyebrow = eyebrow, back = back, titleStyle = MaterialTheme.typography.headlineMedium, focusOnTitle = true)
        Column(Modifier.padding(horizontal = Spacing.s5), verticalArrangement = Arrangement.spacedBy(Spacing.s4)) {
            content()
            Spacer(Modifier.height(Spacing.s2))
        }
    }
}

/** Speichern und Abbrechen mit dem Stand darüber: «Wird gespeichert …» oder der Fehler. */
@Composable
private fun SaveBar(edit: EditState, enabled: Boolean, onSave: () -> Unit, onCancel: () -> Unit) {
    SlowHint(edit.running)
    edit.error?.let { NoticeBanner(NoticeKind.ERROR, stringResource(it.text)) }
    PrimaryButton(
        stringResource(R.string.save), onSave, Modifier.fillMaxWidth(), enabled = enabled,
        loading = edit.running, loadingText = stringResource(R.string.saving),
    )
    TextActionButton(stringResource(R.string.cancel), onCancel, Modifier.fillMaxWidth())
}
