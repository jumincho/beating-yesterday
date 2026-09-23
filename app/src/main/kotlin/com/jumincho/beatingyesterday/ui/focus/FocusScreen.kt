package com.jumincho.beatingyesterday.ui.focus

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jumincho.beatingyesterday.R
import com.jumincho.beatingyesterday.core.model.FocusSession
import com.jumincho.beatingyesterday.core.timer.TimerMode
import com.jumincho.beatingyesterday.ui.AppViewModelProvider
import com.jumincho.beatingyesterday.ui.components.KeepScreenOn
import com.jumincho.beatingyesterday.ui.components.LoadingState
import com.jumincho.beatingyesterday.ui.components.SectionHeader
import com.jumincho.beatingyesterday.ui.format.clockText
import com.jumincho.beatingyesterday.ui.format.durationText
import com.jumincho.beatingyesterday.ui.format.timeText
import com.jumincho.beatingyesterday.ui.theme.BeatingYesterdayTheme
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

private const val CUSTOM_MIN_MINUTES = 5
private const val CUSTOM_MAX_MINUTES = 180
private const val CUSTOM_STEP_MINUTES = 5

/** Focus: a countdown or stopwatch whose sessions count towards the Focus round. */
@Composable
fun FocusScreen(viewModel: FocusViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val messageText = message?.let { focusMessageText(it) }
    val undoLabel = stringResource(R.string.action_undo)

    LaunchedEffect(message) {
        val current = message ?: return@LaunchedEffect
        val text = messageText ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = text,
            actionLabel = if (current == FocusMessage.SessionDeleted) undoLabel else null,
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) viewModel.undoDeleteSession()
        viewModel.onMessageShown()
    }
    if (uiState.status == TimerStatus.RUNNING) KeepScreenOn()

    FocusScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onSelectTimer = viewModel::selectTimer,
        onSelectStopwatch = viewModel::selectStopwatch,
        onSelectCountdown = viewModel::selectCountdown,
        onStart = viewModel::start,
        onPause = viewModel::pause,
        onResume = viewModel::resume,
        onStop = viewModel::stop,
        onDeleteSession = viewModel::deleteSession,
    )
}

/** Stateless Focus screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(
    uiState: FocusUiState,
    snackbarHostState: SnackbarHostState,
    onSelectTimer: () -> Unit,
    onSelectStopwatch: () -> Unit,
    onSelectCountdown: (minutes: Int) -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onDeleteSession: (FocusSession) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showCustomDuration by rememberSaveable { mutableStateOf(false) }
    val countdown = uiState.mode as? TimerMode.Countdown
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.focus_title)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        if (uiState.isLoading) {
            LoadingState(Modifier.padding(innerPadding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    ModeSelector(
                        isCountdown = countdown != null,
                        enabled = uiState.status == TimerStatus.IDLE,
                        onSelectTimer = onSelectTimer,
                        onSelectStopwatch = onSelectStopwatch,
                    )
                }
                if (uiState.status == TimerStatus.IDLE && countdown != null) {
                    item {
                        DurationPicker(
                            selected = countdown.duration,
                            onPreset = { onSelectCountdown(it.inWholeMinutes.toInt()) },
                            onCustom = { showCustomDuration = true },
                        )
                    }
                }
                item { TimerRing(state = uiState) }
                item {
                    TimerControls(
                        status = uiState.status,
                        onStart = onStart,
                        onPause = onPause,
                        onResume = onResume,
                        onStop = onStop,
                    )
                }
                item {
                    SectionHeader(
                        title = stringResource(R.string.focus_sessions_title),
                        action = {
                            Text(
                                text = stringResource(
                                    R.string.focus_sessions_summary,
                                    durationText(uiState.todayTotal),
                                    pluralStringResource(
                                        R.plurals.focus_session_count,
                                        uiState.sessions.size,
                                        uiState.sessions.size,
                                    ),
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                }
                if (uiState.sessions.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.focus_sessions_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    items(uiState.sessions, key = { it.session.id }) { item ->
                        SessionRow(item = item, onDelete = { onDeleteSession(item.session) })
                    }
                }
            }
        }
    }
    if (showCustomDuration) {
        CustomDurationDialog(
            initialMinutes = countdown?.duration?.inWholeMinutes?.toInt() ?: CUSTOM_MIN_MINUTES,
            onConfirm = { minutes ->
                onSelectCountdown(minutes)
                showCustomDuration = false
            },
            onDismiss = { showCustomDuration = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeSelector(
    isCountdown: Boolean,
    enabled: Boolean,
    onSelectTimer: () -> Unit,
    onSelectStopwatch: () -> Unit,
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SegmentedButton(
            selected = isCountdown,
            onClick = onSelectTimer,
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            enabled = enabled,
            label = { Text(stringResource(R.string.focus_mode_timer)) },
        )
        SegmentedButton(
            selected = !isCountdown,
            onClick = onSelectStopwatch,
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            enabled = enabled,
            label = { Text(stringResource(R.string.focus_mode_stopwatch)) },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DurationPicker(selected: Duration, onPreset: (Duration) -> Unit, onCustom: () -> Unit) {
    val isCustom = selected !in TimerMode.Countdown.PRESETS
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        TimerMode.Countdown.PRESETS.forEach { preset ->
            FilterChip(
                selected = selected == preset,
                onClick = { onPreset(preset) },
                label = { Text(durationText(preset)) },
            )
        }
        FilterChip(
            selected = isCustom,
            onClick = onCustom,
            label = {
                Text(
                    if (isCustom) {
                        stringResource(R.string.focus_custom_selected, durationText(selected))
                    } else {
                        stringResource(R.string.focus_custom)
                    },
                )
            },
        )
    }
}

@Composable
private fun TimerRing(state: FocusUiState) {
    val display = if (state.mode is TimerMode.Countdown) {
        clockText(state.remaining ?: Duration.ZERO, roundUp = true)
    } else {
        clockText(state.elapsed)
    }
    val status = stringResource(
        when (state.status) {
            TimerStatus.IDLE -> R.string.focus_status_ready
            TimerStatus.RUNNING -> R.string.focus_status_running
            TimerStatus.PAUSED -> R.string.focus_status_paused
        },
    )
    ProgressRing(
        progress = state.progress,
        modifier = Modifier
            .size(264.dp)
            .progressSemantics(state.progress)
            .semantics(mergeDescendants = true) { stateDescription = "$display, $status" },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = display, style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = "tnum"))
            Text(
                text = status,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TimerControls(
    status: TimerStatus,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
) {
    val buttonModifier = Modifier.heightIn(min = 56.dp)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        when (status) {
            TimerStatus.IDLE -> Button(onClick = onStart, modifier = buttonModifier) {
                ButtonIcon(R.drawable.ic_play_arrow)
                Text(stringResource(R.string.focus_start))
            }

            TimerStatus.RUNNING -> {
                FilledTonalButton(onClick = onPause, modifier = buttonModifier) {
                    ButtonIcon(R.drawable.ic_pause)
                    Text(stringResource(R.string.focus_pause))
                }
                OutlinedButton(onClick = onStop, modifier = buttonModifier) {
                    ButtonIcon(R.drawable.ic_stop)
                    Text(stringResource(R.string.focus_stop))
                }
            }

            TimerStatus.PAUSED -> {
                Button(onClick = onResume, modifier = buttonModifier) {
                    ButtonIcon(R.drawable.ic_play_arrow)
                    Text(stringResource(R.string.focus_resume))
                }
                OutlinedButton(onClick = onStop, modifier = buttonModifier) {
                    ButtonIcon(R.drawable.ic_stop)
                    Text(stringResource(R.string.focus_stop))
                }
            }
        }
    }
}

@Composable
private fun ButtonIcon(@DrawableRes icon: Int) {
    Icon(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
    Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
}

@Composable
private fun SessionRow(item: SessionItem, onDelete: () -> Unit) {
    val start = timeText(item.start)
    ListItem(
        headlineContent = { Text(stringResource(R.string.focus_session_range, start, timeText(item.end))) },
        supportingContent = { Text(durationText(item.session.duration)) },
        leadingContent = { Icon(painter = painterResource(R.drawable.ic_timer), contentDescription = null) },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = stringResource(R.string.focus_delete_session, start),
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun CustomDurationDialog(initialMinutes: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var minutes by rememberSaveable {
        val stepped = initialMinutes / CUSTOM_STEP_MINUTES * CUSTOM_STEP_MINUTES
        mutableIntStateOf(stepped.coerceIn(CUSTOM_MIN_MINUTES, CUSTOM_MAX_MINUTES))
    }
    val label = durationText(minutes.minutes)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.focus_custom_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = label, style = MaterialTheme.typography.headlineSmall)
                Slider(
                    value = minutes.toFloat(),
                    onValueChange = { minutes = it.roundToInt() },
                    valueRange = CUSTOM_MIN_MINUTES.toFloat()..CUSTOM_MAX_MINUTES.toFloat(),
                    steps = (CUSTOM_MAX_MINUTES - CUSTOM_MIN_MINUTES) / CUSTOM_STEP_MINUTES - 1,
                    modifier = Modifier.semantics { stateDescription = label },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(minutes) }) { Text(stringResource(R.string.action_done)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun focusMessageText(message: FocusMessage): String = when (message) {
    is FocusMessage.Saved -> stringResource(R.string.focus_message_saved, durationText(message.duration))
    FocusMessage.TooShort -> stringResource(R.string.focus_message_too_short)
    is FocusMessage.Completed -> stringResource(R.string.focus_message_completed, durationText(message.duration))
    FocusMessage.SessionDeleted -> stringResource(R.string.focus_message_deleted)
}

@PreviewLightDark
@Composable
private fun FocusScreenPreview() {
    val today = LocalDate.of(2026, 9, 23)
    val session = FocusSession(
        id = 1,
        date = today,
        startedAt = Instant.parse("2026-09-23T00:10:00Z"),
        endedAt = Instant.parse("2026-09-23T00:35:00Z"),
        duration = 25.minutes,
    )
    BeatingYesterdayTheme(dynamicColor = false) {
        FocusScreen(
            uiState = FocusUiState(
                isLoading = false,
                status = TimerStatus.RUNNING,
                mode = TimerMode.Countdown(50.minutes),
                elapsed = 12.minutes,
                remaining = 38.minutes,
                progress = 38f / 50f,
                sessions = listOf(SessionItem(session, LocalTime.of(9, 10), LocalTime.of(9, 35))),
                todayTotal = 25.minutes,
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onSelectTimer = {},
            onSelectStopwatch = {},
            onSelectCountdown = {},
            onStart = {},
            onPause = {},
            onResume = {},
            onStop = {},
            onDeleteSession = {},
        )
    }
}
