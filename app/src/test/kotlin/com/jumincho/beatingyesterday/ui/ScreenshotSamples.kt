package com.jumincho.beatingyesterday.ui

import com.jumincho.beatingyesterday.core.contest.DailyTotals
import com.jumincho.beatingyesterday.core.contest.Scoreboard
import com.jumincho.beatingyesterday.core.health.HealthCalculator
import com.jumincho.beatingyesterday.core.model.ActivityLevel
import com.jumincho.beatingyesterday.core.model.FocusSession
import com.jumincho.beatingyesterday.core.model.MealEntry
import com.jumincho.beatingyesterday.core.model.MealType
import com.jumincho.beatingyesterday.core.model.Sex
import com.jumincho.beatingyesterday.core.model.TodoItem
import com.jumincho.beatingyesterday.core.model.UserProfile
import com.jumincho.beatingyesterday.core.profile.ProfileDraft
import com.jumincho.beatingyesterday.core.timer.TimerMode
import com.jumincho.beatingyesterday.ui.diet.DietUiState
import com.jumincho.beatingyesterday.ui.diet.MealSection
import com.jumincho.beatingyesterday.ui.focus.FocusUiState
import com.jumincho.beatingyesterday.ui.focus.SessionItem
import com.jumincho.beatingyesterday.ui.focus.TimerStatus
import com.jumincho.beatingyesterday.ui.home.HomeUiState
import com.jumincho.beatingyesterday.ui.profile.ProfileUiState
import com.jumincho.beatingyesterday.ui.tasks.TasksUiState
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * One consistent sample day for the screenshot tests: the numbers on Home match the meals, focus
 * sessions and tasks shown on the other screens, and every derived value (health numbers, rounds,
 * verdict, streak) comes from the real `:core` calculations.
 */
internal object ScreenshotSamples {
    private val zone = ZoneId.of("Asia/Seoul")

    val today: LocalDate = LocalDate.of(2026, 9, 23)

    private val userProfile = UserProfile(
        name = "Minji",
        sex = Sex.FEMALE,
        birthYear = 1999,
        heightCm = 168.0,
        weightKg = 60.0,
        activityLevel = ActivityLevel.MODERATE,
    )

    private val metrics = HealthCalculator.metricsFor(userProfile, today)

    private val meals = listOf(
        MealEntry(1, today, MealType.BREAKFAST, "Greek yogurt with berries", 320),
        MealEntry(2, today, MealType.BREAKFAST, "Americano", 10),
        MealEntry(3, today, MealType.LUNCH, "Bibimbap", 560),
        MealEntry(4, today, MealType.LUNCH, "Barley tea", 0),
        MealEntry(5, today, MealType.DINNER, "Grilled mackerel set meal", 720),
        MealEntry(6, today, MealType.SNACK, "Banana", 105),
        MealEntry(7, today, MealType.SNACK, "Café latte", 190),
        MealEntry(8, today, MealType.SNACK, "Almonds, 20 g", 115),
    )

    private val sessions = listOf(
        session(id = 1, start = LocalTime.of(9, 10), length = 25.minutes),
        session(id = 2, start = LocalTime.of(13, 0), length = 50.minutes),
        session(id = 3, start = LocalTime.of(16, 5), length = 20.minutes),
    )

    private val tasks = listOf(
        TodoItem(1, "Finish the statistics assignment", createdOn = today.minusDays(1)),
        TodoItem(5, "Call grandma", createdOn = today),
        TodoItem(2, "Review this week's lecture notes", createdOn = today, completedOn = today),
        TodoItem(3, "Go for a 20-minute walk", createdOn = today, completedOn = today),
        TodoItem(4, "Read two chapters", createdOn = today, completedOn = today),
    )

    /** Today plus the week before it: calories, focus minutes and completed tasks per day. */
    private val totals: DailyTotals = run {
        val week = listOf(
            Triple(2_600, 10, 1),
            Triple(2_200, 30, 3),
            Triple(2_500, 40, 3),
            Triple(2_150, 45, 2),
            Triple(2_400, 20, 1),
            Triple(2_300, 40, 2),
            Triple(2_250, 60, 4),
        )
        val days = week.indices.map { today.minusDays((week.size - it).toLong()) }
        DailyTotals(
            intakeKcal = days.zip(week) { day, (kcal, _, _) -> day to kcal }.toMap() +
                (today to meals.sumOf { it.kcal }),
            focusTime = days.zip(week) { day, (_, minutes, _) -> day to minutes.minutes }.toMap() +
                (today to sessions.fold(Duration.ZERO) { total, session -> total + session.duration }),
            tasksCompleted = days.zip(week) { day, (_, _, done) -> day to done }.toMap() +
                (today to tasks.count { it.completedOn == today }),
        )
    }

    val home = HomeUiState.Ready(
        userName = userProfile.name,
        today = today,
        scoreboard = Scoreboard.calculate(today, totals, metrics.dailyTargetKcal),
    )

    val diet = DietUiState.Ready(
        date = today,
        daysAgo = 0,
        sections = MealType.entries.map { type -> MealSection(type, meals.filter { it.type == type }) },
        metrics = metrics,
        suggestedType = MealType.SNACK,
    )

    /** A 50-minute countdown with 38 minutes to go, after three finished sessions. */
    val focus = FocusUiState(
        isLoading = false,
        status = TimerStatus.RUNNING,
        mode = TimerMode.Countdown(50.minutes),
        elapsed = 12.minutes,
        remaining = 38.minutes,
        progress = 38f / 50f,
        sessions = sessions.asReversed().map { session ->
            SessionItem(
                session = session,
                start = session.startedAt.atZone(zone).toLocalTime(),
                end = session.endedAt.atZone(zone).toLocalTime(),
            )
        },
        todayTotal = totals.recordFor(today).focusTime,
    )

    val tasksState = TasksUiState(isLoading = false, today = today, tasks = tasks)

    /** The saved profile, open for editing, with its health numbers. */
    val profile = ProfileUiState(isLoading = false, draft = ProfileDraft.from(userProfile), preview = metrics)

    private fun session(id: Long, start: LocalTime, length: Duration): FocusSession {
        val startedAt = today.atTime(start).atZone(zone).toInstant()
        return FocusSession(
            id = id,
            date = today,
            startedAt = startedAt,
            endedAt = startedAt.plusSeconds(length.inWholeSeconds),
            duration = length,
        )
    }
}
