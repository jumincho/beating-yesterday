package com.jumincho.beatingyesterday.core.health

import com.jumincho.beatingyesterday.core.model.ActivityLevel
import com.jumincho.beatingyesterday.core.model.Sex
import com.jumincho.beatingyesterday.core.model.UserProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDate

class HealthCalculatorTest {

    @Nested
    inner class Bmi {
        @Test
        fun `is weight over height in metres squared`() {
            assertEquals(70 / (1.75 * 1.75), HealthCalculator.bmi(weightKg = 70.0, heightCm = 175.0), 1e-9)
        }

        @Test
        fun `rejects non-positive input`() {
            assertThrows<IllegalArgumentException> { HealthCalculator.bmi(weightKg = 70.0, heightCm = 0.0) }
            assertThrows<IllegalArgumentException> { HealthCalculator.bmi(weightKg = -1.0, heightCm = 170.0) }
        }

        @ParameterizedTest(name = "BMI {0} is {1}")
        @CsvSource(
            "10.0, UNDERWEIGHT",
            "18.4, UNDERWEIGHT",
            "18.5, NORMAL",
            "22.9, NORMAL",
            "23.0, OVERWEIGHT",
            "24.9, OVERWEIGHT",
            "25.0, OBESE",
            "40.0, OBESE",
        )
        fun `uses the Asia-Pacific cut-offs`(bmi: Double, expected: BmiCategory) {
            assertEquals(expected, BmiCategory.of(bmi))
        }
    }

    @Nested
    inner class EnergyExpenditure {
        @Test
        fun `Mifflin-St Jeor for men adds 5`() {
            // 10 × 70 + 6.25 × 175 − 5 × 30 + 5
            assertEquals(1648.75, HealthCalculator.basalMetabolicRate(Sex.MALE, 70.0, 175.0, 30), 1e-9)
        }

        @Test
        fun `Mifflin-St Jeor for women subtracts 161`() {
            // 10 × 60 + 6.25 × 165 − 5 × 25 − 161
            assertEquals(1345.25, HealthCalculator.basalMetabolicRate(Sex.FEMALE, 60.0, 165.0, 25), 1e-9)
        }

        @ParameterizedTest(name = "{0} multiplies BMR by {1}")
        @CsvSource("SEDENTARY, 1.2", "LIGHT, 1.375", "MODERATE, 1.55", "ACTIVE, 1.725", "VERY_ACTIVE, 1.9")
        fun `TDEE scales BMR by the activity multiplier`(level: ActivityLevel, multiplier: Double) {
            assertEquals(1500 * multiplier, HealthCalculator.totalDailyEnergyExpenditure(1500.0, level), 1e-9)
        }
    }

    @Nested
    inner class DailyTarget {
        @ParameterizedTest(name = "{0} adjusts TDEE by {1} kcal")
        @CsvSource("UNDERWEIGHT, 300", "NORMAL, 0", "OVERWEIGHT, -300", "OBESE, -500")
        fun `adjusts TDEE by BMI category`(category: BmiCategory, adjustment: Int) {
            assertEquals(2500 + adjustment, HealthCalculator.dailyCalorieTarget(2500.0, category, Sex.MALE))
        }

        @Test
        fun `rounds to the nearest kcal`() {
            assertEquals(2256, HealthCalculator.dailyCalorieTarget(2555.5625, BmiCategory.OVERWEIGHT, Sex.MALE))
        }

        @Test
        fun `never goes below the floor for women`() {
            assertEquals(1_200, HealthCalculator.dailyCalorieTarget(1591.8, BmiCategory.OBESE, Sex.FEMALE))
        }

        @Test
        fun `never goes below the floor for men`() {
            assertEquals(1_500, HealthCalculator.dailyCalorieTarget(1371.0, BmiCategory.OBESE, Sex.MALE))
        }
    }

    @Nested
    inner class Metrics {
        private val today = LocalDate.of(2026, 9, 23)

        @Test
        fun `combines every formula for a profile`() {
            val profile = UserProfile("Minji", Sex.MALE, 1996, 175.0, 70.0, ActivityLevel.MODERATE)

            val metrics = HealthCalculator.metricsFor(profile, today)

            // Age 30; BMR 1648.75; TDEE 1648.75 × 1.55 = 2555.5625; BMI 22.857… → 22.9.
            assertEquals(
                HealthMetrics(
                    bmi = 22.9,
                    bmiCategory = BmiCategory.NORMAL,
                    basalMetabolicRateKcal = 1649,
                    totalDailyEnergyExpenditureKcal = 2556,
                    dailyTargetKcal = 2556,
                ),
                metrics,
            )
        }

        @Test
        fun `categorises the rounded BMI so number and label agree`() {
            // 66.35 / 1.7² = 22.958…, shown as 23.0, which is overweight.
            val profile = UserProfile("Minji", Sex.FEMALE, 2000, 170.0, 66.35, ActivityLevel.LIGHT)

            val metrics = HealthCalculator.metricsFor(profile, today)

            assertEquals(23.0, metrics.bmi)
            assertEquals(BmiCategory.OVERWEIGHT, metrics.bmiCategory)
        }
    }
}
