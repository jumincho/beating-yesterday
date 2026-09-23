package com.jumincho.beatingyesterday.core.contest

import java.time.LocalDate

/** The [verdict] of [date] against the day before it. */
data class DayResult(val date: LocalDate, val verdict: Verdict)

/**
 * Everything the Home screen shows.
 *
 * @property today today's contest against yesterday; still in progress until midnight.
 * @property streakDays consecutive days that beat the day before them (see [Scoreboard.streak]).
 * @property recentDays one result per day for the last few days, oldest first, ending today.
 */
data class Scoreboard(val today: ContestResult, val streakDays: Int, val recentDays: List<DayResult>) {
    companion object {
        /** How many days [recentDays] covers by default. */
        const val HISTORY_DAYS = 7

        /** Builds the scoreboard for [today] from all logged [totals]. */
        fun calculate(
            today: LocalDate,
            totals: DailyTotals,
            dailyTargetKcal: Int?,
            historyDays: Int = HISTORY_DAYS,
        ): Scoreboard {
            require(historyDays > 0) { "historyDays must be positive but was $historyDays" }
            return Scoreboard(
                today = judgeDay(today, totals, dailyTargetKcal),
                streakDays = streak(today, totals, dailyTargetKcal),
                recentDays = (historyDays - 1 downTo 0).map { daysAgo ->
                    val date = today.minusDays(daysAgo.toLong())
                    DayResult(date, judgeDay(date, totals, dailyTargetKcal).verdict)
                },
            )
        }

        /**
         * Number of consecutive days, ending today, that each won against the day before.
         *
         * Today is still being played: it extends the streak as soon as it is winning, but it
         * cannot break the streak before it is over. So when today is not (yet) a win, the streak
         * is counted up to yesterday instead.
         */
        fun streak(today: LocalDate, totals: DailyTotals, dailyTargetKcal: Int?): Int {
            val firstDate = totals.firstDate ?: return 0
            var day = if (judgeDay(today, totals, dailyTargetKcal).verdict == Verdict.WIN) {
                today
            } else {
                today.minusDays(1)
            }
            var streak = 0
            while (!day.isBefore(firstDate) && judgeDay(day, totals, dailyTargetKcal).verdict == Verdict.WIN) {
                streak++
                day = day.minusDays(1)
            }
            return streak
        }

        private fun judgeDay(date: LocalDate, totals: DailyTotals, dailyTargetKcal: Int?): ContestResult =
            Contest.judge(totals.recordFor(date), totals.recordFor(date.minusDays(1)), dailyTargetKcal)
    }
}
