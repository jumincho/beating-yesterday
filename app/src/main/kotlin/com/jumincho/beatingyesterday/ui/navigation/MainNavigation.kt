package com.jumincho.beatingyesterday.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.jumincho.beatingyesterday.ui.AppViewModelProvider
import com.jumincho.beatingyesterday.ui.diet.DietScreen
import com.jumincho.beatingyesterday.ui.diet.MealEditorScreen
import com.jumincho.beatingyesterday.ui.focus.FocusScreen
import com.jumincho.beatingyesterday.ui.home.HomeScreen
import com.jumincho.beatingyesterday.ui.profile.ProfileScreen
import com.jumincho.beatingyesterday.ui.tasks.TasksScreen

/** The main app: bottom navigation between Home, Diet, Focus and Tasks, plus detail screens. */
@Composable
fun MainNavigation(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()

    MainScaffold(
        currentDestination = backStackEntry?.destination?.topLevelDestination(),
        onNavigate = { destination -> navController.navigateToTopLevel(destination) },
    ) { modifier ->
        NavHost(navController = navController, startDestination = HomeRoute, modifier = modifier) {
            composable<HomeRoute> {
                HomeScreen(onOpenProfile = dropUnlessResumed { navController.navigate(ProfileRoute) })
            }
            composable<DietRoute> {
                DietScreen(
                    onAddMeal = { date, type ->
                        navController.navigate(MealEditorRoute(epochDay = date.toEpochDay(), mealType = type.name)) {
                            launchSingleTop = true
                        }
                    },
                    onEditMeal = { meal ->
                        navController.navigate(
                            MealEditorRoute(
                                epochDay = meal.date.toEpochDay(),
                                mealType = meal.type.name,
                                mealId = meal.id,
                            ),
                        ) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable<MealEditorRoute> { entry ->
                val route = entry.toRoute<MealEditorRoute>()
                MealEditorScreen(
                    viewModel = viewModel(factory = AppViewModelProvider.mealEditor(route)),
                    onClose = dropUnlessResumed { navController.popBackStack() },
                )
            }
            composable<FocusRoute> { FocusScreen() }
            composable<TasksRoute> { TasksScreen() }
            composable<ProfileRoute> {
                ProfileScreen(onBack = dropUnlessResumed { navController.popBackStack() })
            }
        }
    }
}

/**
 * The app frame: [content] above the bottom navigation bar, which is shown only while one of the
 * top-level destinations is current.
 *
 * @param content receives the modifier that keeps it clear of the navigation bar.
 */
@Composable
internal fun MainScaffold(
    currentDestination: TopLevelDestination?,
    onNavigate: (TopLevelDestination) -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    Scaffold(
        bottomBar = {
            if (currentDestination != null) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { destination ->
                        val selected = destination == currentDestination
                        NavigationBarItem(
                            selected = selected,
                            onClick = { onNavigate(destination) },
                            icon = {
                                Icon(
                                    painter = painterResource(
                                        if (selected) destination.selectedIcon else destination.icon,
                                    ),
                                    contentDescription = null,
                                )
                            },
                            label = { Text(stringResource(destination.label)) },
                        )
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        content(
            Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        )
    }
}

private fun NavDestination.topLevelDestination(): TopLevelDestination? =
    TopLevelDestination.entries.firstOrNull { destination ->
        hierarchy.any { it.hasRoute(destination.route::class) }
    }

private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo<HomeRoute> { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
