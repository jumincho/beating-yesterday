package com.jumincho.beatingyesterday.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.jumincho.beatingyesterday.core.model.MealType
import java.time.Instant
import java.time.LocalDate

/** Row of the `meals` table. */
@Entity(tableName = "meals", indices = [Index("date")])
data class MealEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val type: MealType,
    val name: String,
    val kcal: Int,
)

/** Row of the `focus_sessions` table. */
@Entity(tableName = "focus_sessions", indices = [Index("date")])
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    @ColumnInfo(name = "started_at") val startedAt: Instant,
    @ColumnInfo(name = "ended_at") val endedAt: Instant,
    @ColumnInfo(name = "duration_ms") val durationMillis: Long,
)

/** Row of the `todos` table. */
@Entity(tableName = "todos", indices = [Index("created_on"), Index("completed_on")])
data class TodoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    @ColumnInfo(name = "created_on") val createdOn: LocalDate,
    @ColumnInfo(name = "completed_on") val completedOn: LocalDate?,
)

/** Calories eaten on [date]. */
data class DailyKcal(val date: LocalDate, @ColumnInfo(name = "total_kcal") val totalKcal: Int)

/** Focused milliseconds on [date]. */
data class DailyFocus(val date: LocalDate, @ColumnInfo(name = "total_ms") val totalMillis: Long)

/** Tasks completed on [date]. */
data class DailyCompletions(val date: LocalDate, val count: Int)
