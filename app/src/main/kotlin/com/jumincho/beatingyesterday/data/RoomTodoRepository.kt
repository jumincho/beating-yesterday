package com.jumincho.beatingyesterday.data

import com.jumincho.beatingyesterday.core.data.TodoRepository
import com.jumincho.beatingyesterday.core.model.TodoItem
import com.jumincho.beatingyesterday.data.local.TodoDao
import com.jumincho.beatingyesterday.data.local.TodoEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** [TodoRepository] backed by Room. */
class RoomTodoRepository(private val dao: TodoDao) : TodoRepository {

    override fun observeTodos(date: LocalDate): Flow<List<TodoItem>> =
        dao.observeForDate(date).map { rows -> rows.map { it.toModel() } }

    override fun observeDailyCompletions(): Flow<Map<LocalDate, Int>> =
        dao.observeDailyCompletions().map { rows -> rows.associate { it.date to it.count } }

    override suspend fun saveTodo(item: TodoItem) {
        dao.upsert(TodoEntity(item.id, item.title, item.createdOn, item.completedOn))
    }

    override suspend fun setCompleted(id: Long, completedOn: LocalDate?) {
        dao.setCompletedOn(id, completedOn)
    }

    override suspend fun deleteTodo(id: Long) {
        dao.deleteById(id)
    }

    private fun TodoEntity.toModel() = TodoItem(id, title, createdOn, completedOn)
}
