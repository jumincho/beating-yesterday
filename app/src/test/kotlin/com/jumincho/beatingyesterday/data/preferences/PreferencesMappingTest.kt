package com.jumincho.beatingyesterday.data.preferences

import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jumincho.beatingyesterday.core.model.ActivityLevel
import com.jumincho.beatingyesterday.core.model.Sex
import com.jumincho.beatingyesterday.core.model.UserProfile
import com.jumincho.beatingyesterday.core.timer.FocusTimerState
import com.jumincho.beatingyesterday.core.timer.TimerMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.Instant
import kotlin.time.Duration.Companion.minutes

class PreferencesMappingTest {

    @ParameterizedTest
    @MethodSource("timerStates")
    fun `every timer state survives a round trip`(state: FocusTimerState) {
        val preferences = mutablePreferencesOf()

        preferences.putTimerState(state)

        assertEquals(state, preferences.toTimerState())
    }

    @Test
    fun `incomplete or unknown timer data reads as idle`() {
        assertEquals(FocusTimerState.Idle, mutablePreferencesOf().toTimerState())
        assertEquals(
            FocusTimerState.Idle,
            mutablePreferencesOf(
                stringPreferencesKey("status") to "running",
                stringPreferencesKey("mode") to "stopwatch",
            )
                .toTimerState(),
        )
        assertEquals(
            FocusTimerState.Idle,
            mutablePreferencesOf(
                stringPreferencesKey("status") to "paused",
                stringPreferencesKey("mode") to "countdown",
                longPreferencesKey("countdown_ms") to 1L,
                longPreferencesKey("started_at_ms") to 0L,
            ).toTimerState(),
        )
    }

    @Test
    fun `a profile survives a round trip`() {
        val profile = UserProfile("Minji", Sex.FEMALE, 1998, 162.5, 54.2, ActivityLevel.MODERATE)
        val preferences = mutablePreferencesOf()

        preferences.putUserProfile(profile)

        assertEquals(profile, preferences.toUserProfile())
    }

    @Test
    fun `an incomplete profile reads as missing`() {
        assertNull(mutablePreferencesOf(stringPreferencesKey("name") to "Minji").toUserProfile())
        assertNull(mutablePreferencesOf().toUserProfile())
    }

    companion object {
        private val start: Instant = Instant.parse("2026-09-23T00:00:00Z")

        @JvmStatic
        fun timerStates(): List<FocusTimerState> = listOf(
            FocusTimerState.Idle,
            FocusTimerState.Running(TimerMode.Stopwatch, startedAt = start),
            FocusTimerState.Running(
                TimerMode.Countdown(25.minutes),
                startedAt = start,
                resumedAt = start.plusSeconds(900),
                accumulated = 10.minutes,
            ),
            FocusTimerState.Paused(TimerMode.Countdown(50.minutes), startedAt = start, elapsed = 12.minutes),
        )
    }
}
