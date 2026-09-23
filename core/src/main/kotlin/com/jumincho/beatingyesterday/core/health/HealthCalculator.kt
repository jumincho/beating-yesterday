package com.jumincho.beatingyesterday.core.health

import com.jumincho.beatingyesterday.core.model.ActivityLevel
import com.jumincho.beatingyesterday.core.model.Sex
import com.jumincho.beatingyesterday.core.model.UserProfile
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Everything the app derives from a [UserProfile] on a given day.
 *
 * @property bmi body mass index rounded to one decimal place.
 * @property bmiCategory the category of the rounded [bmi], so the number shown and the label
 * shown always agree.
 * @property basalMetabolicRateKcal Mifflin–St Jeor resting energy expenditure.
 * @property totalDailyEnergyExpenditureKcal [basalMetabolicRateKcal] scaled by the activity level.
 * @property dailyTargetKcal the calorie goal the Diet round is judged against.
 */
data class HealthMetrics(
    val bmi: Double,
    val bmiCategory: BmiCategory,
    val basalMetabolicRateKcal: Int,
    val totalDailyEnergyExpenditureKcal: Int,
    val dailyTargetKcal: Int,
)

/**
 * Pure health formulas. These are general population estimates, not medical advice.
 *
 * - **BMI** = weight (kg) / height (m)².
 * - **BMR** (Mifflin–St Jeor): `10 × weight + 6.25 × height − 5 × age + s`, with `s = +5` for men
 *   and `s = −161` for women. Source: Mifflin MD, St Jeor ST, et al. *A new predictive equation
 *   for resting energy expenditure in healthy individuals.* Am J Clin Nutr. 1990;51(2):241–247.
 * - **TDEE** = BMR × [ActivityLevel.multiplier].
 * - **Daily target** = TDEE adjusted by BMI category (underweight +300, normal ±0,
 *   overweight −300, obese −500 kcal), never below 1,500 kcal for men or 1,200 kcal for women.
 *   The adjustments and floors are this app's own conservative choices.
 */
object HealthCalculator {

    /** Lowest daily target suggested to men. */
    const val MIN_TARGET_KCAL_MALE = 1_500

    /** Lowest daily target suggested to women. */
    const val MIN_TARGET_KCAL_FEMALE = 1_200

    /** Body mass index in kg/m², unrounded. */
    fun bmi(weightKg: Double, heightCm: Double): Double {
        require(weightKg > 0) { "weightKg must be positive but was $weightKg" }
        require(heightCm > 0) { "heightCm must be positive but was $heightCm" }
        val heightM = heightCm / 100
        return weightKg / (heightM * heightM)
    }

    /** Basal metabolic rate in kcal/day using the Mifflin–St Jeor equation. */
    fun basalMetabolicRate(sex: Sex, weightKg: Double, heightCm: Double, ageYears: Int): Double {
        val base = 10 * weightKg + 6.25 * heightCm - 5 * ageYears
        return when (sex) {
            Sex.MALE -> base + 5
            Sex.FEMALE -> base - 161
        }
    }

    /** Total daily energy expenditure in kcal/day. */
    fun totalDailyEnergyExpenditure(basalMetabolicRate: Double, activityLevel: ActivityLevel): Double =
        basalMetabolicRate * activityLevel.multiplier

    /** Calories added to (or removed from) TDEE for a [category]. */
    fun calorieAdjustment(category: BmiCategory): Int = when (category) {
        BmiCategory.UNDERWEIGHT -> 300
        BmiCategory.NORMAL -> 0
        BmiCategory.OVERWEIGHT -> -300
        BmiCategory.OBESE -> -500
    }

    /** The floor applied to the daily target for [sex]. */
    fun minimumTarget(sex: Sex): Int = when (sex) {
        Sex.MALE -> MIN_TARGET_KCAL_MALE
        Sex.FEMALE -> MIN_TARGET_KCAL_FEMALE
    }

    /** Daily calorie target: [totalDailyEnergyExpenditure] adjusted for [category], floored by [sex]. */
    fun dailyCalorieTarget(totalDailyEnergyExpenditure: Double, category: BmiCategory, sex: Sex): Int {
        val adjusted = (totalDailyEnergyExpenditure + calorieAdjustment(category)).roundToInt()
        return maxOf(adjusted, minimumTarget(sex))
    }

    /** Computes every [HealthMetrics] value for [profile] as of [date]. */
    fun metricsFor(profile: UserProfile, date: LocalDate): HealthMetrics {
        val bmi = roundToOneDecimal(bmi(profile.weightKg, profile.heightCm))
        val category = BmiCategory.of(bmi)
        val bmr = basalMetabolicRate(profile.sex, profile.weightKg, profile.heightCm, profile.ageOn(date))
        val tdee = totalDailyEnergyExpenditure(bmr, profile.activityLevel)
        return HealthMetrics(
            bmi = bmi,
            bmiCategory = category,
            basalMetabolicRateKcal = bmr.roundToInt(),
            totalDailyEnergyExpenditureKcal = tdee.roundToInt(),
            dailyTargetKcal = dailyCalorieTarget(tdee, category, profile.sex),
        )
    }

    private fun roundToOneDecimal(value: Double): Double = (value * 10).roundToInt() / 10.0
}
