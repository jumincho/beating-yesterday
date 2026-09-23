package com.jumincho.beatingyesterday.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jumincho.beatingyesterday.R
import com.jumincho.beatingyesterday.core.health.HealthMetrics
import com.jumincho.beatingyesterday.core.model.ActivityLevel
import com.jumincho.beatingyesterday.core.model.Sex
import com.jumincho.beatingyesterday.core.profile.ProfileField
import com.jumincho.beatingyesterday.core.validation.FieldError
import com.jumincho.beatingyesterday.ui.format.bmiText
import com.jumincho.beatingyesterday.ui.format.description
import com.jumincho.beatingyesterday.ui.format.errorSupportingText
import com.jumincho.beatingyesterday.ui.format.kcalText
import com.jumincho.beatingyesterday.ui.format.label
import com.jumincho.beatingyesterday.ui.format.message

/** The profile fields, shared by onboarding and the Profile screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileForm(
    uiState: ProfileUiState,
    onNameChange: (String) -> Unit,
    onSexChange: (Sex) -> Unit,
    onBirthYearChange: (String) -> Unit,
    onHeightChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onActivityLevelChange: (ActivityLevel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val draft = uiState.draft
    val errors = uiState.errors
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = draft.name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.profile_name)) },
            singleLine = true,
            isError = errors[ProfileField.NAME] != null,
            supportingText = errorSupportingText(errors[ProfileField.NAME]),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
            ),
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = stringResource(R.string.profile_sex), style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                Sex.entries.forEachIndexed { index, sex ->
                    SegmentedButton(
                        selected = draft.sex == sex,
                        onClick = { onSexChange(sex) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = Sex.entries.size),
                        label = { Text(stringResource(sex.label)) },
                    )
                }
            }
            FieldErrorText(errors[ProfileField.SEX])
        }

        OutlinedTextField(
            value = draft.birthYear,
            onValueChange = onBirthYearChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.profile_birth_year)) },
            placeholder = { Text(stringResource(R.string.profile_birth_year_hint)) },
            singleLine = true,
            isError = errors[ProfileField.BIRTH_YEAR] != null,
            supportingText = errorSupportingText(errors[ProfileField.BIRTH_YEAR]),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = draft.heightCm,
                onValueChange = onHeightChange,
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(R.string.profile_height)) },
                suffix = { Text(stringResource(R.string.unit_cm)) },
                singleLine = true,
                isError = errors[ProfileField.HEIGHT] != null,
                supportingText = errorSupportingText(errors[ProfileField.HEIGHT]),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            )
            OutlinedTextField(
                value = draft.weightKg,
                onValueChange = onWeightChange,
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(R.string.profile_weight)) },
                suffix = { Text(stringResource(R.string.unit_kg)) },
                singleLine = true,
                isError = errors[ProfileField.WEIGHT] != null,
                supportingText = errorSupportingText(errors[ProfileField.WEIGHT]),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            )
        }

        Column(modifier = Modifier.selectableGroup()) {
            Text(text = stringResource(R.string.profile_activity_level), style = MaterialTheme.typography.labelLarge)
            ActivityLevel.entries.forEach { level ->
                val selected = draft.activityLevel == level
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .selectable(selected = selected, role = Role.RadioButton, onClick = {
                            onActivityLevelChange(level)
                        }),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = selected, onClick = null)
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(text = stringResource(level.label), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = stringResource(level.description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            FieldErrorText(errors[ProfileField.ACTIVITY_LEVEL])
        }
    }
}

/** BMI, energy expenditure and the resulting daily target, with a disclaimer. */
@Composable
fun HealthSummaryCard(metrics: HealthMetrics, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.health_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            MetricRow(
                label = stringResource(R.string.health_bmi),
                value = stringResource(
                    R.string.health_bmi_value,
                    bmiText(metrics.bmi),
                    stringResource(metrics.bmiCategory.label),
                ),
            )
            MetricRow(label = stringResource(R.string.health_bmr), value = kcalText(metrics.basalMetabolicRateKcal))
            MetricRow(
                label = stringResource(R.string.health_tdee),
                value = kcalText(metrics.totalDailyEnergyExpenditureKcal),
            )
            MetricRow(
                label = stringResource(R.string.health_target),
                value = kcalText(metrics.dailyTargetKcal),
                emphasized = true,
            )
            Text(
                text = stringResource(R.string.health_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String, emphasized: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasized) FontWeight.SemiBold else null,
        )
    }
}

@Composable
private fun FieldErrorText(error: FieldError?) {
    if (error != null) {
        Text(
            text = error.message(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}
