package com.jumincho.beatingyesterday.core.contest

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ContestTest {

    private val today = LocalDate.of(2026, 9, 23)
    private val yesterday = today.minusDays(1)
    private val target = 2_000

    private fun judge(todayRecord: DailyRecord, yesterdayRecord: DailyRecord, dailyTarget: Int? = target) =
        Contest.judge(todayRecord, yesterdayRecord, dailyTarget)

    @Nested
    inner class DietRound {
        private fun outcome(todayKcal: Int?, yesterdayKcal: Int?, dailyTarget: Int? = target) = judge(
            DailyRecord(today, intakeKcal = todayKcal),
            DailyRecord(yesterday, intakeKcal = yesterdayKcal),
            dailyTarget,
        ).outcomeOf(Round.DIET)

        @Test
        fun `the intake closer to the target wins`() {
            assertEquals(RoundOutcome.WON, outcome(todayKcal = 1_900, yesterdayKcal = 2_400))
            assertEquals(RoundOutcome.LOST, outcome(todayKcal = 1_200, yesterdayKcal = 2_100))
        }

        @Test
        fun `being under or over by the same amount ties`() {
            assertEquals(RoundOutcome.TIED, outcome(todayKcal = 1_800, yesterdayKcal = 2_200))
        }

        @Test
        fun `a day without meals has no data`() {
            assertEquals(RoundOutcome.NO_DATA, outcome(todayKcal = null, yesterdayKcal = 2_000))
            assertEquals(RoundOutcome.NO_DATA, outcome(todayKcal = 2_000, yesterdayKcal = null))
        }

        @Test
        fun `a logged zero-calorie day still counts as data`() {
            assertEquals(RoundOutcome.LOST, outcome(todayKcal = 0, yesterdayKcal = 1_500))
        }

        @Test
        fun `an unknown target has no data`() {
            assertEquals(RoundOutcome.NO_DATA, outcome(todayKcal = 1_900, yesterdayKcal = 2_400, dailyTarget = null))
        }
    }

    @Nested
    inner class FocusRound {
        private fun outcome(todayFocus: kotlin.time.Duration, yesterdayFocus: kotlin.time.Duration) = judge(
            DailyRecord(today, focusTime = todayFocus),
            DailyRecord(yesterday, focusTime = yesterdayFocus),
        ).outcomeOf(Round.FOCUS)

        @Test
        fun `more focus minutes wins`() {
            assertEquals(RoundOutcome.WON, outcome(50.minutes, 25.minutes))
            assertEquals(RoundOutcome.LOST, outcome(0.minutes, 1.minutes))
        }

        @Test
        fun `compares whole minutes only`() {
            assertEquals(RoundOutcome.TIED, outcome(25.minutes + 59.seconds, 25.minutes))
        }
    }

    @Nested
    inner class TasksRound {
        @Test
        fun `more completed tasks wins and equal counts tie`() {
            fun outcome(todayTasks: Int, yesterdayTasks: Int) = judge(
                DailyRecord(today, tasksCompleted = todayTasks),
                DailyRecord(yesterday, tasksCompleted = yesterdayTasks),
            ).outcomeOf(Round.TASKS)

            assertEquals(RoundOutcome.WON, outcome(3, 2))
            assertEquals(RoundOutcome.LOST, outcome(0, 1))
            assertEquals(RoundOutcome.TIED, outcome(4, 4))
        }
    }

    @Nested
    inner class Verdicts {
        @Test
        fun `more rounds won than lost is a win`() {
            val result = judge(
                DailyRecord(today, intakeKcal = 2_050, focusTime = 60.minutes, tasksCompleted = 1),
                DailyRecord(yesterday, intakeKcal = 2_500, focusTime = 30.minutes, tasksCompleted = 3),
            )

            assertEquals(2, result.roundsWon)
            assertEquals(1, result.roundsLost)
            assertEquals(Verdict.WIN, result.verdict)
        }

        @Test
        fun `fewer rounds won than lost is a loss, even when a round has no data`() {
            val result = judge(
                DailyRecord(today, focusTime = 10.minutes, tasksCompleted = 2),
                DailyRecord(yesterday, intakeKcal = 1_900, focusTime = 20.minutes, tasksCompleted = 2),
            )

            assertEquals(
                listOf(
                    RoundResult(Round.DIET, RoundOutcome.NO_DATA),
                    RoundResult(Round.FOCUS, RoundOutcome.LOST),
                    RoundResult(Round.TASKS, RoundOutcome.TIED),
                ),
                result.rounds,
            )
            assertEquals(Verdict.LOSE, result.verdict)
        }

        @Test
        fun `one win and one loss is a draw`() {
            val result = judge(
                DailyRecord(today, focusTime = 40.minutes, tasksCompleted = 1),
                DailyRecord(yesterday, focusTime = 20.minutes, tasksCompleted = 2),
            )

            assertEquals(Verdict.DRAW, result.verdict)
        }

        @Test
        fun `two empty days draw`() {
            assertEquals(Verdict.DRAW, judge(DailyRecord(today), DailyRecord(yesterday)).verdict)
        }
    }
}
