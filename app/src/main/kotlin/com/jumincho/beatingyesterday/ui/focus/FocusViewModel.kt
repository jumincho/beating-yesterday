package com.jumincho.beatingyesterday.ui.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jumincho.beatingyesterday.core.data.FocusSessionRepository
import com.jumincho.beatingyesterday.core.model.FocusSession
import com.jumincho.beatingyesterday.core.timer.FocusTimer
import com.jumincho.beatingyesterday.core.timer.FocusTimerState
import com.jumincho.beatingyesterday.core.timer.StopResult
import com.jumincho.beatingyesterday.core.timer.TimerMode
import com.jumincho.beatingyesterday.ui.WhileUiSubscribed
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

/** Whether the timer is idle, running or paused. */
enum class TimerStatus { IDLE, RUNNING, PAUSED }

/** A finished session with its local start and end times. */
data class SessionItem(val session: FocusSession, val start: LocalTime, val end: LocalTime)

/** One-off feedback for the Focus screen. */
sealed interface FocusMessage {
    data class Saved(val duration: Duration) : FocusMessage

    data object TooShort : FocusMessage

    data class Completed(val duration: Duration) : FocusMessage

    data object SessionDeleted : FocusMessage
}

/**
 * State of the Focus screen.
 *
 * @property mode the active session's mode, or the mode that Start will use.
 * @property remaining time left for a countdown; `null` for a stopwatch.
 * @property progress ring fill: the remaining fraction of a countdown, or the position within the
 * current hour of a stopwatch.
 * @property sessions today's sessions, newest first.
 */
data class FocusUiState(
    val isLoading: Boolean = true,
    val status: TimerStatus = TimerStatus.IDLE,
    val mode: TimerMode = DEFAULT_COUNTDOWN,
    val elapsed: Duration = Duration.ZERO,
    val remaining: Duration? = null,
    val progress: Float = 0f,
    val sessions: List<SessionItem> = emptyList(),
    val todayTotal: Duration = Duration.ZERO,
)

private val DEFAULT_COUNTDOWN = TimerMode.Countdown(TimerMode.Countdown.PRESETS.first())
private val TICK_INTERVAL = 200.milliseconds

/** Drives the focus timer and lists today's sessions. */
@OptIn(ExperimentalCoroutinesApi::class)
class FocusViewModel(
    private val timer: FocusTimer,
    private val focusSessionRepository: FocusSessionRepository,
    private val clock: Clock,
    today: Flow<LocalDate>,
) : ViewModel() {

    private val selectedMode = MutableStateFlow<TimerMode>(DEFAULT_COUNTDOWN)
    private var lastCountdown = DEFAULT_COUNTDOWN
    private val _message = MutableStateFlow<FocusMessage?>(null)
    private var deletedSession: FocusSession? = null

    /** Feedback to show once, until [onMessageShown]. */
    val message: StateFlow<FocusMessage?> = _message.asStateFlow()

    private val timerAtNow: Flow<Pair<FocusTimerState, Instant>> = timer.state.flatMapLatest { state ->
        if (state is FocusTimerState.Running) ticks().map { now -> state to now } else flowOf(state to clock.instant())
    }

    val uiState: StateFlow<FocusUiState> = combine(
        timerAtNow,
        selectedMode,
        today.flatMapLatest { focusSessionRepository.observeSessions(it) },
    ) { (state, now), selected, sessions -> buildState(state, now, selected, sessions) }
        .stateIn(viewModelScope, WhileUiSubscribed, FocusUiState())

    init {
        viewModelScope.launch {
            timer.completedSessions.collect { _message.value = FocusMessage.Completed(it.duration) }
        }
    }

    /** Chooses a countdown of [minutes] for the next session. */
    fun selectCountdown(minutes: Int) {
        val duration = minutes.minutes.coerceIn(TimerMode.Countdown.MIN_DURATION, TimerMode.Countdown.MAX_DURATION)
        lastCountdown = TimerMode.Countdown(duration)
        selectedMode.value = lastCountdown
    }

    /** Switches to the countdown chosen last. */
    fun selectTimer() {
        selectedMode.value = lastCountdown
    }

    /** Chooses the stopwatch for the next session. */
    fun selectStopwatch() {
        selectedMode.value = TimerMode.Stopwatch
    }

    fun start() {
        viewModelScope.launch { timer.start(selectedMode.value) }
    }

    fun pause() {
        viewModelScope.launch { timer.pause() }
    }

    fun resume() {
        viewModelScope.launch { timer.resume() }
    }

    fun stop() {
        viewModelScope.launch {
            _message.value = when (val result = timer.stop()) {
                is StopResult.Saved -> FocusMessage.Saved(result.session.duration)
                is StopResult.TooShort -> FocusMessage.TooShort
                StopResult.NotRunning -> return@launch
            }
        }
    }

    fun deleteSession(session: FocusSession) {
        viewModelScope.launch {
            focusSessionRepository.deleteSession(session.id)
            deletedSession = session
            _message.value = FocusMessage.SessionDeleted
        }
    }

    /** Restores the session deleted last. */
    fun undoDeleteSession() {
        val session = deletedSession ?: return
        deletedSession = null
        viewModelScope.launch { focusSessionRepository.saveSession(session) }
    }

    fun onMessageShown() {
        _message.value = null
    }

    private fun ticks(): Flow<Instant> = flow {
        while (true) {
            emit(clock.instant())
            delay(TICK_INTERVAL)
        }
    }

    private fun buildState(
        state: FocusTimerState,
        now: Instant,
        selected: TimerMode,
        sessions: List<FocusSession>,
    ): FocusUiState {
        val active = state as? FocusTimerState.Active
        val mode = active?.mode ?: selected
        val elapsed = state.elapsedAt(now)
        val remaining = active?.remainingAt(now) ?: (mode as? TimerMode.Countdown)?.duration
        val progress = when (mode) {
            is TimerMode.Countdown -> ((remaining ?: Duration.ZERO) / mode.duration).toFloat()

            TimerMode.Stopwatch -> (elapsed.inWholeMilliseconds % 1.hours.inWholeMilliseconds) /
                1.hours.inWholeMilliseconds.toFloat()
        }
        return FocusUiState(
            isLoading = false,
            status = when (state) {
                FocusTimerState.Idle -> TimerStatus.IDLE
                is FocusTimerState.Running -> TimerStatus.RUNNING
                is FocusTimerState.Paused -> TimerStatus.PAUSED
            },
            mode = mode,
            elapsed = elapsed,
            remaining = remaining,
            progress = progress,
            sessions = sessions.asReversed().map { session ->
                SessionItem(
                    session = session,
                    start = session.startedAt.atZone(clock.zone).toLocalTime(),
                    end = session.endedAt.atZone(clock.zone).toLocalTime(),
                )
            },
            todayTotal = sessions.fold(Duration.ZERO) { total, session -> total + session.duration },
        )
    }
}
