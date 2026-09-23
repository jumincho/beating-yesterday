package com.jumincho.beatingyesterday.core.contest

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class ScoreboardTest {

    private val today = LocalDate.of(2026, 9, 23)
    private val target = 2_000

    private fun focusByDaysAgo(vararg minutesByDaysAgo: Pair<Int, Int>): DailyTotals = DailyTotals(
        focusTime = minutesByDaysAgo.associate { (daysAgo, minutes) ->
            today.minusDays(daysAgo.toLong()) to
                minutes.minutes
        },
    )

    @Test
    fun `nothing logged means no streak and draws everywhere`() {
        val scoreboard = Scoreboard.calculate(today, DailyTotals(), target)

        assertEquals(0, scoreboard.streakDays)
        assertEquals(Verdict.DRAW, scoreboard.today.verdict)
        assertTrue(scoreboard.recentDays.all { it.verdict == Verdict.DRAW })
    }

    @Test
    fun `counts consecutive winning days ending today`() {
        val totals = focusByDaysAgo(3 to 10, 2 to 20, 1 to 30, 0 to 40)

        assertEquals(4, Scoreboard.calculate(today, totals, target).streakDays)
    }

    @Test
    fun `a day that is not winning yet does not break the streak before it ends`() {
        val totals = focusByDaysAgo(3 to 10, 2 to 20, 1 to 30)

        val scoreboard = Scoreboard.calculate(today, totals, target)

        assertEquals(Verdict.LOSE, scoreboard.today.verdict)
        assertEquals(3, scoreboard.streakDays)
    }

    @Test
    fun `a lost day breaks the streak`() {
        val totals = focusByDaysAgo(3 to 10, 2 to 5, 1 to 30, 0 to 40)

        assertEquals(2, Scoreboard.calculate(today, totals, target).streakDays)
    }

    @Test
    fun `a drawn day breaks the streak`() {
        val totals = focusByDaysAgo(3 to 10, 2 to 20, 1 to 20, 0 to 40)

        assertEquals(1, Scoreboard.calculate(today, totals, target).streakDays)
    }

    @Test
    fun `recent days end today, oldest first`() {
        val totals = focusByDaysAgo(1 to 30, 0 to 10)

        val recent = Scoreboard.calculate(today, totals, target).recentDays

        assertEquals(Scoreboard.HISTORY_DAYS, recent.size)
        assertEquals(today.minusDays(6), recent.first().date)
        assertEquals(DayResult(today.minusDays(1), Verdict.WIN), recent[5])
        assertEquals(DayResult(today, Verdict.LOSE), recent[6])
    }

    @Test
    fun `today's contest uses yesterday's record and the daily target`() {
        val totals = DailyTotals(
            intakeKcal = mapOf(today to 2_100, today.minusDays(1) to 2_600),
            tasksCompleted = mapOf(today.minusDays(1) to 2),
        )

        val contest = Scoreboard.calculate(today, totals, target).today

        assertEquals(DailyRecord(today, intakeKcal = 2_100), contest.today)
        assertEquals(DailyRecord(today.minusDays(1), intakeKcal = 2_600, tasksCompleted = 2), contest.yesterday)
        assertEquals(target, contest.dailyTargetKcal)
    }

    @Test
    fun `history length must be positive`() {
        assertThrows<IllegalArgumentException> { Scoreboard.calculate(today, DailyTotals(), target, historyDays = 0) }
    }

    @Test
    fun `daily records know whether anything was logged`() {
        assertFalse(DailyRecord(today).hasActivity)
        assertTrue(DailyRecord(today, intakeKcal = 0).hasActivity)
        assertTrue(DailyRecord(today, focusTime = 1.minutes).hasActivity)
        assertFalse(DailyRecord(today, focusTime = Duration.ZERO, tasksCompleted = 0).hasActivity)
    }
}
