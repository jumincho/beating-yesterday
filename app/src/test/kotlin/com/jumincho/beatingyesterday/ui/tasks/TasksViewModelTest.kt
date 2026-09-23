package com.jumincho.beatingyesterday.ui.tasks

import com.jumincho.beatingyesterday.core.model.TodoItem
import com.jumincho.beatingyesterday.core.testing.FakeTodoRepository
import com.jumincho.beatingyesterday.testing.MainDispatcherExtension
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class TasksViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val today = LocalDate.of(2026, 9, 23)
    private val yesterday = today.minusDays(1)
    private val todos = FakeTodoRepository(
        listOf(
            TodoItem(1, "Finish the report", createdOn = yesterday),
            TodoItem(2, "Stretch", createdOn = yesterday, completedOn = yesterday),
            TodoItem(3, "Read", createdOn = today),
        ),
    )

    private fun TestScope.createViewModel(date: MutableStateFlow<LocalDate> = MutableStateFlow(today)) =
        TasksViewModel(todos, date).also { viewModel ->
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        }

    private fun TasksViewModel.titles(): List<String> = uiState.value.tasks.map { it.title }

    @Test
    fun `lists open tasks, including ones carried over, but not tasks finished on earlier days`() = runTest {
        val viewModel = createViewModel()

        assertEquals(listOf("Finish the report", "Read"), viewModel.titles())
        assertEquals(0, viewModel.uiState.value.completedCount)
    }

    @Test
    fun `adds trimmed tasks for today and ignores blank ones`() = runTest {
        val viewModel = createViewModel()

        viewModel.addTask("  Call grandma  ")
        viewModel.addTask("   ")

        assertEquals(listOf("Finish the report", "Read", "Call grandma"), viewModel.titles())
        assertEquals(today, todos.all.last().createdOn)
    }

    @Test
    fun `completing a task counts it today and reopening it undoes that`() = runTest {
        val viewModel = createViewModel()
        val report = viewModel.uiState.value.tasks.first()

        viewModel.toggle(report)
        assertEquals(today, todos.all.first { it.id == report.id }.completedOn)
        assertEquals(1, viewModel.uiState.value.completedCount)
        assertEquals(listOf("Read", "Finish the report"), viewModel.titles())

        viewModel.toggle(viewModel.uiState.value.tasks.last())
        assertNull(todos.all.first { it.id == report.id }.completedOn)
        assertEquals(0, viewModel.uiState.value.completedCount)
    }

    @Test
    fun `the list follows the new day`() = runTest {
        val date = MutableStateFlow(today)
        val viewModel = createViewModel(date)
        viewModel.toggle(viewModel.uiState.value.tasks.first())

        date.value = today.plusDays(1)

        assertEquals(listOf("Read"), viewModel.titles())
    }

    @Test
    fun `deleting a task can be undone`() = runTest {
        val viewModel = createViewModel()
        val read = viewModel.uiState.value.tasks.last()

        viewModel.delete(read)
        assertEquals(read, viewModel.deletedTask.value)
        assertEquals(listOf("Finish the report"), viewModel.titles())

        viewModel.undoDelete()
        assertEquals(listOf("Finish the report", "Read"), viewModel.titles())
        assertNull(viewModel.deletedTask.value)
    }
}
