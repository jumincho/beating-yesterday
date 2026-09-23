package com.jumincho.beatingyesterday.core.timer

import com.jumincho.beatingyesterday.core.model.FocusSession
import com.jumincho.beatingyesterday.core.testing.FakeFocusSessionRepository
import com.jumincho.beatingyesterday.core.testing.InMemoryTimerStateStore
import com.jumincho.beatingyesterday.core.testing.MutableClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@OptIn(ExperimentalCoroutinesApi::class)
class FocusTimerTest {

    private val clock = MutableClock.at(LocalDateTime.of(2026, 9, 23, 9, 0))
    private val store = InMemoryTimerStateStore()
    private val sessions = FakeFocusSessionRepository()
    private val september23 = LocalDate.of(2026, 9, 23)

    private fun TestScope.createTimer(): FocusTimer =
        FocusTimer(store, sessions, clock, backgroundScope).also { runCurrent() }

    /** Lets [duration] pass on both the wall clock and the coroutine scheduler. */
    private fun TestScope.elapse(duration: Duration) {
        runCurrent()
        clock.advanceBy(duration)
        advanceTimeBy(duration)
        runCurrent()
    }

    private fun Instant.plus(duration: Duration): Instant = plus(duration.toJavaDuration())

    private suspend fun FocusTimer.current(): FocusTimerState = state.first()

    @Test
    fun `starting persists a running session`() = runTest {
        val timer = createTimer()

        timer.start(TimerMode.Stopwatch)

        val expected = FocusTimerState.Running(TimerMode.Stopwatch, startedAt = clock.instant)
        assertEquals(expected, store.state)
        assertEquals(expected, timer.current())
    }

    @Test
    fun `starting is ignored while a session is active`() = runTest {
        val timer = createTimer()
        val startedAt = clock.instant
        timer.start(TimerMode.Stopwatch)
        elapse(1.minutes)

        timer.start(TimerMode.Countdown(25.minutes))

        assertEquals(FocusTimerState.Running(TimerMode.Stopwatch, startedAt), timer.current())
    }

    @Test
    fun `stopping after less than a minute discards the session`() = runTest {
        val timer = createTimer()
        timer.start(TimerMode.Stopwatch)
        elapse(59.seconds)

        assertEquals(StopResult.TooShort(59.seconds), timer.stop())
        assertEquals(emptyList<FocusSession>(), sessions.all)
        assertEquals(FocusTimerState.Idle, store.state)
    }

    @Test
    fun `stopping records the focused time without pauses`() = runTest {
        val timer = createTimer()
        val startedAt = clock.instant
        timer.start(TimerMode.Stopwatch)
        elapse(10.minutes)
        timer.pause()
        elapse(5.minutes)
        timer.resume()
        elapse(15.minutes)

        val result = timer.stop()

        val expected = FocusSession(
            date = september23,
            startedAt = startedAt,
            endedAt = startedAt.plus(30.minutes),
            duration = 25.minutes,
        )
        assertEquals(StopResult.Saved(expected), result)
        assertEquals(listOf(expected.copy(id = 1)), sessions.all)
        assertEquals(FocusTimerState.Idle, timer.current())
    }

    @Test
    fun `stopping without a session does nothing`() = runTest {
        assertEquals(StopResult.NotRunning, createTimer().stop())
    }

    @Test
    fun `a countdown records itself when it reaches zero`() = runTest {
        val timer = createTimer()
        val completed = mutableListOf<FocusSession>()
        backgroundScope.launch { timer.completedSessions.collect { completed += it } }
        val startedAt = clock.instant
        timer.start(TimerMode.Countdown(25.minutes))

        elapse(24.minutes)
        assertEquals(emptyList<FocusSession>(), sessions.all)

        elapse(1.minutes)
        val expected =
            FocusSession(
                date = september23,
                startedAt = startedAt,
                endedAt = startedAt.plus(25.minutes),
                duration = 25.minutes,
            )
        assertEquals(listOf(expected.copy(id = 1)), sessions.all)
        assertEquals(listOf(expected), completed)
        assertEquals(FocusTimerState.Idle, timer.current())
    }

    @Test
    fun `a paused countdown does not complete`() = runTest {
        val timer = createTimer()
        timer.start(TimerMode.Countdown(25.minutes))
        elapse(20.minutes)
        timer.pause()

        elapse(1.hours)

        assertEquals(emptyList<FocusSession>(), sessions.all)
        assertEquals(5.minutes, (timer.current() as FocusTimerState.Paused).remainingAt(clock.instant))
    }

    @Test
    fun `a countdown that finished while the app was not running is recorded on restore`() = runTest {
        val startedAt = clock.instant.minusSeconds(3_600)
        store.state = FocusTimerState.Running(TimerMode.Countdown(25.minutes), startedAt)

        val timer = createTimer()

        assertEquals(FocusTimerState.Idle, timer.current())
        assertEquals(
            listOf(FocusSession(1, september23, startedAt, startedAt.plus(25.minutes), 25.minutes)),
            sessions.all,
        )
    }

    @Test
    fun `a running stopwatch is restored with its original start time`() = runTest {
        store.state = FocusTimerState.Running(TimerMode.Stopwatch, startedAt = clock.instant.minusSeconds(40 * 60))

        val timer = createTimer()

        assertEquals(40.minutes, timer.current().elapsedAt(clock.instant))
    }

    @Test
    fun `an overdue countdown is recorded as completed before any other action`() = runTest {
        val timer = createTimer()
        val startedAt = clock.instant
        timer.start(TimerMode.Countdown(25.minutes))
        runCurrent()
        clock.advanceBy(30.minutes)

        timer.pause()

        assertEquals(FocusTimerState.Idle, timer.current())
        assertEquals(
            listOf(FocusSession(1, september23, startedAt, startedAt.plus(25.minutes), 25.minutes)),
            sessions.all,
        )
    }

    @Test
    fun `a session crossing midnight counts towards the day it started`() = runTest {
        clock.instant = LocalDateTime.of(2026, 9, 23, 23, 50).atZone(clock.zone).toInstant()
        val timer = createTimer()
        timer.start(TimerMode.Stopwatch)
        elapse(30.minutes)

        val result = timer.stop()

        assertEquals(september23, (result as StopResult.Saved).session.date)
    }
}
