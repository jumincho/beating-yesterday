package com.jumincho.beatingyesterday.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jumincho.beatingyesterday.core.timer.FocusTimerState
import com.jumincho.beatingyesterday.core.timer.TimerMode
import com.jumincho.beatingyesterday.core.timer.TimerStateStore
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.IOException
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds

/** [TimerStateStore] backed by a Preferences DataStore, so a session survives process death. */
class DataStoreTimerStateStore(private val dataStore: DataStore<Preferences>) : TimerStateStore {

    override suspend fun read(): FocusTimerState = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .first()
        .toTimerState()

    override suspend fun write(state: FocusTimerState) {
        dataStore.edit { preferences ->
            preferences.clear()
            preferences.putTimerState(state)
        }
    }
}

private const val STATUS_RUNNING = "running"
private const val STATUS_PAUSED = "paused"
private const val MODE_COUNTDOWN = "countdown"
private const val MODE_STOPWATCH = "stopwatch"

private val STATUS = stringPreferencesKey("status")
private val MODE = stringPreferencesKey("mode")
private val COUNTDOWN_MS = longPreferencesKey("countdown_ms")
private val STARTED_AT_MS = longPreferencesKey("started_at_ms")
private val RESUMED_AT_MS = longPreferencesKey("resumed_at_ms")
private val ELAPSED_MS = longPreferencesKey("elapsed_ms")

/** The stored timer state; anything missing or malformed reads as [FocusTimerState.Idle]. */
internal fun Preferences.toTimerState(): FocusTimerState {
    val mode = when (this[MODE]) {
        MODE_STOPWATCH -> TimerMode.Stopwatch

        MODE_COUNTDOWN -> this[COUNTDOWN_MS]?.milliseconds
            ?.takeIf { it in TimerMode.Countdown.MIN_DURATION..TimerMode.Countdown.MAX_DURATION }
            ?.let(TimerMode::Countdown)

        else -> null
    } ?: return FocusTimerState.Idle
    val startedAt = this[STARTED_AT_MS]?.let(Instant::ofEpochMilli) ?: return FocusTimerState.Idle
    val elapsed = (this[ELAPSED_MS] ?: 0L).milliseconds
    return when (this[STATUS]) {
        STATUS_RUNNING -> {
            val resumedAt = this[RESUMED_AT_MS]?.let(Instant::ofEpochMilli) ?: return FocusTimerState.Idle
            FocusTimerState.Running(mode, startedAt, resumedAt, accumulated = elapsed)
        }

        STATUS_PAUSED -> FocusTimerState.Paused(mode, startedAt, elapsed)

        else -> FocusTimerState.Idle
    }
}

/** Writes [state]; [FocusTimerState.Idle] writes nothing. */
internal fun MutablePreferences.putTimerState(state: FocusTimerState) {
    if (state !is FocusTimerState.Active) return
    when (val mode = state.mode) {
        TimerMode.Stopwatch -> this[MODE] = MODE_STOPWATCH

        is TimerMode.Countdown -> {
            this[MODE] = MODE_COUNTDOWN
            this[COUNTDOWN_MS] = mode.duration.inWholeMilliseconds
        }
    }
    this[STARTED_AT_MS] = state.startedAt.toEpochMilli()
    when (state) {
        is FocusTimerState.Running -> {
            this[STATUS] = STATUS_RUNNING
            this[RESUMED_AT_MS] = state.resumedAt.toEpochMilli()
            this[ELAPSED_MS] = state.accumulated.inWholeMilliseconds
        }

        is FocusTimerState.Paused -> {
            this[STATUS] = STATUS_PAUSED
            this[ELAPSED_MS] = state.elapsed.inWholeMilliseconds
        }
    }
}
