package com.jumincho.beatingyesterday.ui

import android.app.Application
import androidx.annotation.StringRes
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import com.jumincho.beatingyesterday.R
import com.jumincho.beatingyesterday.ui.diet.DietScreen
import com.jumincho.beatingyesterday.ui.focus.FocusScreen
import com.jumincho.beatingyesterday.ui.home.HomeScreen
import com.jumincho.beatingyesterday.ui.navigation.MainScaffold
import com.jumincho.beatingyesterday.ui.navigation.TopLevelDestination
import com.jumincho.beatingyesterday.ui.profile.ProfileScreen
import com.jumincho.beatingyesterday.ui.tasks.TasksScreen
import com.jumincho.beatingyesterday.ui.theme.BeatingYesterdayTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders the main screens with [ScreenshotSamples] on a simulated Pixel 7 and checks that each
 * one shows its key content. With `-PrecordScreenshots`, each test also saves a screenshot to
 * `docs/screenshots`, where the README picks it up.
 *
 * Robolectric needs JUnit 4, so these tests run on the JUnit Vintage engine. SDK 35 is the newest
 * Android version that Robolectric runs on Java 17.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = RobolectricDeviceQualifiers.Pixel7, application = Application::class)
class ScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun home() {
        show(TopLevelDestination.HOME) { modifier ->
            HomeScreen(uiState = ScreenshotSamples.home, onOpenProfile = {}, modifier = modifier)
        }
        assertDisplayed(string(R.string.home_verdict_win))
        capture("home")
    }

    @Test
    fun homeInDarkTheme() {
        show(TopLevelDestination.HOME, darkTheme = true) { modifier ->
            HomeScreen(uiState = ScreenshotSamples.home, onOpenProfile = {}, modifier = modifier)
        }
        assertDisplayed(string(R.string.home_verdict_win))
        capture("home-dark")
    }

    @Test
    fun diet() {
        show(TopLevelDestination.DIET) { modifier ->
            DietScreen(
                uiState = ScreenshotSamples.diet,
                snackbarHostState = remember { SnackbarHostState() },
                onPreviousDay = {},
                onNextDay = {},
                onToday = {},
                onAddMeal = { _, _ -> },
                onEditMeal = {},
                onDeleteMeal = {},
                modifier = modifier,
            )
        }
        assertDisplayed("Bibimbap")
        capture("diet")
    }

    @Test
    fun focus() {
        show(TopLevelDestination.FOCUS) { modifier ->
            FocusScreen(
                uiState = ScreenshotSamples.focus,
                snackbarHostState = remember { SnackbarHostState() },
                onSelectTimer = {},
                onSelectStopwatch = {},
                onSelectCountdown = {},
                onStart = {},
                onPause = {},
                onResume = {},
                onStop = {},
                onDeleteSession = {},
                modifier = modifier,
            )
        }
        assertDisplayed(string(R.string.focus_status_running))
        capture("focus")
    }

    @Test
    fun tasks() {
        show(TopLevelDestination.TASKS) { modifier ->
            TasksScreen(
                uiState = ScreenshotSamples.tasksState,
                snackbarHostState = remember { SnackbarHostState() },
                onAddTask = {},
                onToggle = {},
                onDelete = {},
                modifier = modifier,
            )
        }
        assertDisplayed("Finish the statistics assignment")
        capture("tasks")
    }

    @Test
    fun profile() {
        show(destination = null) { modifier ->
            ProfileScreen(
                uiState = ScreenshotSamples.profile,
                onBack = {},
                onNameChange = {},
                onSexChange = {},
                onBirthYearChange = {},
                onHeightChange = {},
                onWeightChange = {},
                onActivityLevelChange = {},
                onSave = {},
                modifier = modifier,
            )
        }
        assertDisplayed(string(R.string.health_title))
        capture("profile")
    }

    /** Shows [screen] the way the app does: top-level screens above the navigation bar. */
    private fun show(
        destination: TopLevelDestination?,
        darkTheme: Boolean = false,
        screen: @Composable (Modifier) -> Unit,
    ) {
        composeRule.setContent {
            BeatingYesterdayTheme(darkTheme = darkTheme, dynamicColor = false) {
                if (destination == null) {
                    screen(Modifier)
                } else {
                    MainScaffold(currentDestination = destination, onNavigate = {}, content = screen)
                }
            }
        }
    }

    private fun assertDisplayed(text: String) {
        composeRule.onNodeWithText(text).assertIsDisplayed()
    }

    /** Saves the screen to `docs/screenshots/[name].png`; does nothing unless recording. */
    private fun capture(name: String) {
        composeRule.onRoot().captureRoboImage(
            file = File(checkNotNull(System.getProperty("screenshots.dir")), "$name.png"),
            roborazziOptions = RoborazziOptions(recordOptions = RoborazziOptions.RecordOptions(resizeScale = 0.5)),
        )
    }

    private fun string(@StringRes id: Int): String = RuntimeEnvironment.getApplication().getString(id)
}
