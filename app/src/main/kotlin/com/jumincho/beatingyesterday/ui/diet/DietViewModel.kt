package com.jumincho.beatingyesterday.ui.diet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jumincho.beatingyesterday.core.data.MealRepository
import com.jumincho.beatingyesterday.core.data.ProfileRepository
import com.jumincho.beatingyesterday.core.health.HealthCalculator
import com.jumincho.beatingyesterday.core.health.HealthMetrics
import com.jumincho.beatingyesterday.core.model.MealEntry
import com.jumincho.beatingyesterday.core.model.MealType
import com.jumincho.beatingyesterday.ui.WhileUiSubscribed
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime

/** The meals of one [type]. */
data class MealSection(val type: MealType, val meals: List<MealEntry>) {
    val totalKcal: Int get() = meals.sumOf { it.kcal }
}

/** State of the Diet screen. */
sealed interface DietUiState {
    data object Loading : DietUiState

    /**
     * @property daysAgo how many days before today [date] is: 0 for today, 1 for yesterday.
     * @property sections one section per [MealType], in display order, possibly empty.
     * @property metrics health numbers from the profile, including the daily target.
     * @property suggestedType the meal type offered first when adding a meal.
     */
    data class Ready(
        val date: LocalDate,
        val daysAgo: Int,
        val sections: List<MealSection>,
        val metrics: HealthMetrics?,
        val suggestedType: MealType,
    ) : DietUiState {
        val isToday: Boolean get() = daysAgo == 0
        val totalKcal: Int get() = sections.sumOf { it.totalKcal }
        val hasMeals: Boolean get() = sections.any { it.meals.isNotEmpty() }
    }
}

/** Shows the meals of today or an earlier day against the daily calorie target. */
@OptIn(ExperimentalCoroutinesApi::class)
class DietViewModel(
    private val profileRepository: ProfileRepository,
    private val mealRepository: MealRepository,
    private val clock: Clock,
    today: Flow<LocalDate>,
) : ViewModel() {

    /** 0 for today, -1 for yesterday and so on. */
    private val dayOffset = MutableStateFlow(0)

    private val _deletedMeal = MutableStateFlow<MealEntry?>(null)

    /** The meal just deleted, offered for undo until [onDeletedMessageShown]. */
    val deletedMeal: StateFlow<MealEntry?> = _deletedMeal.asStateFlow()

    val uiState: StateFlow<DietUiState> = combine(today, dayOffset) { date, offset -> date to offset }
        .flatMapLatest { (currentDate, offset) ->
            val date = currentDate.plusDays(offset.toLong())
            combine(mealRepository.observeMeals(date), profileRepository.profile) { meals, profile ->
                DietUiState.Ready(
                    date = date,
                    daysAgo = -offset,
                    sections = MealType.entries.map { type -> MealSection(type, meals.filter { it.type == type }) },
                    metrics = profile?.let { HealthCalculator.metricsFor(it, date) },
                    suggestedType = if (offset ==
                        0
                    ) {
                        MealType.suggestedFor(LocalTime.now(clock))
                    } else {
                        MealType.BREAKFAST
                    },
                )
            }
        }
        .stateIn(viewModelScope, WhileUiSubscribed, DietUiState.Loading)

    fun showPreviousDay() {
        dayOffset.update { it - 1 }
    }

    fun showNextDay() {
        dayOffset.update { minOf(it + 1, 0) }
    }

    fun showToday() {
        dayOffset.value = 0
    }

    fun deleteMeal(meal: MealEntry) {
        viewModelScope.launch {
            mealRepository.deleteMeal(meal.id)
            _deletedMeal.value = meal
        }
    }

    /** Restores the meal deleted last. */
    fun undoDelete() {
        val meal = _deletedMeal.value ?: return
        _deletedMeal.value = null
        viewModelScope.launch { mealRepository.saveMeal(meal) }
    }

    fun onDeletedMessageShown() {
        _deletedMeal.value = null
    }
}
