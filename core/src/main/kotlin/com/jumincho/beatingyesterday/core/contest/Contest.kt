package com.jumincho.beatingyesterday.core.contest

import kotlin.math.abs

/** The three rounds of a daily contest, in display order. */
enum class Round { DIET, FOCUS, TASKS }

/** How a round went, from today's point of view. */
enum class RoundOutcome {
    WON,
    LOST,
    TIED,

    /** The round could not be judged because a day had nothing to compare. */
    NO_DATA,
}

/** The overall result of a day against the day before it. */
enum class Verdict { WIN, LOSE, DRAW }

/** The outcome of one [round]. */
data class RoundResult(val round: Round, val outcome: RoundOutcome)

/**
 * Today against yesterday.
 *
 * @property dailyTargetKcal the calorie target both days were judged against, or `null` if it is
 * unknown (in which case the Diet round has no data).
 * @property rounds one result per [Round], in [Round] order.
 */
data class ContestResult(
    val today: DailyRecord,
    val yesterday: DailyRecord,
    val dailyTargetKcal: Int?,
    val rounds: List<RoundResult>,
) {
    /** Number of rounds today won. */
    val roundsWon: Int get() = rounds.count { it.outcome == RoundOutcome.WON }

    /** Number of rounds today lost. */
    val roundsLost: Int get() = rounds.count { it.outcome == RoundOutcome.LOST }

    /** [Verdict.WIN] with more rounds won than lost, [Verdict.LOSE] with fewer, else [Verdict.DRAW]. */
    val verdict: Verdict
        get() = when {
            roundsWon > roundsLost -> Verdict.WIN
            roundsWon < roundsLost -> Verdict.LOSE
            else -> Verdict.DRAW
        }

    /** The outcome of [round]. */
    fun outcomeOf(round: Round): RoundOutcome = rounds.first { it.round == round }.outcome
}

/**
 * The rules of "beating yesterday":
 *
 * - **Diet** — the day whose intake is closer to the daily target wins. A day without any logged
 *   meal has no data, so the round is not judged.
 * - **Focus** — more whole minutes of focus wins.
 * - **Tasks** — more completed tasks wins.
 *
 * Equal values tie the round. Both days are judged against the same, current daily target.
 */
object Contest {

    /** Judges [today] against [yesterday]. */
    fun judge(today: DailyRecord, yesterday: DailyRecord, dailyTargetKcal: Int?): ContestResult = ContestResult(
        today = today,
        yesterday = yesterday,
        dailyTargetKcal = dailyTargetKcal,
        rounds = listOf(
            RoundResult(Round.DIET, dietOutcome(today, yesterday, dailyTargetKcal)),
            RoundResult(
                Round.FOCUS,
                compareHigherWins(today.focusTime.inWholeMinutes, yesterday.focusTime.inWholeMinutes),
            ),
            RoundResult(
                Round.TASKS,
                compareHigherWins(today.tasksCompleted.toLong(), yesterday.tasksCompleted.toLong()),
            ),
        ),
    )

    private fun dietOutcome(today: DailyRecord, yesterday: DailyRecord, target: Int?): RoundOutcome {
        val todayIntake = today.intakeKcal
        val yesterdayIntake = yesterday.intakeKcal
        if (target == null || todayIntake == null || yesterdayIntake == null) return RoundOutcome.NO_DATA
        return compareHigherWins(-abs(todayIntake - target).toLong(), -abs(yesterdayIntake - target).toLong())
    }

    private fun compareHigherWins(today: Long, yesterday: Long): RoundOutcome = when {
        today > yesterday -> RoundOutcome.WON
        today < yesterday -> RoundOutcome.LOST
        else -> RoundOutcome.TIED
    }
}
