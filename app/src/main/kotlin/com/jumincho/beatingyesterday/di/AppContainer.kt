package com.jumincho.beatingyesterday.di

import android.content.Context
import com.jumincho.beatingyesterday.BuildConfig
import com.jumincho.beatingyesterday.core.data.FocusSessionRepository
import com.jumincho.beatingyesterday.core.data.MealRepository
import com.jumincho.beatingyesterday.core.data.ProfileRepository
import com.jumincho.beatingyesterday.core.data.TodoRepository
import com.jumincho.beatingyesterday.core.food.FoodSafetyKoreaClient
import com.jumincho.beatingyesterday.core.food.FoodSearchService
import com.jumincho.beatingyesterday.core.time.DeviceClock
import com.jumincho.beatingyesterday.core.time.observeToday
import com.jumincho.beatingyesterday.core.timer.FocusTimer
import com.jumincho.beatingyesterday.data.RoomFocusSessionRepository
import com.jumincho.beatingyesterday.data.RoomMealRepository
import com.jumincho.beatingyesterday.data.RoomTodoRepository
import com.jumincho.beatingyesterday.data.local.AppDatabase
import com.jumincho.beatingyesterday.data.preferences.DataStoreProfileRepository
import com.jumincho.beatingyesterday.data.preferences.DataStoreTimerStateStore
import com.jumincho.beatingyesterday.data.preferences.focusTimerDataStore
import com.jumincho.beatingyesterday.data.preferences.profileDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import java.time.LocalDate

/**
 * Manual dependency injection: creates every app-wide dependency once. Lives as long as the
 * [android.app.Application].
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val database by lazy { AppDatabase.create(appContext) }

    /** Wall clock in the device's current time zone. */
    val clock: Clock = DeviceClock

    /** The local date, re-emitted when the day changes. */
    val today: Flow<LocalDate> = clock.observeToday()

    val profileRepository: ProfileRepository by lazy { DataStoreProfileRepository(appContext.profileDataStore) }

    val mealRepository: MealRepository by lazy { RoomMealRepository(database.mealDao()) }

    val focusSessionRepository: FocusSessionRepository by lazy {
        RoomFocusSessionRepository(database.focusSessionDao())
    }

    val todoRepository: TodoRepository by lazy { RoomTodoRepository(database.todoDao()) }

    /**
     * Created eagerly, so a countdown that reached zero while the app was not running is recorded
     * as soon as the process starts.
     */
    val focusTimer: FocusTimer = FocusTimer(
        store = DataStoreTimerStateStore(appContext.focusTimerDataStore),
        sessions = focusSessionRepository,
        clock = clock,
        scope = applicationScope,
    )

    /** Food lookup, or `null` when the build has no `FOOD_API_KEY`. */
    val foodSearchService: FoodSearchService? by lazy {
        BuildConfig.FOOD_API_KEY.takeIf { it.isNotBlank() }?.let { FoodSafetyKoreaClient(apiKey = it) }
    }
}
