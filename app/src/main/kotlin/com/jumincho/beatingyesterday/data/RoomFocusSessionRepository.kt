package com.jumincho.beatingyesterday.data

import com.jumincho.beatingyesterday.core.data.FocusSessionRepository
import com.jumincho.beatingyesterday.core.model.FocusSession
import com.jumincho.beatingyesterday.data.local.FocusSessionDao
import com.jumincho.beatingyesterday.data.local.FocusSessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** [FocusSessionRepository] backed by Room. */
class RoomFocusSessionRepository(private val dao: FocusSessionDao) : FocusSessionRepository {

    override fun observeSessions(date: LocalDate): Flow<List<FocusSession>> =
        dao.observeByDate(date).map { rows -> rows.map { it.toModel() } }

    override fun observeDailyFocus(): Flow<Map<LocalDate, Duration>> =
        dao.observeDailyTotals().map { rows -> rows.associate { it.date to it.totalMillis.milliseconds } }

    override suspend fun saveSession(session: FocusSession) {
        dao.upsert(
            FocusSessionEntity(
                id = session.id,
                date = session.date,
                startedAt = session.startedAt,
                endedAt = session.endedAt,
                durationMillis = session.duration.inWholeMilliseconds,
            ),
        )
    }

    override suspend fun deleteSession(id: Long) {
        dao.deleteById(id)
    }

    private fun FocusSessionEntity.toModel() = FocusSession(id, date, startedAt, endedAt, durationMillis.milliseconds)
}
