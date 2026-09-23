package com.jumincho.beatingyesterday.core.contest

import java.time.LocalDate
import kotlin.time.Duration

/**
 * What happened on one day, reduced to the three numbers the contest compares.
 *
 * @property intakeKcal calories eaten, or `null` when no meal was logged (a meal of 0 kcal still
 * counts as logged).
 * @property focusTime total focused time of the day's sessions.
 * @property tasksCompleted number of tasks completed on the day.
 */
data class DailyRecord(
    val date: LocalDate,
    val intakeKcal: Int? = null,
    val focusTime: Duration = Duration.ZERO,
    val tasksCompleted: Int = 0,
) {
    /** Whether anything at all was logged on this day. */
    val hasActivity: Boolean
        get() = intakeKcal != null || focusTime.isPositive() || tasksCompleted > 0
}

/**
 * Per-day aggregates as produced by the repositories. Days missing from a map had nothing logged
 * for that metric.
 */
data class DailyTotals(
    val intakeKcal: Map<LocalDate, Int> = emptyMap(),
    val focusTime: Map<LocalDate, Duration> = emptyMap(),
    val tasksCompleted: Map<LocalDate, Int> = emptyMap(),
) {
    /** The [DailyRecord] for [date]; empty if nothing was logged. */
    fun recordFor(date: LocalDate): DailyRecord = DailyRecord(
        date = date,
        intakeKcal = intakeKcal[date],
        focusTime = focusTime[date] ?: Duration.ZERO,
        tasksCompleted = tasksCompleted[date] ?: 0,
    )

    /** The earliest day with any data, or `null` if there is none. */
    val firstDate: LocalDate?
        get() = (intakeKcal.keys + focusTime.keys + tasksCompleted.keys).minOrNull()
}
