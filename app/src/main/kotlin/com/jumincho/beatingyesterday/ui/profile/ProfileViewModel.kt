package com.jumincho.beatingyesterday.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jumincho.beatingyesterday.core.data.ProfileRepository
import com.jumincho.beatingyesterday.core.health.HealthCalculator
import com.jumincho.beatingyesterday.core.health.HealthMetrics
import com.jumincho.beatingyesterday.core.model.ActivityLevel
import com.jumincho.beatingyesterday.core.model.Sex
import com.jumincho.beatingyesterday.core.profile.ProfileDraft
import com.jumincho.beatingyesterday.core.profile.ProfileField
import com.jumincho.beatingyesterday.core.profile.ProfileValidationResult
import com.jumincho.beatingyesterday.core.profile.ProfileValidator
import com.jumincho.beatingyesterday.core.time.today
import com.jumincho.beatingyesterday.core.validation.FieldError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock

/**
 * State of the profile form.
 *
 * @property preview health numbers for the draft, available as soon as the draft is valid.
 * @property isSaved set once the profile has been saved, until [ProfileViewModel.onSavedHandled].
 */
data class ProfileUiState(
    val isLoading: Boolean = true,
    val draft: ProfileDraft = ProfileDraft(),
    val errors: Map<ProfileField, FieldError> = emptyMap(),
    val preview: HealthMetrics? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
)

/** Edits and saves the profile, for both onboarding and the Profile screen. */
class ProfileViewModel(private val profileRepository: ProfileRepository, private val clock: Clock) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val draft = profileRepository.profile.first()?.let(ProfileDraft::from) ?: ProfileDraft()
            _uiState.update { it.copy(isLoading = false, draft = draft, preview = preview(draft)) }
        }
    }

    fun onNameChange(name: String) {
        edit(ProfileField.NAME) { copy(name = name) }
    }

    fun onSexChange(sex: Sex) {
        edit(ProfileField.SEX) { copy(sex = sex) }
    }

    fun onBirthYearChange(birthYear: String) {
        edit(ProfileField.BIRTH_YEAR) { copy(birthYear = birthYear) }
    }

    fun onHeightChange(heightCm: String) {
        edit(ProfileField.HEIGHT) { copy(heightCm = heightCm) }
    }

    fun onWeightChange(weightKg: String) {
        edit(ProfileField.WEIGHT) { copy(weightKg = weightKg) }
    }

    fun onActivityLevelChange(activityLevel: ActivityLevel) {
        edit(ProfileField.ACTIVITY_LEVEL) { copy(activityLevel = activityLevel) }
    }

    /** Validates the draft and saves it, or shows the errors. */
    fun save() {
        val state = _uiState.value
        if (state.isLoading || state.isSaving) return
        when (val result = ProfileValidator.validate(state.draft, clock.today())) {
            is ProfileValidationResult.Invalid -> _uiState.update { it.copy(errors = result.errors) }

            is ProfileValidationResult.Valid -> {
                _uiState.update { it.copy(isSaving = true, errors = emptyMap()) }
                viewModelScope.launch {
                    profileRepository.saveProfile(result.profile)
                    _uiState.update { it.copy(isSaving = false, isSaved = true) }
                }
            }
        }
    }

    /** Call once the screen has reacted to [ProfileUiState.isSaved]. */
    fun onSavedHandled() {
        _uiState.update { it.copy(isSaved = false) }
    }

    private fun edit(field: ProfileField, transform: ProfileDraft.() -> ProfileDraft) {
        _uiState.update { state ->
            val draft = state.draft.transform()
            state.copy(draft = draft, errors = state.errors - field, preview = preview(draft))
        }
    }

    private fun preview(draft: ProfileDraft): HealthMetrics? {
        val today = clock.today()
        val result = ProfileValidator.validate(draft, today) as? ProfileValidationResult.Valid ?: return null
        return HealthCalculator.metricsFor(result.profile, today)
    }
}
