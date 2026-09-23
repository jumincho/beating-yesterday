package com.jumincho.beatingyesterday.core.timer

import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toKotlinDuration

/** How the focus timer counts. */
sealed interface TimerMode {

    /** Counts down from [duration] and completes, recording a session, when it reaches zero. */
    data class Countdown(val duration: Duration) : TimerMode {
        init {
            require(duration in MIN_DURATION..MAX_DURATION) {
                "duration must be within $MIN_DURATION..$MAX_DURATION but was $duration"
            }
        }

        companion object {
            /** Shortest countdown that can be set. */
            val MIN_DURATION: Duration = 1.minutes

            /** Longest countdown that can be set. */
            val MAX_DURATION: Duration = 3.hours

            /** Durations offered as one-tap presets. */
            val PRESETS: List<Duration> = listOf(25.minutes, 50.minutes)
        }
    }

    /** Counts up until stopped. */
    data object Stopwatch : TimerMode
}

/**
 * The focus timer as a pure value.
 *
 * Elapsed time is always derived from timestamps (`accumulated` time of earlier running segments
 * plus the time since the current segment started) rather than from counting ticks, so a state
 * restored after process death reports the correct time.
 */
sealed interface FocusTimerState {

    /** Focused time at [now], excluding pauses. A countdown never exceeds its duration. */
    fun elapsedAt(now: Instant): Duration

    /** Not timing anything. */
    data object Idle : FocusTimerState {
        override fun elapsedAt(now: Instant): Duration = Duration.ZERO
    }

    /** A session in progress, running or paused. */
    sealed interface Active : FocusTimerState {
        /** How the session counts. */
        val mode: TimerMode

        /** When the session was started. */
        val startedAt: Instant

        /** Time left at [now] for a countdown; `null` for a stopwatch. */
        fun remainingAt(now: Instant): Duration? =
            (mode as? TimerMode.Countdown)?.let { (it.duration - elapsedAt(now)).coerceAtLeast(Duration.ZERO) }
    }

    /**
     * The clock is running.
     *
     * @property resumedAt when the current running segment began.
     * @property accumulated focused time of the segments before [resumedAt].
     */
    data class Running(
        override val mode: TimerMode,
        override val startedAt: Instant,
        val resumedAt: Instant = startedAt,
        val accumulated: Duration = Duration.ZERO,
    ) : Active {
        override fun elapsedAt(now: Instant): Duration {
            val segment = java.time.Duration.between(resumedAt, now).toKotlinDuration().coerceAtLeast(Duration.ZERO)
            val elapsed = accumulated + segment
            return if (mode is TimerMode.Countdown) elapsed.coerceAtMost(mode.duration) else elapsed
        }

        /** When a countdown will reach zero if it keeps running; `null` for a stopwatch. */
        fun completesAt(): Instant? = (mode as? TimerMode.Countdown)?.let {
            resumedAt.plusNanos((it.duration - accumulated).inWholeNanoseconds)
        }

        /** Whether this is a countdown that has reached zero at [now]. */
        fun isCompleteAt(now: Instant): Boolean = completesAt()?.let { !now.isBefore(it) } ?: false

        /** This session paused at [now]. */
        fun pause(now: Instant): Paused = Paused(mode, startedAt, elapsedAt(now))
    }

    /**
     * The clock is paused.
     *
     * @property elapsed focused time when the session was paused.
     */
    data class Paused(override val mode: TimerMode, override val startedAt: Instant, val elapsed: Duration) : Active {
        override fun elapsedAt(now: Instant): Duration = elapsed

        /** This session resumed at [now]. */
        fun resume(now: Instant): Running = Running(mode, startedAt, resumedAt = now, accumulated = elapsed)
    }
}
