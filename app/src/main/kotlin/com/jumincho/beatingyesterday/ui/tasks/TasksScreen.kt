package com.jumincho.beatingyesterday.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jumincho.beatingyesterday.R
import com.jumincho.beatingyesterday.core.model.TodoItem
import com.jumincho.beatingyesterday.ui.AppViewModelProvider
import com.jumincho.beatingyesterday.ui.components.EmptyState
import com.jumincho.beatingyesterday.ui.components.LoadingState
import com.jumincho.beatingyesterday.ui.format.dateText
import com.jumincho.beatingyesterday.ui.theme.BeatingYesterdayTheme
import java.time.LocalDate

/** Tasks: today's to-do list; completed tasks count towards the Tasks round. */
@Composable
fun TasksScreen(viewModel: TasksViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val deletedTask by viewModel.deletedTask.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val deletedMessage = deletedTask?.let { stringResource(R.string.tasks_deleted, it.title) }
    val undoLabel = stringResource(R.string.action_undo)

    LaunchedEffect(deletedTask) {
        val message = deletedMessage ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(message, actionLabel = undoLabel, duration = SnackbarDuration.Short)
        if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete() else viewModel.onDeletedMessageShown()
    }

    TasksScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onAddTask = viewModel::addTask,
        onToggle = viewModel::toggle,
        onDelete = viewModel::delete,
    )
}

/** Stateless Tasks screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    uiState: TasksUiState,
    snackbarHostState: SnackbarHostState,
    onAddTask: (String) -> Unit,
    onToggle: (TodoItem) -> Unit,
    onDelete: (TodoItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tasks_title)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = { AddTaskBar(enabled = !uiState.isLoading, onAddTask = onAddTask) },
    ) { innerPadding ->
        val today = uiState.today
        when {
            uiState.isLoading || today == null -> LoadingState(Modifier.padding(innerPadding))

            uiState.tasks.isEmpty() -> EmptyState(
                icon = R.drawable.ic_task_alt,
                title = stringResource(R.string.tasks_empty_title),
                body = stringResource(R.string.tasks_empty_body),
                modifier = Modifier.padding(innerPadding),
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                item { TasksSummary(completed = uiState.completedCount, total = uiState.tasks.size) }
                items(uiState.tasks, key = { it.id }) { task ->
                    TaskRow(task = task, today = today, onToggle = { onToggle(task) }, onDelete = { onDelete(task) })
                }
            }
        }
    }
}

@Composable
private fun TasksSummary(completed: Int, total: Int) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.tasks_summary, completed, total),
            style = MaterialTheme.typography.titleMedium,
        )
        LinearProgressIndicator(
            progress = { if (total == 0) 0f else completed.toFloat() / total },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun TaskRow(task: TodoItem, today: LocalDate, onToggle: () -> Unit, onDelete: () -> Unit) {
    val done = task.isDone
    ListItem(
        headlineContent = {
            Text(
                text = task.title,
                textDecoration = if (done) TextDecoration.LineThrough else null,
                color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
        },
        supportingContent = if (task.isCarriedOverTo(today)) {
            { Text(stringResource(R.string.tasks_carried_over, dateText(task.createdOn, skeleton = "MMMd"))) }
        } else {
            null
        },
        leadingContent = { Checkbox(checked = done, onCheckedChange = null) },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = stringResource(R.string.tasks_delete, task.title),
                )
            }
        },
        modifier = Modifier.toggleable(value = done, role = Role.Checkbox, onValueChange = { onToggle() }),
    )
}

@Composable
private fun AddTaskBar(enabled: Boolean, onAddTask: (String) -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }
    val submit = {
        if (title.isNotBlank()) {
            onAddTask(title)
            title = ""
        }
    }
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= TodoItem.TITLE_MAX_LENGTH) title = it },
                modifier = Modifier.weight(1f),
                enabled = enabled,
                placeholder = { Text(stringResource(R.string.tasks_new_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
            )
            IconButton(onClick = submit, enabled = enabled && title.isNotBlank()) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = stringResource(R.string.tasks_add),
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun TasksScreenPreview() {
    val today = LocalDate.of(2026, 9, 23)
    BeatingYesterdayTheme(dynamicColor = false) {
        TasksScreen(
            uiState = TasksUiState(
                isLoading = false,
                today = today,
                tasks = listOf(
                    TodoItem(1, "Finish the statistics assignment", today.minusDays(1)),
                    TodoItem(2, "Read two chapters", today),
                    TodoItem(3, "Go for a 20-minute walk", today, completedOn = today),
                ),
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onAddTask = {},
            onToggle = {},
            onDelete = {},
        )
    }
}
