package com.jumincho.beatingyesterday.ui.diet

import com.jumincho.beatingyesterday.core.food.FoodItem
import com.jumincho.beatingyesterday.core.food.FoodSearchError
import com.jumincho.beatingyesterday.core.food.FoodSearchResult
import com.jumincho.beatingyesterday.core.food.FoodSearchService
import com.jumincho.beatingyesterday.core.model.MealEntry
import com.jumincho.beatingyesterday.core.model.MealType
import com.jumincho.beatingyesterday.core.testing.FakeFoodSearchService
import com.jumincho.beatingyesterday.core.testing.FakeMealRepository
import com.jumincho.beatingyesterday.core.validation.FieldError
import com.jumincho.beatingyesterday.testing.MainDispatcherExtension
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class MealEditorViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val today = LocalDate.of(2026, 9, 23)
    private val meals = FakeMealRepository()
    private val foodSearch = FakeFoodSearchService()

    private fun newMealEditor(search: FoodSearchService? = foodSearch) = MealEditorViewModel(
        mealId = 0,
        date = today,
        initialType = MealType.LUNCH,
        mealRepository = meals,
        foodSearch = search,
    )

    @Test
    fun `saves a new meal on the given day`() = runTest {
        val viewModel = newMealEditor()
        viewModel.onTypeChange(MealType.DINNER)
        viewModel.onNameChange(" Bibimbap ")
        viewModel.onKcalChange("560")

        viewModel.save()

        assertEquals(listOf(MealEntry(1, today, MealType.DINNER, "Bibimbap", 560)), meals.all)
        assertTrue(viewModel.uiState.value.isSaved)
    }

    @Test
    fun `shows validation errors instead of saving`() = runTest {
        val viewModel = newMealEditor()
        viewModel.onKcalChange("lots")

        viewModel.save()

        val state = viewModel.uiState.value
        assertEquals(FieldError.Required, state.nameError)
        assertEquals(FieldError.NotANumber, state.kcalError)
        assertFalse(state.isSaved)
        assertTrue(meals.all.isEmpty())
        viewModel.onNameChange("Rice")
        assertNull(viewModel.uiState.value.nameError)
    }

    @Test
    fun `edits an existing meal, keeping its id and day`() = runTest {
        val existing = MealEntry(7, today.minusDays(2), MealType.SNACK, "Chips", 300)
        val repository = FakeMealRepository(listOf(existing))
        val viewModel = MealEditorViewModel(7, today, MealType.BREAKFAST, repository, foodSearch = null)

        val loaded = viewModel.uiState.value
        assertTrue(loaded.isEditing)
        assertEquals(MealType.SNACK, loaded.type)
        assertEquals("Chips", loaded.name)
        assertEquals("300", loaded.kcal)

        viewModel.onKcalChange("250")
        viewModel.save()

        assertEquals(listOf(existing.copy(kcal = 250)), repository.all)
    }

    @Test
    fun `food lookup is unavailable without an API key`() = runTest {
        val viewModel = newMealEditor(search = null)
        viewModel.onQueryChange("kimchi")

        viewModel.search()

        assertEquals(FoodSearchUiState.Unavailable, viewModel.uiState.value.search)
    }

    @Test
    fun `searches, then fills the form from the chosen food`() = runTest {
        val stew = FoodItem("Kimchi stew", kcalPerServing = 412.5, servingSize = "300", maker = null)
        foodSearch.result = FoodSearchResult.Success(listOf(stew), totalCount = 3)
        val gate = CompletableDeferred<Unit>()
        foodSearch.gate = gate
        val viewModel = newMealEditor()
        viewModel.onQueryChange("  kimchi ")

        viewModel.search()
        assertEquals(FoodSearchUiState.Loading, viewModel.uiState.value.search)
        gate.complete(Unit)
        runCurrent()

        assertEquals(listOf("kimchi"), foodSearch.queries)
        assertEquals(FoodSearchUiState.Results("kimchi", listOf(stew), 3), viewModel.uiState.value.search)
        viewModel.onFoodSelected(stew)
        assertEquals("Kimchi stew", viewModel.uiState.value.name)
        assertEquals("413", viewModel.uiState.value.kcal)
    }

    @Test
    fun `a food without calories keeps the typed value`() = runTest {
        val viewModel = newMealEditor()
        viewModel.onKcalChange("90")

        viewModel.onFoodSelected(FoodItem("Green tea", kcalPerServing = null, servingSize = null, maker = null))

        assertEquals("Green tea", viewModel.uiState.value.name)
        assertEquals("90", viewModel.uiState.value.kcal)
    }

    @Test
    fun `shows search failures and ignores blank queries`() = runTest {
        foodSearch.result = FoodSearchResult.Failure(FoodSearchError.Timeout)
        val viewModel = newMealEditor()

        viewModel.onQueryChange("   ")
        viewModel.search()
        assertEquals(FoodSearchUiState.Idle, viewModel.uiState.value.search)
        assertTrue(foodSearch.queries.isEmpty())

        viewModel.onQueryChange("kimchi")
        viewModel.search()
        assertEquals(FoodSearchUiState.Failed(FoodSearchError.Timeout), viewModel.uiState.value.search)
    }
}
