package com.jumincho.beatingyesterday.core.model

import java.time.LocalDate

/**
 * A task planned for a day.
 *
 * Unfinished tasks carry over: a task created on an earlier day stays on every following day's
 * list until it is completed or deleted. A completed task counts towards the day it was
 * completed on, so carrying it over never counts it twice.
 *
 * @property id storage identifier; `0` for a task that has not been saved yet.
 * @property createdOn the day the task was added.
 * @property completedOn the day the task was ticked off, or `null` while it is open.
 */
data class TodoItem(
    val id: Long = 0,
    val title: String,
    val createdOn: LocalDate,
    val completedOn: LocalDate? = null,
) {
    /** Whether the task has been completed. */
    val isDone: Boolean get() = completedOn != null

    /** Whether this open task was carried over from a day before [date]. */
    fun isCarriedOverTo(date: LocalDate): Boolean = completedOn == null && createdOn < date

    companion object {
        /** Longest title accepted, in characters. */
        const val TITLE_MAX_LENGTH = 100
    }
}
