package com.jumincho.beatingyesterday.core.time

import com.jumincho.beatingyesterday.core.testing.MutableClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class ClocksTest {

    @Test
    fun `today is the date in the clock's zone`() {
        val clock = MutableClock(Instant.parse("2026-09-23T15:30:00Z"), ZoneId.of("Asia/Seoul"))

        assertEquals(LocalDate.of(2026, 9, 24), clock.today())
    }

    @Test
    fun `observeToday emits the new date at midnight`() = runTest {
        val clock = MutableClock.at(LocalDateTime.of(2026, 9, 23, 23, 59, 30))
        val dates = mutableListOf<LocalDate>()
        backgroundScope.launch { clock.observeToday().collect { dates += it } }
        runCurrent()

        clock.advanceBy(30.seconds)
        advanceTimeBy(30.seconds)
        runCurrent()

        assertEquals(listOf(LocalDate.of(2026, 9, 23), LocalDate.of(2026, 9, 24)), dates)
    }

    @Test
    fun `observeToday notices a manual clock change within the recheck interval`() = runTest {
        val clock = MutableClock.at(LocalDateTime.of(2026, 9, 23, 10, 0))
        val dates = mutableListOf<LocalDate>()
        backgroundScope.launch { clock.observeToday(recheckInterval = 1.minutes).collect { dates += it } }
        runCurrent()

        clock.instant = LocalDateTime.of(2026, 9, 25, 10, 0).atZone(clock.zone).toInstant()
        advanceTimeBy(1.minutes)
        runCurrent()

        assertEquals(listOf(LocalDate.of(2026, 9, 23), LocalDate.of(2026, 9, 25)), dates)
    }

    @Test
    fun `DeviceClock follows the system default zone`() {
        assertEquals(ZoneId.systemDefault(), DeviceClock.zone)
    }
}
