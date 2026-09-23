package com.jumincho.beatingyesterday.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jumincho.beatingyesterday.BeatingYesterdayApplication
import com.jumincho.beatingyesterday.core.model.MealType
import com.jumincho.beatingyesterday.di.AppContainer
import com.jumincho.beatingyesterday.ui.diet.DietViewModel
import com.jumincho.beatingyesterday.ui.diet.MealEditorViewModel
import com.jumincho.beatingyesterday.ui.focus.FocusViewModel
import com.jumincho.beatingyesterday.ui.home.HomeViewModel
import com.jumincho.beatingyesterday.ui.navigation.MealEditorRoute
import com.jumincho.beatingyesterday.ui.profile.ProfileViewModel
import com.jumincho.beatingyesterday.ui.tasks.TasksViewModel
import java.time.LocalDate

/** ViewModel factories wired to the [AppContainer] of the running application. */
object AppViewModelProvider {

    /** Factory for every ViewModel without navigation arguments. */
    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer { MainViewModel(container().profileRepository) }
        initializer {
            val container = container()
            ProfileViewModel(container.profileRepository, container.clock)
        }
        initializer {
            val container = container()
            HomeViewModel(
                profileRepository = container.profileRepository,
                mealRepository = container.mealRepository,
                focusSessionRepository = container.focusSessionRepository,
                todoRepository = container.todoRepository,
                today = container.today,
            )
        }
        initializer {
            val container = container()
            DietViewModel(container.profileRepository, container.mealRepository, container.clock, container.today)
        }
        initializer {
            val container = container()
            FocusViewModel(container.focusTimer, container.focusSessionRepository, container.clock, container.today)
        }
        initializer {
            val container = container()
            TasksViewModel(container.todoRepository, container.today)
        }
    }

    /** Factory for the meal editor opened with [route]. */
    fun mealEditor(route: MealEditorRoute): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            val container = container()
            MealEditorViewModel(
                mealId = route.mealId,
                date = LocalDate.ofEpochDay(route.epochDay),
                initialType = MealType.entries.firstOrNull { it.name == route.mealType } ?: MealType.BREAKFAST,
                mealRepository = container.mealRepository,
                foodSearch = container.foodSearchService,
            )
        }
    }

    private fun CreationExtras.container(): AppContainer {
        val application = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
        return (application as BeatingYesterdayApplication).container
    }
}
