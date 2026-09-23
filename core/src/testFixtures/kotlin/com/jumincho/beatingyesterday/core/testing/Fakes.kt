package com.jumincho.beatingyesterday.core.testing

import com.jumincho.beatingyesterday.core.data.FocusSessionRepository
import com.jumincho.beatingyesterday.core.data.MealRepository
import com.jumincho.beatingyesterday.core.data.ProfileRepository
import com.jumincho.beatingyesterday.core.data.TodoRepository
import com.jumincho.beatingyesterday.core.food.FoodSearchResult
import com.jumincho.beatingyesterday.core.food.FoodSearchService
import com.jumincho.beatingyesterday.core.model.FocusSession
import com.jumincho.beatingyesterday.core.model.MealEntry
import com.jumincho.beatingyesterday.core.model.TodoItem
import com.jumincho.beatingyesterday.core.model.UserProfile
import com.jumincho.beatingyesterday.core.timer.FocusTimerState
import com.jumincho.beatingyesterday.core.timer.TimerStateStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import kotlin.time.Duration

/** In-memory [ProfileRepository]. */
class FakeProfileRepository(initial: UserProfile? = null) : ProfileRepository {
    private val state = MutableStateFlow(initial)

    override val profile: Flow<UserProfile?> = state

    /** The currently saved profile. */
    val saved: UserProfile? get() = state.value

    override suspend fun saveProfile(profile: UserProfile) {
        state.value = profile
    }
}

/** In-memory [MealRepository] that assigns ids like an auto-increment column. */
class FakeMealRepository(initial: List<MealEntry> = emptyList()) : MealRepository {
    private val meals = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0) + 1

    /** Every stored meal. */
    val all: List<MealEntry> get() = meals.value

    override fun observeMeals(date: LocalDate): Flow<List<MealEntry>> =
        meals.map { list -> list.filter { it.date == date }.sortedBy { it.id } }

    override fun observeDailyIntake(): Flow<Map<LocalDate, Int>> =
        meals.map { list -> list.groupBy { it.date }.mapValues { (_, day) -> day.sumOf { it.kcal } } }

    override suspend fun getMeal(id: Long): MealEntry? = meals.value.firstOrNull { it.id == id }

    override suspend fun saveMeal(meal: MealEntry) {
        val stored = if (meal.id == 0L) meal.copy(id = nextId++) else meal
        meals.update { list -> list.filterNot { it.id == stored.id } + stored }
    }

    override suspend fun deleteMeal(id: Long) {
        meals.update { list -> list.filterNot { it.id == id } }
    }
}

/** In-memory [FocusSessionRepository] that assigns ids like an auto-increment column. */
class FakeFocusSessionRepository(initial: List<FocusSession> = emptyList()) : FocusSessionRepository {
    private val sessions = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0) + 1

    /** Every stored session. */
    val all: List<FocusSession> get() = sessions.value

    override fun observeSessions(date: LocalDate): Flow<List<FocusSession>> =
        sessions.map { list -> list.filter { it.date == date }.sortedBy { it.startedAt } }

    override fun observeDailyFocus(): Flow<Map<LocalDate, Duration>> = sessions.map { list ->
        list.groupBy { it.date }.mapValues { (_, day) -> day.fold(Duration.ZERO) { sum, s -> sum + s.duration } }
    }

    override suspend fun saveSession(session: FocusSession) {
        val stored = if (session.id == 0L) session.copy(id = nextId++) else session
        sessions.update { list -> list.filterNot { it.id == stored.id } + stored }
    }

    override suspend fun deleteSession(id: Long) {
        sessions.update { list -> list.filterNot { it.id == id } }
    }
}

/** In-memory [TodoRepository] with the same carry-over rules as the real one. */
class FakeTodoRepository(initial: List<TodoItem> = emptyList()) : TodoRepository {
    private val todos = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0) + 1

    /** Every stored task. */
    val all: List<TodoItem> get() = todos.value

    override fun observeTodos(date: LocalDate): Flow<List<TodoItem>> = todos.map { list ->
        val open = list.filter { it.completedOn == null && !it.createdOn.isAfter(date) }.sortedBy { it.id }
        val done = list.filter { it.completedOn == date }.sortedBy { it.id }
        open + done
    }

    override fun observeDailyCompletions(): Flow<Map<LocalDate, Int>> = todos.map { list ->
        list.mapNotNull { it.completedOn }.groupingBy { it }.eachCount()
    }

    override suspend fun saveTodo(item: TodoItem) {
        val stored = if (item.id == 0L) item.copy(id = nextId++) else item
        todos.update { list -> list.filterNot { it.id == stored.id } + stored }
    }

    override suspend fun setCompleted(id: Long, completedOn: LocalDate?) {
        todos.update { list -> list.map { if (it.id == id) it.copy(completedOn = completedOn) else it } }
    }

    override suspend fun deleteTodo(id: Long) {
        todos.update { list -> list.filterNot { it.id == id } }
    }
}

/** In-memory [TimerStateStore]. */
class InMemoryTimerStateStore(var state: FocusTimerState = FocusTimerState.Idle) : TimerStateStore {
    override suspend fun read(): FocusTimerState = state

    override suspend fun write(state: FocusTimerState) {
        this.state = state
    }
}

/**
 * Scriptable [FoodSearchService]. Set [result] to choose the answer; set [gate] to hold searches
 * in flight until it is completed.
 */
class FakeFoodSearchService(var result: FoodSearchResult = FoodSearchResult.Success(emptyList(), 0)) :
    FoodSearchService {

    /** Every query received, in order. */
    val queries = mutableListOf<String>()

    /** When set, searches suspend until it completes. */
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun search(query: String): FoodSearchResult {
        queries += query
        gate?.await()
        return result
    }
}
