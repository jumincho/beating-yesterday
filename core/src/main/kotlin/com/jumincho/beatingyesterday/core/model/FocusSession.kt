package com.jumincho.beatingyesterday.core.model

import java.time.Instant
import java.time.LocalDate
import kotlin.time.Duration

/**
 * A finished block of focused time.
 *
 * @property id storage identifier; `0` for a session that has not been saved yet.
 * @property date the local day the session started on; the whole session counts towards it.
 * @property duration focused time, excluding pauses, so it can be shorter than
 * [endedAt] minus [startedAt].
 */
data class FocusSession(
    val id: Long = 0,
    val date: LocalDate,
    val startedAt: Instant,
    val endedAt: Instant,
    val duration: Duration,
)
