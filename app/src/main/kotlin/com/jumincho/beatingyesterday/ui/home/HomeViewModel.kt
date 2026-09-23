package com.jumincho.beatingyesterday.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jumincho.beatingyesterday.core.contest.Scoreboard
import com.jumincho.beatingyesterday.core.data.FocusSessionRepository
import com.jumincho.beatingyesterday.core.data.MealRepository
import com.jumincho.beatingyesterday.core.data.ProfileRepository
import com.jumincho.beatingyesterday.core.data.TodoRepository
import com.jumincho.beatingyesterday.core.data.observeDailyTotals
import com.jumincho.beatingyesterday.core.health.HealthCalculator
import com.jumincho.beatingyesterday.ui.WhileUiSubscribed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

/** State of the Home scoreboard. */
sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Ready(val userName: String, val today: LocalDate, val scoreboard: Scoreboard) : HomeUiState
}

/** Compares today with yesterday. The day rolls over automatically with [today]. */
class HomeViewModel(
    profileRepository: ProfileRepository,
    mealRepository: MealRepository,
    focusSessionRepository: FocusSessionRepository,
    todoRepository: TodoRepository,
    today: Flow<LocalDate>,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        profileRepository.profile,
        today,
        observeDailyTotals(mealRepository, focusSessionRepository, todoRepository),
    ) { profile, date, totals ->
        val dailyTarget = profile?.let { HealthCalculator.metricsFor(it, date).dailyTargetKcal }
        HomeUiState.Ready(
            userName = profile?.name.orEmpty(),
            today = date,
            scoreboard = Scoreboard.calculate(date, totals, dailyTarget),
        )
    }.stateIn(viewModelScope, WhileUiSubscribed, HomeUiState.Loading)
}
