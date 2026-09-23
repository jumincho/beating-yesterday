package com.jumincho.beatingyesterday.ui.diet

import com.jumincho.beatingyesterday.core.health.HealthCalculator
import com.jumincho.beatingyesterday.core.model.ActivityLevel
import com.jumincho.beatingyesterday.core.model.MealEntry
import com.jumincho.beatingyesterday.core.model.MealType
import com.jumincho.beatingyesterday.core.model.Sex
import com.jumincho.beatingyesterday.core.model.UserProfile
import com.jumincho.beatingyesterday.core.testing.FakeMealRepository
import com.jumincho.beatingyesterday.core.testing.FakeProfileRepository
import com.jumincho.beatingyesterday.core.testing.MutableClock
import com.jumincho.beatingyesterday.testing.MainDispatcherExtension
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.LocalDate
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class DietViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val today = LocalDate.of(2026, 9, 23)
    private val clock = MutableClock.at(LocalDateTime.of(2026, 9, 23, 12, 30))
    private val profile = UserProfile("Minji", Sex.FEMALE, 1998, 162.5, 54.0, ActivityLevel.LIGHT)
    private val meals = FakeMealRepository(
        listOf(
            MealEntry(1, today, MealType.DINNER, "Pasta", 700),
            MealEntry(2, today, MealType.BREAKFAST, "Toast", 250),
            MealEntry(3, today, MealType.BREAKFAST, "Latte", 120),
            MealEntry(4, today.minusDays(1), MealType.LUNCH, "Ramen", 500),
        ),
    )

    private fun TestScope.createViewModel(): DietViewModel =
        DietViewModel(FakeProfileRepository(profile), meals, clock, MutableStateFlow(today)).also { viewModel ->
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        }

    private fun DietViewModel.ready(): DietUiState.Ready = assertInstanceOf<DietUiState.Ready>(uiState.value)

    @Test
    fun `groups the day's meals by type in display order`() = runTest {
        val state = createViewModel().ready()

        assertEquals(MealType.entries, state.sections.map { it.type })
        assertEquals(listOf("Toast", "Latte"), state.sections.first().meals.map { it.name })
        assertTrue(state.sections[1].meals.isEmpty())
        assertEquals(1_070, state.totalKcal)
        assertEquals(HealthCalculator.metricsFor(profile, today), state.metrics)
        assertTrue(state.isToday)
    }

    @Test
    fun `suggests the meal for the current time`() = runTest {
        assertEquals(MealType.LUNCH, createViewModel().ready().suggestedType)
    }

    @Test
    fun `browses earlier days but never the future`() = runTest {
        val viewModel = createViewModel()

        viewModel.showNextDay()
        assertEquals(today, viewModel.ready().date)

        viewModel.showPreviousDay()
        val yesterday = viewModel.ready()
        assertEquals(today.minusDays(1), yesterday.date)
        assertEquals(1, yesterday.daysAgo)
        assertEquals(500, yesterday.totalKcal)

        viewModel.showToday()
        assertEquals(today, viewModel.ready().date)
    }

    @Test
    fun `deleting a meal can be undone`() = runTest {
        val viewModel = createViewModel()
        val toast = meals.all.first { it.name == "Toast" }

        viewModel.deleteMeal(toast)
        assertEquals(toast, viewModel.deletedMeal.value)
        assertFalse(toast in meals.all)

        viewModel.undoDelete()
        assertTrue(toast in meals.all)
        assertNull(viewModel.deletedMeal.value)
    }
}
