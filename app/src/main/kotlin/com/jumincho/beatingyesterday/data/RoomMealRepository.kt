package com.jumincho.beatingyesterday.data

import com.jumincho.beatingyesterday.core.data.MealRepository
import com.jumincho.beatingyesterday.core.model.MealEntry
import com.jumincho.beatingyesterday.data.local.MealDao
import com.jumincho.beatingyesterday.data.local.MealEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** [MealRepository] backed by Room. */
class RoomMealRepository(private val dao: MealDao) : MealRepository {

    override fun observeMeals(date: LocalDate): Flow<List<MealEntry>> =
        dao.observeByDate(date).map { rows -> rows.map { it.toModel() } }

    override fun observeDailyIntake(): Flow<Map<LocalDate, Int>> =
        dao.observeDailyTotals().map { rows -> rows.associate { it.date to it.totalKcal } }

    override suspend fun getMeal(id: Long): MealEntry? = dao.findById(id)?.toModel()

    override suspend fun saveMeal(meal: MealEntry) {
        dao.upsert(MealEntity(meal.id, meal.date, meal.type, meal.name, meal.kcal))
    }

    override suspend fun deleteMeal(id: Long) {
        dao.deleteById(id)
    }

    private fun MealEntity.toModel() = MealEntry(id, date, type, name, kcal)
}
