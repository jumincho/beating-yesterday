package com.jumincho.beatingyesterday.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jumincho.beatingyesterday.core.data.ProfileRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Keeps upstream flows alive for a few seconds so a configuration change does not restart them. */
internal val WhileUiSubscribed: SharingStarted = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000)

/** Which part of the app to show. */
sealed interface AppUiState {
    /** The profile has not been read yet. */
    data object Loading : AppUiState

    /** No profile yet: show onboarding. */
    data object NeedsOnboarding : AppUiState

    /** A profile exists: show the main navigation. */
    data object Ready : AppUiState
}

/** Decides between onboarding and the main app. Saving a profile switches to [AppUiState.Ready]. */
class MainViewModel(profileRepository: ProfileRepository) : ViewModel() {
    val uiState: StateFlow<AppUiState> = profileRepository.profile
        .map { profile -> if (profile == null) AppUiState.NeedsOnboarding else AppUiState.Ready }
        .stateIn(viewModelScope, WhileUiSubscribed, AppUiState.Loading)
}
