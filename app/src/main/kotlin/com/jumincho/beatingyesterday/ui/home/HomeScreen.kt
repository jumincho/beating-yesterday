package com.jumincho.beatingyesterday.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jumincho.beatingyesterday.R
import com.jumincho.beatingyesterday.core.contest.ContestResult
import com.jumincho.beatingyesterday.core.contest.DailyRecord
import com.jumincho.beatingyesterday.core.contest.DailyTotals
import com.jumincho.beatingyesterday.core.contest.DayResult
import com.jumincho.beatingyesterday.core.contest.Round
import com.jumincho.beatingyesterday.core.contest.RoundOutcome
import com.jumincho.beatingyesterday.core.contest.RoundResult
import com.jumincho.beatingyesterday.core.contest.Scoreboard
import com.jumincho.beatingyesterday.core.contest.Verdict
import com.jumincho.beatingyesterday.ui.AppViewModelProvider
import com.jumincho.beatingyesterday.ui.components.InfoCard
import com.jumincho.beatingyesterday.ui.components.LoadingState
import com.jumincho.beatingyesterday.ui.components.SectionHeader
import com.jumincho.beatingyesterday.ui.format.currentLocale
import com.jumincho.beatingyesterday.ui.format.dateText
import com.jumincho.beatingyesterday.ui.format.durationText
import com.jumincho.beatingyesterday.ui.format.icon
import com.jumincho.beatingyesterday.ui.format.kcalText
import com.jumincho.beatingyesterday.ui.format.label
import com.jumincho.beatingyesterday.ui.format.shortLabel
import com.jumincho.beatingyesterday.ui.theme.BeatingYesterdayTheme
import java.time.LocalDate
import java.time.format.TextStyle
import kotlin.time.Duration.Companion.minutes

/** Home: today's contest against yesterday, the streak and the last seven days. */
@Composable
fun HomeScreen(
    onOpenProfile: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(uiState = uiState, onOpenProfile = onOpenProfile)
}

/** Stateless Home screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(uiState: HomeUiState, onOpenProfile: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenProfile) {
                        Icon(
                            painter = painterResource(R.drawable.ic_account_circle),
                            contentDescription = stringResource(R.string.home_open_profile),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when (uiState) {
            HomeUiState.Loading -> LoadingState(Modifier.padding(innerPadding))

            is HomeUiState.Ready -> HomeContent(
                state = uiState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
}

@Composable
private fun HomeContent(state: HomeUiState.Ready, modifier: Modifier = Modifier) {
    val contest = state.scoreboard.today
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text(
                    text = stringResource(R.string.home_greeting, state.userName),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = dateText(state.today),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item { VerdictCard(contest = contest, streakDays = state.scoreboard.streakDays) }
        if (!contest.yesterday.hasActivity) {
            item { InfoCard(icon = R.drawable.ic_info, text = stringResource(R.string.home_first_day_hint)) }
        }
        item { SectionHeader(title = stringResource(R.string.home_rounds_title)) }
        items(contest.rounds, key = { it.round }) { result -> RoundCard(result = result, contest = contest) }
        item { SectionHeader(title = stringResource(R.string.home_history_title)) }
        item { HistoryStrip(days = state.scoreboard.recentDays, today = state.today) }
        item { RulesCard() }
    }
}

@Composable
private fun VerdictCard(contest: ContestResult, streakDays: Int, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val (container, content) = when (contest.verdict) {
        Verdict.WIN -> colors.primaryContainer to colors.onPrimaryContainer
        Verdict.LOSE -> colors.errorContainer to colors.onErrorContainer
        Verdict.DRAW -> colors.secondaryContainer to colors.onSecondaryContainer
    }
    val (icon, title) = when (contest.verdict) {
        Verdict.WIN -> R.drawable.ic_emoji_events to R.string.home_verdict_win
        Verdict.LOSE -> R.drawable.ic_trending_down to R.string.home_verdict_lose
        Verdict.DRAW -> R.drawable.ic_balance to R.string.home_verdict_draw
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(40.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = stringResource(R.string.home_rounds_summary, contest.roundsWon, contest.roundsLost),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        HorizontalDivider(color = content.copy(alpha = 0.16f))
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painter = painterResource(R.drawable.ic_local_fire_department), contentDescription = null)
            Text(
                text = if (streakDays > 0) {
                    pluralStringResource(R.plurals.home_streak, streakDays, streakDays)
                } else {
                    stringResource(R.string.home_streak_none)
                },
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun RoundCard(result: RoundResult, contest: ContestResult, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(result.round.icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(result.round.label),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .weight(1f),
                )
                OutcomeBadge(result.outcome)
            }
            RoundValue(label = stringResource(R.string.today), value = roundValue(result.round, contest.today, contest))
            RoundValue(
                label = stringResource(R.string.yesterday),
                value = roundValue(result.round, contest.yesterday, contest),
            )
        }
    }
}

@Composable
private fun RoundValue(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun roundValue(round: Round, record: DailyRecord, contest: ContestResult): String = when (round) {
    Round.DIET -> dietValue(record.intakeKcal, contest.dailyTargetKcal)
    Round.FOCUS -> durationText(record.focusTime)
    Round.TASKS -> pluralStringResource(R.plurals.home_tasks_done, record.tasksCompleted, record.tasksCompleted)
}

@Composable
private fun dietValue(intakeKcal: Int?, dailyTargetKcal: Int?): String {
    if (intakeKcal == null) return stringResource(R.string.home_diet_no_meals)
    if (dailyTargetKcal == null) return kcalText(intakeKcal)
    val difference = intakeKcal - dailyTargetKcal
    return when {
        difference == 0 -> stringResource(R.string.home_diet_on_target, kcalText(intakeKcal))
        difference < 0 -> stringResource(R.string.home_diet_under, kcalText(intakeKcal), kcalText(-difference))
        else -> stringResource(R.string.home_diet_over, kcalText(intakeKcal), kcalText(difference))
    }
}

@Composable
private fun OutcomeBadge(outcome: RoundOutcome) {
    val colors = MaterialTheme.colorScheme
    val (container, content) = when (outcome) {
        RoundOutcome.WON -> colors.primary to colors.onPrimary
        RoundOutcome.LOST -> colors.error to colors.onError
        RoundOutcome.TIED -> colors.secondaryContainer to colors.onSecondaryContainer
        RoundOutcome.NO_DATA -> colors.surfaceVariant to colors.onSurfaceVariant
    }
    Surface(color = container, contentColor = content, shape = CircleShape) {
        Text(
            text = stringResource(outcome.label),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun HistoryStrip(days: List<DayResult>, today: LocalDate, modifier: Modifier = Modifier) {
    val locale = currentLocale()
    val colors = MaterialTheme.colorScheme
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        days.forEach { day ->
            val (container, content) = when (day.verdict) {
                Verdict.WIN -> colors.primary to colors.onPrimary
                Verdict.LOSE -> colors.error to colors.onError
                Verdict.DRAW -> colors.surfaceVariant to colors.onSurfaceVariant
            }
            val description = stringResource(
                R.string.home_history_day,
                dateText(day.date, skeleton = "EEEEMMMd"),
                stringResource(day.verdict.label),
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.clearAndSetSemantics { contentDescription = description },
            ) {
                Text(
                    text = if (day.date == today) {
                        stringResource(R.string.today_short)
                    } else {
                        day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .background(container, CircleShape)
                        .then(
                            if (day.date == today) Modifier.border(2.dp, colors.outline, CircleShape) else Modifier,
                        ),
                ) {
                    Text(
                        text = stringResource(day.verdict.shortLabel),
                        style = MaterialTheme.typography.labelLarge,
                        color = content,
                    )
                }
            }
        }
    }
}

@Composable
private fun RulesCard(modifier: Modifier = Modifier) {
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.home_rules_title),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.semantics { heading() },
            )
            Text(text = stringResource(R.string.home_rules_diet), style = MaterialTheme.typography.bodySmall)
            Text(text = stringResource(R.string.home_rules_focus), style = MaterialTheme.typography.bodySmall)
            Text(text = stringResource(R.string.home_rules_tasks), style = MaterialTheme.typography.bodySmall)
            Text(text = stringResource(R.string.home_rules_verdict), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@PreviewLightDark
@Composable
private fun HomeScreenPreview() {
    val today = LocalDate.of(2026, 9, 23)
    val totals = DailyTotals(
        intakeKcal = mapOf(today to 1_950, today.minusDays(1) to 2_450),
        focusTime = mapOf(today to 95.minutes, today.minusDays(1) to 60.minutes, today.minusDays(2) to 30.minutes),
        tasksCompleted = mapOf(today to 3, today.minusDays(1) to 4, today.minusDays(3) to 1),
    )
    BeatingYesterdayTheme(dynamicColor = false) {
        HomeScreen(
            uiState = HomeUiState.Ready(
                userName = "Minji",
                today = today,
                scoreboard = Scoreboard.calculate(today, totals, dailyTargetKcal = 2_100),
            ),
            onOpenProfile = {},
        )
    }
}
