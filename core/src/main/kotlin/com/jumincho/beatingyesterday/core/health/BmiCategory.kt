package com.jumincho.beatingyesterday.core.health

/**
 * Body-mass-index bands using the Asia-Pacific cut-offs that the Korean Society for the Study of
 * Obesity (KSSO) also uses. They are lower than the WHO global cut-offs (25 and 30) because
 * obesity-related risk rises at lower BMI in Asian populations.
 *
 * | Category    | BMI (kg/m²)   |
 * |-------------|---------------|
 * | Underweight | < 18.5        |
 * | Normal      | 18.5 – < 23   |
 * | Overweight  | 23 – < 25     |
 * | Obese       | ≥ 25          |
 *
 * Sources: WHO Western Pacific Region, IASO and IOTF, *The Asia-Pacific perspective: redefining
 * obesity and its treatment* (2000); KSSO clinical practice guidelines for obesity.
 */
enum class BmiCategory {
    UNDERWEIGHT,
    NORMAL,
    OVERWEIGHT,
    OBESE,
    ;

    companion object {
        /** Lower bound of [NORMAL]. */
        const val NORMAL_FROM = 18.5

        /** Lower bound of [OVERWEIGHT]. */
        const val OVERWEIGHT_FROM = 23.0

        /** Lower bound of [OBESE]. */
        const val OBESE_FROM = 25.0

        /** The category a [bmi] value falls into. */
        fun of(bmi: Double): BmiCategory = when {
            bmi < NORMAL_FROM -> UNDERWEIGHT
            bmi < OVERWEIGHT_FROM -> NORMAL
            bmi < OBESE_FROM -> OVERWEIGHT
            else -> OBESE
        }
    }
}
