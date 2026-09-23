package com.jumincho.beatingyesterday.core.timer

import com.jumincho.beatingyesterday.core.data.FocusSessionRepository
import com.jumincho.beatingyesterday.core.model.FocusSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toKotlinDuration

/** Persists the [FocusTimerState] so a running session survives process death. */
interface TimerStateStore {
    /** The last written state, or [FocusTimerState.Idle] if there is none. */
    suspend fun read(): FocusTimerState

    /** Replaces the stored state. */
    suspend fun write(state: FocusTimerState)
}

/** What [FocusTimer.stop] did. */
sealed interface StopResult {
    /** The session was long enough and has been recorded. */
    data class Saved(val session: FocusSession) : StopResult

    /** The session was shorter than [FocusTimer.MIN_RECORDED_DURATION] and was discarded. */
    data class TooShort(val elapsed: Duration) : StopResult

    /** There was no session to stop. */
    data object NotRunning : StopResult
}

/**
 * The single, app-wide focus timer.
 *
 * Every transition is persisted through [store] before it becomes visible, and all transitions
 * are serialised with a mutex. A countdown that reaches zero is recorded automatically, even if
 * that happened while the app was not running: the next time the state is read, the session is
 * recorded with the exact moment it completed.
 *
 * @param scope a long-lived scope that restores the state and watches for countdown completion.
 */
class FocusTimer(
    private val store: TimerStateStore,
    private val sessions: FocusSessionRepository,
    private val clock: Clock,
    private val scope: CoroutineScope,
) {
    private val mutex = Mutex()
    private val current = MutableStateFlow<FocusTimerState?>(null)
    private val completions = MutableSharedFlow<FocusSession>(extraBufferCapacity = 8)

    /** The timer state, emitted once the persisted state has been restored. */
    val state: Flow<FocusTimerState> = current.filterNotNull()

    /** Sessions recorded because a countdown reached zero. */
    val completedSessions: SharedFlow<FocusSession> = completions.asSharedFlow()

    init {
        scope.launch {
            settle()
            current.filterNotNull().collectLatest { state ->
                val completesAt = (state as? FocusTimerState.Running)?.completesAt() ?: return@collectLatest
                delayUntil(completesAt)
                scope.launch { settle() }
            }
        }
    }

    /** Starts a new session in [mode]. Ignored while a session is already active. */
    suspend fun start(mode: TimerMode) {
        transition { state, now -> if (state is FocusTimerState.Idle) FocusTimerState.Running(mode, now) else state }
    }

    /** Pauses a running session. */
    suspend fun pause() {
        transition { state, now -> if (state is FocusTimerState.Running) state.pause(now) else state }
    }

    /** Resumes a paused session. */
    suspend fun resume() {
        transition { state, now -> if (state is FocusTimerState.Paused) state.resume(now) else state }
    }

    /**
     * Ends the active session. It is recorded if it lasted at least [MIN_RECORDED_DURATION];
     * a countdown that already reached zero is recorded as completed instead.
     */
    suspend fun stop(): StopResult = withSettledState { state, now ->
        if (state !is FocusTimerState.Active) return@withSettledState StopResult.NotRunning
        val elapsed = state.elapsedAt(now)
        if (elapsed < MIN_RECORDED_DURATION) {
            publish(FocusTimerState.Idle)
            StopResult.TooShort(elapsed)
        } else {
            val session = sessionOf(state, endedAt = now, duration = elapsed)
            sessions.saveSession(session)
            publish(FocusTimerState.Idle)
            StopResult.Saved(session)
        }
    }

    private suspend fun transition(transform: (FocusTimerState, Instant) -> FocusTimerState) {
        withSettledState { state, now ->
            val next = transform(state, now)
            if (next != state) publish(next)
        }
    }

    private suspend fun settle() {
        withSettledState { _, _ -> }
    }

    /**
     * Runs [block] with the current state after completing a countdown that has reached zero.
     * Must be the only way the state is read or changed.
     */
    private suspend fun <T> withSettledState(block: suspend (FocusTimerState, Instant) -> T): T = mutex.withLock {
        val now = clock.instant()
        val state = current.value ?: store.read()
        if (state is FocusTimerState.Running && state.isCompleteAt(now)) {
            val mode = state.mode as TimerMode.Countdown
            val session = sessionOf(state, endedAt = checkNotNull(state.completesAt()), duration = mode.duration)
            sessions.saveSession(session)
            publish(FocusTimerState.Idle)
            completions.tryEmit(session)
        } else {
            current.value = state
        }
        block(checkNotNull(current.value), now)
    }

    private suspend fun publish(state: FocusTimerState) {
        store.write(state)
        current.value = state
    }

    private fun sessionOf(state: FocusTimerState.Active, endedAt: Instant, duration: Duration) = FocusSession(
        date = state.startedAt.atZone(clock.zone).toLocalDate(),
        startedAt = state.startedAt,
        endedAt = endedAt,
        duration = duration,
    )

    private suspend fun delayUntil(instant: Instant) {
        while (true) {
            val remaining = java.time.Duration.between(clock.instant(), instant).toKotlinDuration()
            if (!remaining.isPositive()) return
            delay(remaining)
        }
    }

    companion object {
        /** Sessions stopped before this much focused time are not recorded. */
        val MIN_RECORDED_DURATION: Duration = 1.minutes
    }
}
