package com.jumincho.beatingyesterday.ui.diet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jumincho.beatingyesterday.core.data.MealRepository
import com.jumincho.beatingyesterday.core.food.FoodItem
import com.jumincho.beatingyesterday.core.food.FoodSearchError
import com.jumincho.beatingyesterday.core.food.FoodSearchResult
import com.jumincho.beatingyesterday.core.food.FoodSearchService
import com.jumincho.beatingyesterday.core.meal.MealValidationResult
import com.jumincho.beatingyesterday.core.meal.MealValidator
import com.jumincho.beatingyesterday.core.model.MealEntry
import com.jumincho.beatingyesterday.core.model.MealType
import com.jumincho.beatingyesterday.core.validation.FieldError
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

/** State of the optional food lookup. */
sealed interface FoodSearchUiState {
    /** The build has no API key, so only manual entry is possible. */
    data object Unavailable : FoodSearchUiState

    data object Idle : FoodSearchUiState

    data object Loading : FoodSearchUiState

    data class Results(val query: String, val items: List<FoodItem>, val totalCount: Int) : FoodSearchUiState

    data class Failed(val error: FoodSearchError) : FoodSearchUiState
}

/** State of the meal editor. [isSaved] tells the screen to close. */
data class MealEditorUiState(
    val isLoading: Boolean = false,
    val isEditing: Boolean = false,
    val type: MealType = MealType.BREAKFAST,
    val name: String = "",
    val kcal: String = "",
    val nameError: FieldError? = null,
    val kcalError: FieldError? = null,
    val query: String = "",
    val search: FoodSearchUiState = FoodSearchUiState.Idle,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
)

/**
 * Adds a meal on [date] or, when [mealId] is not `0`, edits an existing one.
 *
 * @param foodSearch calorie lookup, or `null` when it is not configured.
 */
class MealEditorViewModel(
    mealId: Long,
    private val date: LocalDate,
    initialType: MealType,
    private val mealRepository: MealRepository,
    private val foodSearch: FoodSearchService?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        MealEditorUiState(
            isLoading = mealId != 0L,
            isEditing = mealId != 0L,
            type = initialType,
            search = if (foodSearch == null) FoodSearchUiState.Unavailable else FoodSearchUiState.Idle,
        ),
    )
    val uiState: StateFlow<MealEditorUiState> = _uiState.asStateFlow()

    private var editedMeal: MealEntry? = null
    private var searchJob: Job? = null

    init {
        if (mealId != 0L) {
            viewModelScope.launch {
                val meal = mealRepository.getMeal(mealId)
                editedMeal = meal
                _uiState.update { state ->
                    if (meal == null) {
                        state.copy(isLoading = false, isEditing = false)
                    } else {
                        state.copy(isLoading = false, type = meal.type, name = meal.name, kcal = meal.kcal.toString())
                    }
                }
            }
        }
    }

    fun onTypeChange(type: MealType) {
        _uiState.update { it.copy(type = type) }
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name, nameError = null) }
    }

    fun onKcalChange(kcal: String) {
        _uiState.update { it.copy(kcal = kcal, kcalError = null) }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    /** Looks up the current query; a newer search replaces one still in flight. */
    fun search() {
        val service = foodSearch ?: return
        val query = _uiState.value.query.trim()
        if (query.isEmpty()) return
        searchJob?.cancel()
        _uiState.update { it.copy(search = FoodSearchUiState.Loading) }
        searchJob = viewModelScope.launch {
            val search = when (val result = service.search(query)) {
                is FoodSearchResult.Success -> FoodSearchUiState.Results(query, result.items, result.totalCount)
                is FoodSearchResult.Failure -> FoodSearchUiState.Failed(result.error)
            }
            _uiState.update { it.copy(search = search) }
        }
    }

    /** Fills the form from a search result, keeping the typed calories if the food has none. */
    fun onFoodSelected(food: FoodItem) {
        val kcal = food.kcalPerServing?.roundToInt()
        _uiState.update { state ->
            state.copy(
                name = food.name.take(MealValidator.NAME_MAX_LENGTH),
                kcal = kcal?.toString() ?: state.kcal,
                nameError = null,
                kcalError = if (kcal != null) null else state.kcalError,
            )
        }
    }

    fun save() {
        val state = _uiState.value
        if (state.isLoading || state.isSaving || state.isSaved) return
        when (val result = MealValidator.validate(state.name, state.kcal)) {
            is MealValidationResult.Invalid ->
                _uiState.update { it.copy(nameError = result.nameError, kcalError = result.kcalError) }

            is MealValidationResult.Valid -> {
                _uiState.update { it.copy(isSaving = true) }
                viewModelScope.launch {
                    val existing = editedMeal
                    mealRepository.saveMeal(
                        MealEntry(
                            id = existing?.id ?: 0,
                            date = existing?.date ?: date,
                            type = state.type,
                            name = result.name,
                            kcal = result.kcal,
                        ),
                    )
                    _uiState.update { it.copy(isSaving = false, isSaved = true) }
                }
            }
        }
    }
}
