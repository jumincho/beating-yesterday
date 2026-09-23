package com.jumincho.beatingyesterday.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jumincho.beatingyesterday.ui.navigation.MainNavigation
import com.jumincho.beatingyesterday.ui.profile.OnboardingScreen

/** Root composable: onboarding until a profile exists, then the main navigation. */
@Composable
fun BeatingYesterdayApp(viewModel: MainViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Crossfade(targetState = uiState, label = "app content") { state ->
            when (state) {
                AppUiState.Loading -> {}
                AppUiState.NeedsOnboarding -> OnboardingScreen()
                AppUiState.Ready -> MainNavigation()
            }
        }
    }
}
