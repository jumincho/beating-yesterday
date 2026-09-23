package com.jumincho.beatingyesterday.core.data

import com.jumincho.beatingyesterday.core.contest.DailyTotals
import com.jumincho.beatingyesterday.core.model.FocusSession
import com.jumincho.beatingyesterday.core.model.MealEntry
import com.jumincho.beatingyesterday.core.model.TodoItem
import com.jumincho.beatingyesterday.core.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import kotlin.time.Duration

/** Stores the single user profile. */
interface ProfileRepository {
    /** The saved profile, or `null` until onboarding has been completed. */
    val profile: Flow<UserProfile?>

    /** Saves [profile], replacing any previous one. */
    suspend fun saveProfile(profile: UserProfile)
}

/** Stores logged meals. */
interface MealRepository {
    /** Meals eaten on [date], in the order they were logged. */
    fun observeMeals(date: LocalDate): Flow<List<MealEntry>>

    /** Total calories per day, for days with at least one meal. */
    fun observeDailyIntake(): Flow<Map<LocalDate, Int>>

    /** The meal with [id], or `null` if it does not exist. */
    suspend fun getMeal(id: Long): MealEntry?

    /** Inserts [meal] when its id is `0`, otherwise inserts or replaces the meal with that id. */
    suspend fun saveMeal(meal: MealEntry)

    /** Deletes the meal with [id], if any. */
    suspend fun deleteMeal(id: Long)
}

/** Stores finished focus sessions. */
interface FocusSessionRepository {
    /** Sessions that started on [date], oldest first. */
    fun observeSessions(date: LocalDate): Flow<List<FocusSession>>

    /** Total focused time per day, for days with at least one session. */
    fun observeDailyFocus(): Flow<Map<LocalDate, Duration>>

    /** Inserts [session] when its id is `0`, otherwise inserts or replaces the session with that id. */
    suspend fun saveSession(session: FocusSession)

    /** Deletes the session with [id], if any. */
    suspend fun deleteSession(id: Long)
}

/** Stores tasks. See [TodoItem] for how unfinished tasks carry over. */
interface TodoRepository {
    /**
     * The task list for [date]: tasks still open that were created on or before [date], followed by
     * the tasks completed on [date], each group oldest first.
     */
    fun observeTodos(date: LocalDate): Flow<List<TodoItem>>

    /** Number of tasks completed per day, for days with at least one completion. */
    fun observeDailyCompletions(): Flow<Map<LocalDate, Int>>

    /** Inserts [item] when its id is `0`, otherwise inserts or replaces the task with that id. */
    suspend fun saveTodo(item: TodoItem)

    /** Marks the task with [id] as completed on [completedOn], or as open again when `null`. */
    suspend fun setCompleted(id: Long, completedOn: LocalDate?)

    /** Deletes the task with [id], if any. */
    suspend fun deleteTodo(id: Long)
}

/** Combines the per-day aggregates of all three repositories into [DailyTotals]. */
fun observeDailyTotals(
    meals: MealRepository,
    focusSessions: FocusSessionRepository,
    todos: TodoRepository,
): Flow<DailyTotals> = combine(
    meals.observeDailyIntake(),
    focusSessions.observeDailyFocus(),
    todos.observeDailyCompletions(),
) { intake, focus, completions -> DailyTotals(intake, focus, completions) }
