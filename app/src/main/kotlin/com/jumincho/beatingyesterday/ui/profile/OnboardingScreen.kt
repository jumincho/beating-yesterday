package com.jumincho.beatingyesterday.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jumincho.beatingyesterday.R
import com.jumincho.beatingyesterday.core.model.ActivityLevel
import com.jumincho.beatingyesterday.core.model.Sex
import com.jumincho.beatingyesterday.core.profile.ProfileDraft
import com.jumincho.beatingyesterday.ui.AppViewModelProvider
import com.jumincho.beatingyesterday.ui.theme.BeatingYesterdayTheme

/** First-launch profile setup. Saving it opens the main app. */
@Composable
fun OnboardingScreen(viewModel: ProfileViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    OnboardingScreen(
        uiState = uiState,
        onNameChange = viewModel::onNameChange,
        onSexChange = viewModel::onSexChange,
        onBirthYearChange = viewModel::onBirthYearChange,
        onHeightChange = viewModel::onHeightChange,
        onWeightChange = viewModel::onWeightChange,
        onActivityLevelChange = viewModel::onActivityLevelChange,
        onSubmit = viewModel::save,
    )
}

/** Stateless onboarding screen. */
@Composable
fun OnboardingScreen(
    uiState: ProfileUiState,
    onNameChange: (String) -> Unit,
    onSexChange: (Sex) -> Unit,
    onBirthYearChange: (String) -> Unit,
    onHeightChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onActivityLevelChange: (ActivityLevel) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_emoji_events),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp),
            )
            Text(
                text = stringResource(R.string.onboarding_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.onboarding_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ProfileForm(
                uiState = uiState,
                onNameChange = onNameChange,
                onSexChange = onSexChange,
                onBirthYearChange = onBirthYearChange,
                onHeightChange = onHeightChange,
                onWeightChange = onWeightChange,
                onActivityLevelChange = onActivityLevelChange,
            )
            uiState.preview?.let { HealthSummaryCard(metrics = it) }
            Button(
                onClick = onSubmit,
                enabled = !uiState.isLoading && !uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.onboarding_start))
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun OnboardingScreenPreview() {
    BeatingYesterdayTheme(dynamicColor = false) {
        OnboardingScreen(
            uiState = ProfileUiState(
                isLoading = false,
                draft = ProfileDraft(name = "Minji", sex = Sex.FEMALE, birthYear = "1999"),
            ),
            onNameChange = {},
            onSexChange = {},
            onBirthYearChange = {},
            onHeightChange = {},
            onWeightChange = {},
            onActivityLevelChange = {},
            onSubmit = {},
        )
    }
}
