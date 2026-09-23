package com.jumincho.beatingyesterday.core.model

import java.time.LocalDate

/** Biological sex, used only to pick the matching Mifflin–St Jeor constant and calorie floor. */
enum class Sex { MALE, FEMALE }

/**
 * Self-reported physical activity level.
 *
 * @property multiplier factor that scales the basal metabolic rate up to total daily energy
 * expenditure (the widely used 1.2–1.9 activity multipliers).
 */
enum class ActivityLevel(val multiplier: Double) {
    /** Little or no exercise. */
    SEDENTARY(1.2),

    /** Light exercise one to three days a week. */
    LIGHT(1.375),

    /** Moderate exercise three to five days a week. */
    MODERATE(1.55),

    /** Hard exercise six to seven days a week. */
    ACTIVE(1.725),

    /** Very hard exercise every day or a physically demanding job. */
    VERY_ACTIVE(1.9),
}

/**
 * The person whose days are being compared.
 *
 * Instances are expected to have passed [com.jumincho.beatingyesterday.core.profile.ProfileValidator],
 * so every value is within its supported range.
 */
data class UserProfile(
    val name: String,
    val sex: Sex,
    val birthYear: Int,
    val heightCm: Double,
    val weightKg: Double,
    val activityLevel: ActivityLevel,
) {
    /** Age in whole years on [date], approximated as the difference between calendar years. */
    fun ageOn(date: LocalDate): Int = date.year - birthYear
}
