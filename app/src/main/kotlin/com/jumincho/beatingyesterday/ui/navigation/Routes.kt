package com.jumincho.beatingyesterday.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.jumincho.beatingyesterday.R
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object DietRoute

@Serializable
data object FocusRoute

@Serializable
data object TasksRoute

@Serializable
data object ProfileRoute

/**
 * The meal editor.
 *
 * @property epochDay the day a new meal is logged for.
 * @property mealType name of the pre-selected [com.jumincho.beatingyesterday.core.model.MealType].
 * @property mealId the meal to edit, or `0` to add a new one.
 */
@Serializable
data class MealEditorRoute(val epochDay: Long, val mealType: String, val mealId: Long = 0L)

/** The destinations of the bottom navigation bar, in display order. */
enum class TopLevelDestination(
    val route: Any,
    @get:StringRes val label: Int,
    @get:DrawableRes val icon: Int,
    @get:DrawableRes val selectedIcon: Int,
) {
    HOME(HomeRoute, R.string.nav_home, R.drawable.ic_home, R.drawable.ic_home_filled),
    DIET(DietRoute, R.string.nav_diet, R.drawable.ic_restaurant, R.drawable.ic_restaurant),
    FOCUS(FocusRoute, R.string.nav_focus, R.drawable.ic_timer, R.drawable.ic_timer_filled),
    TASKS(TasksRoute, R.string.nav_tasks, R.drawable.ic_task_alt, R.drawable.ic_task_alt),
}
