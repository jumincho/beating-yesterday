package com.jumincho.beatingyesterday.ui.diet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jumincho.beatingyesterday.R
import com.jumincho.beatingyesterday.core.health.BmiCategory
import com.jumincho.beatingyesterday.core.health.HealthMetrics
import com.jumincho.beatingyesterday.core.model.MealEntry
import com.jumincho.beatingyesterday.core.model.MealType
import com.jumincho.beatingyesterday.ui.AppViewModelProvider
import com.jumincho.beatingyesterday.ui.components.LoadingState
import com.jumincho.beatingyesterday.ui.components.SectionHeader
import com.jumincho.beatingyesterday.ui.format.bmiText
import com.jumincho.beatingyesterday.ui.format.dateText
import com.jumincho.beatingyesterday.ui.format.kcalText
import com.jumincho.beatingyesterday.ui.format.label
import com.jumincho.beatingyesterday.ui.theme.BeatingYesterdayTheme
import java.time.LocalDate

/** Diet: the day's meals grouped by type against the daily calorie target. */
@Composable
fun DietScreen(
    onAddMeal: (LocalDate, MealType) -> Unit,
    onEditMeal: (MealEntry) -> Unit,
    viewModel: DietViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val deletedMeal by viewModel.deletedMeal.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val deletedMessage = deletedMeal?.let { stringResource(R.string.diet_meal_deleted, it.name) }
    val undoLabel = stringResource(R.string.action_undo)

    LaunchedEffect(deletedMeal) {
        val message = deletedMessage ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(message, actionLabel = undoLabel, duration = SnackbarDuration.Short)
        if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete() else viewModel.onDeletedMessageShown()
    }

    DietScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onPreviousDay = viewModel::showPreviousDay,
        onNextDay = viewModel::showNextDay,
        onToday = viewModel::showToday,
        onAddMeal = onAddMeal,
        onEditMeal = onEditMeal,
        onDeleteMeal = viewModel::deleteMeal,
    )
}

/** Stateless Diet screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DietScreen(
    uiState: DietUiState,
    snackbarHostState: SnackbarHostState,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onToday: () -> Unit,
    onAddMeal: (LocalDate, MealType) -> Unit,
    onEditMeal: (MealEntry) -> Unit,
    onDeleteMeal: (MealEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.diet_title)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (uiState is DietUiState.Ready) {
                ExtendedFloatingActionButton(
                    onClick = { onAddMeal(uiState.date, uiState.suggestedType) },
                    icon = { Icon(painter = painterResource(R.drawable.ic_add), contentDescription = null) },
                    text = { Text(stringResource(R.string.diet_add_meal)) },
                )
            }
        },
    ) { innerPadding ->
        when (uiState) {
            DietUiState.Loading -> LoadingState(Modifier.padding(innerPadding))

            is DietUiState.Ready -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    DayNavigator(
                        date = uiState.date,
                        daysAgo = uiState.daysAgo,
                        onPreviousDay = onPreviousDay,
                        onNextDay = onNextDay,
                        onToday = onToday,
                    )
                }
                item { IntakeSummary(state = uiState) }
                uiState.sections.forEach { section ->
                    item(key = "header-${section.type}") {
                        MealSectionHeader(section = section, onAdd = { onAddMeal(uiState.date, section.type) })
                    }
                    if (section.meals.isEmpty()) {
                        item(key = "empty-${section.type}") {
                            Text(
                                text = stringResource(R.string.diet_section_empty),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                            )
                        }
                    } else {
                        items(section.meals, key = { it.id }) { meal ->
                            MealRow(meal = meal, onEdit = { onEditMeal(meal) }, onDelete = { onDeleteMeal(meal) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayNavigator(
    date: LocalDate,
    daysAgo: Int,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onToday: () -> Unit,
) {
    val isToday = daysAgo == 0
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPreviousDay) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_left),
                contentDescription = stringResource(R.string.diet_previous_day),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(
                    enabled = !isToday,
                    onClickLabel = stringResource(R.string.diet_show_today),
                    onClick = onToday,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = when (daysAgo) {
                    0 -> stringResource(R.string.today)
                    1 -> stringResource(R.string.yesterday)
                    else -> dateText(date, skeleton = "EEEE")
                },
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = dateText(date, skeleton = "yMMMd"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onNextDay, enabled = !isToday) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = stringResource(R.string.diet_next_day),
            )
        }
    }
}

@Composable
private fun IntakeSummary(state: DietUiState.Ready) {
    val total = state.totalKcal
    val target = state.metrics?.dailyTargetKcal
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = stringResource(R.string.diet_intake_label), style = MaterialTheme.typography.labelLarge)
            if (target == null) {
                Text(text = kcalText(total), style = MaterialTheme.typography.headlineSmall)
            } else {
                Text(
                    text = stringResource(R.string.diet_intake_of_target, kcalText(total), kcalText(target)),
                    style = MaterialTheme.typography.headlineSmall,
                )
                val over = total > target
                LinearProgressIndicator(
                    progress = { (total.toFloat() / target).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = if (over) {
                        stringResource(R.string.diet_over_target, kcalText(total - target))
                    } else {
                        stringResource(R.string.diet_remaining, kcalText(target - total))
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            state.metrics?.let { metrics ->
                Text(
                    text = stringResource(
                        R.string.diet_target_basis,
                        bmiText(metrics.bmi),
                        stringResource(metrics.bmiCategory.label),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!state.hasMeals) {
                Text(
                    text = stringResource(R.string.diet_no_meals_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MealSectionHeader(section: MealSection, onAdd: () -> Unit) {
    val typeName = stringResource(section.type.label)
    SectionHeader(
        title = if (section.meals.isEmpty()) {
            typeName
        } else {
            stringResource(
                R.string.diet_section_title,
                typeName,
                kcalText(section.totalKcal),
            )
        },
        modifier = Modifier.padding(top = 8.dp),
        action = {
            IconButton(onClick = onAdd) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = stringResource(R.string.diet_add_to, typeName),
                )
            }
        },
    )
}

@Composable
private fun MealRow(meal: MealEntry, onEdit: () -> Unit, onDelete: () -> Unit) {
    ListItem(
        headlineContent = { Text(text = meal.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = kcalText(meal.kcal), style = MaterialTheme.typography.bodyMedium)
                IconButton(onClick = onDelete) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = stringResource(R.string.diet_delete_meal, meal.name),
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClickLabel = stringResource(R.string.diet_edit_meal), onClick = onEdit),
    )
}

@PreviewLightDark
@Composable
private fun DietScreenPreview() {
    val today = LocalDate.of(2026, 9, 23)
    val meals = listOf(
        MealEntry(1, today, MealType.BREAKFAST, "Greek yogurt with berries", 320),
        MealEntry(2, today, MealType.LUNCH, "Bibimbap", 560),
        MealEntry(3, today, MealType.LUNCH, "Barley tea", 0),
    )
    BeatingYesterdayTheme(dynamicColor = false) {
        DietScreen(
            uiState = DietUiState.Ready(
                date = today,
                daysAgo = 0,
                sections = MealType.entries.map { type -> MealSection(type, meals.filter { it.type == type }) },
                metrics = HealthMetrics(22.4, BmiCategory.NORMAL, 1_420, 2_200, 2_200),
                suggestedType = MealType.DINNER,
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onPreviousDay = {},
            onNextDay = {},
            onToday = {},
            onAddMeal = { _, _ -> },
            onEditMeal = {},
            onDeleteMeal = {},
        )
    }
}
