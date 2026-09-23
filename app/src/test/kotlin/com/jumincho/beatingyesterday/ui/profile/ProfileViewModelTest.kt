package com.jumincho.beatingyesterday.ui.profile

import com.jumincho.beatingyesterday.core.health.HealthCalculator
import com.jumincho.beatingyesterday.core.model.ActivityLevel
import com.jumincho.beatingyesterday.core.model.Sex
import com.jumincho.beatingyesterday.core.model.UserProfile
import com.jumincho.beatingyesterday.core.profile.ProfileDraft
import com.jumincho.beatingyesterday.core.profile.ProfileField
import com.jumincho.beatingyesterday.core.testing.FakeProfileRepository
import com.jumincho.beatingyesterday.core.testing.MutableClock
import com.jumincho.beatingyesterday.core.validation.FieldError
import com.jumincho.beatingyesterday.testing.MainDispatcherExtension
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.LocalDate
import java.time.LocalDateTime

class ProfileViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val clock = MutableClock.at(LocalDateTime.of(2026, 9, 23, 9, 0))
    private val profile = UserProfile("Minji", Sex.FEMALE, 1998, 162.5, 54.0, ActivityLevel.LIGHT)

    @Test
    fun `starts with an empty form when there is no profile`() = runTest {
        val state = ProfileViewModel(FakeProfileRepository(), clock).uiState.value

        assertFalse(state.isLoading)
        assertEquals(ProfileDraft(), state.draft)
        assertNull(state.preview)
    }

    @Test
    fun `loads the saved profile with its health numbers`() = runTest {
        val state = ProfileViewModel(FakeProfileRepository(profile), clock).uiState.value

        assertEquals(ProfileDraft.from(profile), state.draft)
        assertEquals(HealthCalculator.metricsFor(profile, LocalDate.of(2026, 9, 23)), state.preview)
    }

    @Test
    fun `shows every error on save and clears a field's error when it is edited`() = runTest {
        val viewModel = ProfileViewModel(FakeProfileRepository(), clock)

        viewModel.save()
        assertEquals(ProfileField.entries.toSet(), viewModel.uiState.value.errors.keys)

        viewModel.onNameChange("Minji")
        assertFalse(ProfileField.NAME in viewModel.uiState.value.errors)
        assertEquals(FieldError.Required, viewModel.uiState.value.errors[ProfileField.SEX])
    }

    @Test
    fun `previews the numbers once the draft is valid and saves the profile`() = runTest {
        val repository = FakeProfileRepository()
        val viewModel = ProfileViewModel(repository, clock)

        viewModel.onNameChange(" Minji ")
        viewModel.onSexChange(Sex.FEMALE)
        viewModel.onBirthYearChange("1998")
        viewModel.onHeightChange("162.5")
        viewModel.onWeightChange("54")
        assertNull(viewModel.uiState.value.preview)
        viewModel.onActivityLevelChange(ActivityLevel.LIGHT)
        assertNotNull(viewModel.uiState.value.preview)

        viewModel.save()

        assertEquals(profile, repository.saved)
        assertTrue(viewModel.uiState.value.isSaved)
        viewModel.onSavedHandled()
        assertFalse(viewModel.uiState.value.isSaved)
    }
}
