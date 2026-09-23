package com.jumincho.beatingyesterday.ui.focus

import com.jumincho.beatingyesterday.core.model.FocusSession
import com.jumincho.beatingyesterday.core.testing.FakeFocusSessionRepository
import com.jumincho.beatingyesterday.core.testing.InMemoryTimerStateStore
import com.jumincho.beatingyesterday.core.testing.MutableClock
import com.jumincho.beatingyesterday.core.timer.FocusTimer
import com.jumincho.beatingyesterday.core.timer.TimerMode
import com.jumincho.beatingyesterday.testing.MainDispatcherExtension
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@OptIn(ExperimentalCoroutinesApi::class)
class FocusViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val clock = MutableClock.at(LocalDateTime.of(2026, 9, 23, 9, 0))
    private val today = LocalDate.of(2026, 9, 23)
    private val sessions = FakeFocusSessionRepository()

    private fun TestScope.createViewModel(): FocusViewModel {
        val timer = FocusTimer(InMemoryTimerStateStore(), sessions, clock, backgroundScope)
        return FocusViewModel(timer, sessions, clock, MutableStateFlow(today)).also { viewModel ->
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
            runCurrent()
        }
    }

    /** Lets [duration] pass on both the wall clock and the coroutine scheduler. */
    private fun TestScope.elapse(duration: Duration) {
        clock.advanceBy(duration)
        advanceTimeBy(duration)
        runCurrent()
    }

    @Test
    fun `offers a full 25-minute countdown when idle`() = runTest {
        val state = createViewModel().uiState.value

        assertFalse(state.isLoading)
        assertEquals(TimerStatus.IDLE, state.status)
        assertEquals(TimerMode.Countdown(25.minutes), state.mode)
        assertEquals(25.minutes, state.remaining)
        assertEquals(1f, state.progress)
    }

    @Test
    fun `a running countdown follows the clock and is saved when stopped`() = runTest {
        val viewModel = createViewModel()
        viewModel.start()
        runCurrent()

        elapse(10.minutes)
        val running = viewModel.uiState.value
        assertEquals(TimerStatus.RUNNING, running.status)
        assertEquals(15.minutes, running.remaining)
        assertEquals(0.6f, running.progress, 0.0001f)

        viewModel.stop()
        runCurrent()

        val stopped = viewModel.uiState.value
        assertEquals(FocusMessage.Saved(10.minutes), viewModel.message.value)
        assertEquals(TimerStatus.IDLE, stopped.status)
        assertEquals(10.minutes, stopped.todayTotal)
        assertEquals(LocalTime.of(9, 0), stopped.sessions.single().start)
        assertEquals(LocalTime.of(9, 10), stopped.sessions.single().end)
    }

    @Test
    fun `stopping within a minute saves nothing`() = runTest {
        val viewModel = createViewModel()
        viewModel.start()
        runCurrent()
        elapse(30.seconds)

        viewModel.stop()
        runCurrent()

        assertEquals(FocusMessage.TooShort, viewModel.message.value)
        assertEquals(emptyList<FocusSession>(), sessions.all)
        viewModel.onMessageShown()
        assertEquals(null, viewModel.message.value)
    }

    @Test
    fun `announces a countdown that reaches zero`() = runTest {
        val viewModel = createViewModel()
        viewModel.selectCountdown(50)
        viewModel.start()
        runCurrent()

        elapse(50.minutes)

        assertEquals(FocusMessage.Completed(50.minutes), viewModel.message.value)
        assertEquals(50.minutes, viewModel.uiState.value.todayTotal)
        assertEquals(TimerStatus.IDLE, viewModel.uiState.value.status)
    }

    @Test
    fun `switching to the stopwatch and back keeps the chosen countdown`() = runTest {
        val viewModel = createViewModel()

        viewModel.selectCountdown(45)
        viewModel.selectStopwatch()
        assertEquals(TimerMode.Stopwatch, viewModel.uiState.value.mode)
        viewModel.selectTimer()
        assertEquals(TimerMode.Countdown(45.minutes), viewModel.uiState.value.mode)

        viewModel.selectCountdown(500)
        assertEquals(TimerMode.Countdown(3.hours), viewModel.uiState.value.mode)
    }

    @Test
    fun `deleting a session can be undone`() = runTest {
        val start = clock.instant.minusSeconds(3_600)
        val session = FocusSession(1, today, start, start.plus(25.minutes.toJavaDuration()), 25.minutes)
        sessions.saveSession(session)
        val viewModel = createViewModel()

        viewModel.deleteSession(session)
        runCurrent()
        assertEquals(FocusMessage.SessionDeleted, viewModel.message.value)
        assertEquals(emptyList<FocusSession>(), sessions.all)

        viewModel.undoDeleteSession()
        runCurrent()
        assertEquals(listOf(session), sessions.all)
    }
}
