package com.jumincho.beatingyesterday.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jumincho.beatingyesterday.core.data.TodoRepository
import com.jumincho.beatingyesterday.core.model.TodoItem
import com.jumincho.beatingyesterday.ui.WhileUiSubscribed
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * State of the Tasks screen.
 *
 * @property tasks open tasks (including ones carried over from earlier days) followed by the
 * tasks completed today.
 */
data class TasksUiState(
    val isLoading: Boolean = true,
    val today: LocalDate? = null,
    val tasks: List<TodoItem> = emptyList(),
) {
    /** Tasks completed today. */
    val completedCount: Int get() = tasks.count { it.completedOn != null }
}

/** Today's task list. */
@OptIn(ExperimentalCoroutinesApi::class)
class TasksViewModel(private val todoRepository: TodoRepository, today: Flow<LocalDate>) : ViewModel() {

    private val currentDate: StateFlow<LocalDate?> = today.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val _deletedTask = MutableStateFlow<TodoItem?>(null)

    /** The task just deleted, offered for undo until [onDeletedMessageShown]. */
    val deletedTask: StateFlow<TodoItem?> = _deletedTask.asStateFlow()

    val uiState: StateFlow<TasksUiState> = currentDate.filterNotNull()
        .flatMapLatest { date ->
            todoRepository.observeTodos(date).map { tasks ->
                TasksUiState(isLoading = false, today = date, tasks = tasks)
            }
        }
        .stateIn(viewModelScope, WhileUiSubscribed, TasksUiState())

    /** Adds a task for today; blank titles are ignored and long ones are shortened. */
    fun addTask(title: String) {
        val date = currentDate.value ?: return
        val trimmed = title.trim().take(TodoItem.TITLE_MAX_LENGTH)
        if (trimmed.isEmpty()) return
        viewModelScope.launch { todoRepository.saveTodo(TodoItem(title = trimmed, createdOn = date)) }
    }

    /** Completes an open task today, or reopens a completed one. */
    fun toggle(task: TodoItem) {
        val date = currentDate.value ?: return
        viewModelScope.launch { todoRepository.setCompleted(task.id, if (task.isDone) null else date) }
    }

    fun delete(task: TodoItem) {
        viewModelScope.launch {
            todoRepository.deleteTodo(task.id)
            _deletedTask.value = task
        }
    }

    /** Restores the task deleted last. */
    fun undoDelete() {
        val task = _deletedTask.value ?: return
        _deletedTask.value = null
        viewModelScope.launch { todoRepository.saveTodo(task) }
    }

    fun onDeletedMessageShown() {
        _deletedTask.value = null
    }
}
