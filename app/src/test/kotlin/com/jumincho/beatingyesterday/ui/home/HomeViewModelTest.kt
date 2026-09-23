package com.jumincho.beatingyesterday.ui.home

import com.jumincho.beatingyesterday.core.contest.RoundOutcome
import com.jumincho.beatingyesterday.core.contest.Verdict
import com.jumincho.beatingyesterday.core.health.HealthCalculator
import com.jumincho.beatingyesterday.core.model.ActivityLevel
import com.jumincho.beatingyesterday.core.model.FocusSession
import com.jumincho.beatingyesterday.core.model.MealEntry
import com.jumincho.beatingyesterday.core.model.MealType
import com.jumincho.beatingyesterday.core.model.Sex
import com.jumincho.beatingyesterday.core.model.TodoItem
import com.jumincho.beatingyesterday.core.model.UserProfile
import com.jumincho.beatingyesterday.core.testing.FakeFocusSessionRepository
import com.jumincho.beatingyesterday.core.testing.FakeMealRepository
import com.jumincho.beatingyesterday.core.testing.FakeProfileRepository
import com.jumincho.beatingyesterday.core.testing.FakeTodoRepository
import com.jumincho.beatingyesterday.testing.MainDispatcherExtension
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.LocalDate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val september23 = LocalDate.of(2026, 9, 23)
    private val profile = UserProfile("Minji", Sex.FEMALE, 1998, 162.5, 54.0, ActivityLevel.LIGHT)

    private fun session(date: LocalDate, duration: Duration): FocusSession {
        val start = date.atTime(9, 0).atZone(java.time.ZoneId.of("Asia/Seoul")).toInstant()
        return FocusSession(
            date = date,
            startedAt = start,
            endedAt = start.plus(duration.toJavaDuration()),
            duration = duration,
        )
    }

    private fun TestScope.readyState(viewModel: HomeViewModel): HomeUiState.Ready {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return assertInstanceOf<HomeUiState.Ready>(viewModel.uiState.value)
    }

    @Test
    fun `judges today against yesterday using every repository`() = runTest {
        val target = HealthCalculator.metricsFor(profile, september23).dailyTargetKcal
        val viewModel = HomeViewModel(
            profileRepository = FakeProfileRepository(profile),
            mealRepository = FakeMealRepository(
                listOf(
                    MealEntry(1, september23, MealType.LUNCH, "Bibimbap", target),
                    MealEntry(2, september23.minusDays(1), MealType.DINNER, "Pizza", target + 800),
                ),
            ),
            focusSessionRepository = FakeFocusSessionRepository(listOf(session(september23, 30.minutes))),
            todoRepository = FakeTodoRepository(
                listOf(TodoItem(1, "Read", september23.minusDays(1), completedOn = september23.minusDays(1))),
            ),
            today = MutableStateFlow(september23),
        )

        val state = readyState(viewModel)

        assertEquals("Minji", state.userName)
        assertEquals(target, state.scoreboard.today.dailyTargetKcal)
        assertEquals(
            listOf(RoundOutcome.WON, RoundOutcome.WON, RoundOutcome.LOST),
            state.scoreboard.today.rounds.map { it.outcome },
        )
        assertEquals(Verdict.WIN, state.scoreboard.today.verdict)
    }

    @Test
    fun `rolls over to the new day automatically`() = runTest {
        val today = MutableStateFlow(september23)
        val viewModel = HomeViewModel(
            FakeProfileRepository(profile),
            FakeMealRepository(),
            FakeFocusSessionRepository(listOf(session(september23, 30.minutes))),
            FakeTodoRepository(),
            today,
        )
        assertEquals(Verdict.WIN, readyState(viewModel).scoreboard.today.verdict)

        today.value = september23.plusDays(1)

        val next = assertInstanceOf<HomeUiState.Ready>(viewModel.uiState.value)
        assertEquals(september23.plusDays(1), next.today)
        assertEquals(Verdict.LOSE, next.scoreboard.today.verdict)
        assertEquals(1, next.scoreboard.streakDays)
    }

    @Test
    fun `without a profile the diet round has no data`() = runTest {
        val viewModel = HomeViewModel(
            FakeProfileRepository(),
            FakeMealRepository(
                listOf(
                    MealEntry(1, september23, MealType.LUNCH, "Bibimbap", 600),
                    MealEntry(2, september23.minusDays(1), MealType.LUNCH, "Ramen", 500),
                ),
            ),
            FakeFocusSessionRepository(),
            FakeTodoRepository(),
            MutableStateFlow(september23),
        )

        val state = readyState(viewModel)

        assertEquals("", state.userName)
        assertEquals(RoundOutcome.NO_DATA, state.scoreboard.today.rounds.first().outcome)
    }
}
