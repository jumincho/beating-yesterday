package com.jumincho.beatingyesterday.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Access to the `meals` table. */
@Dao
interface MealDao {
    @Query("SELECT * FROM meals WHERE date = :date ORDER BY id")
    fun observeByDate(date: LocalDate): Flow<List<MealEntity>>

    @Query("SELECT date, SUM(kcal) AS total_kcal FROM meals GROUP BY date")
    fun observeDailyTotals(): Flow<List<DailyKcal>>

    @Query("SELECT * FROM meals WHERE id = :id")
    suspend fun findById(id: Long): MealEntity?

    @Upsert
    suspend fun upsert(meal: MealEntity)

    @Query("DELETE FROM meals WHERE id = :id")
    suspend fun deleteById(id: Long)
}

/** Access to the `focus_sessions` table. */
@Dao
interface FocusSessionDao {
    @Query("SELECT * FROM focus_sessions WHERE date = :date ORDER BY started_at")
    fun observeByDate(date: LocalDate): Flow<List<FocusSessionEntity>>

    @Query("SELECT date, SUM(duration_ms) AS total_ms FROM focus_sessions GROUP BY date")
    fun observeDailyTotals(): Flow<List<DailyFocus>>

    @Upsert
    suspend fun upsert(session: FocusSessionEntity)

    @Query("DELETE FROM focus_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)
}

/** Access to the `todos` table. */
@Dao
interface TodoDao {
    /** Open tasks created on or before [date], then tasks completed on [date]. */
    @Query(
        """
        SELECT * FROM todos
        WHERE (completed_on IS NULL AND created_on <= :date) OR completed_on = :date
        ORDER BY completed_on IS NOT NULL, id
        """,
    )
    fun observeForDate(date: LocalDate): Flow<List<TodoEntity>>

    @Query(
        """
        SELECT completed_on AS date, COUNT(*) AS count FROM todos
        WHERE completed_on IS NOT NULL
        GROUP BY completed_on
        """,
    )
    fun observeDailyCompletions(): Flow<List<DailyCompletions>>

    @Upsert
    suspend fun upsert(todo: TodoEntity)

    @Query("UPDATE todos SET completed_on = :completedOn WHERE id = :id")
    suspend fun setCompletedOn(id: Long, completedOn: LocalDate?)

    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun deleteById(id: Long)
}
