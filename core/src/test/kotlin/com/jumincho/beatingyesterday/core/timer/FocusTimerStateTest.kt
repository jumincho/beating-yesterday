package com.jumincho.beatingyesterday.core.timer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class FocusTimerStateTest {

    private val start = Instant.parse("2026-09-23T01:00:00Z")
    private val countdown = TimerMode.Countdown(25.minutes)

    private fun at(offset: Duration): Instant = start.plus(offset.toJavaDuration())

    @Test
    fun `idle has no elapsed time`() {
        assertEquals(Duration.ZERO, FocusTimerState.Idle.elapsedAt(start))
    }

    @Test
    fun `running time is derived from timestamps`() {
        val running = FocusTimerState.Running(TimerMode.Stopwatch, startedAt = start)

        assertEquals(90.minutes, running.elapsedAt(at(90.minutes)))
        assertNull(running.remainingAt(at(90.minutes)))
        assertNull(running.completesAt())
    }

    @Test
    fun `a clock that moves backwards never produces negative time`() {
        val running = FocusTimerState.Running(TimerMode.Stopwatch, startedAt = start)

        assertEquals(Duration.ZERO, running.elapsedAt(at((-5).minutes)))
    }

    @Test
    fun `pausing freezes the elapsed time and resuming continues from it`() {
        val paused = FocusTimerState.Running(countdown, startedAt = start).pause(at(10.minutes))

        assertEquals(10.minutes, paused.elapsedAt(at(2.hours)))

        val resumed = paused.resume(at(15.minutes))
        assertEquals(
            FocusTimerState.Running(countdown, start, resumedAt = at(15.minutes), accumulated = 10.minutes),
            resumed,
        )
        assertEquals(12.minutes, resumed.elapsedAt(at(17.minutes)))
        assertEquals(13.minutes, resumed.remainingAt(at(17.minutes)))
    }

    @Test
    fun `a countdown completes exactly when its focused time reaches the duration`() {
        val resumed = FocusTimerState.Running(
            countdown,
            startedAt = start,
            resumedAt = at(15.minutes),
            accumulated = 10.minutes,
        )

        assertEquals(at(30.minutes), resumed.completesAt())
        assertFalse(resumed.isCompleteAt(at(30.minutes).minusMillis(1)))
        assertTrue(resumed.isCompleteAt(at(30.minutes)))
        assertEquals(25.minutes, resumed.elapsedAt(at(3.hours)))
        assertEquals(Duration.ZERO, resumed.remainingAt(at(3.hours)))
    }

    @Test
    fun `countdown durations are limited`() {
        assertThrows<IllegalArgumentException> { TimerMode.Countdown(30.seconds) }
        assertThrows<IllegalArgumentException> { TimerMode.Countdown(3.hours + 1.minutes) }
        assertEquals(TimerMode.Countdown.MIN_DURATION, TimerMode.Countdown(1.minutes).duration)
    }
}
