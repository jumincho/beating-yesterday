package com.jumincho.beatingyesterday.core.model

import java.time.LocalDate
import java.time.LocalTime

/** The slot of the day a meal belongs to, in display order. */
enum class MealType {
    BREAKFAST,
    LUNCH,
    DINNER,
    SNACK,
    ;

    companion object {
        /** The meal most likely being logged at [time]: breakfast before 10:30, lunch before 15:00, dinner before 21:00. */
        fun suggestedFor(time: LocalTime): MealType = when {
            time < LocalTime.of(10, 30) -> BREAKFAST
            time < LocalTime.of(15, 0) -> LUNCH
            time < LocalTime.of(21, 0) -> DINNER
            else -> SNACK
        }
    }
}

/**
 * One logged food or meal.
 *
 * @property id storage identifier; `0` for an entry that has not been saved yet.
 * @property date the local day the meal was eaten on.
 * @property kcal energy in kilocalories, never negative.
 */
data class MealEntry(val id: Long = 0, val date: LocalDate, val type: MealType, val name: String, val kcal: Int)
