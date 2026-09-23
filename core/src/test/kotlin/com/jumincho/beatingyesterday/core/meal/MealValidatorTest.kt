package com.jumincho.beatingyesterday.core.meal

import com.jumincho.beatingyesterday.core.validation.FieldError
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MealValidatorTest {

    @Test
    fun `accepts a name and calories, trimming the name`() {
        assertEquals(MealValidationResult.Valid("Bibimbap", 560), MealValidator.validate("  Bibimbap ", "560"))
    }

    @Test
    fun `rounds decimal calories to whole kcal`() {
        assertEquals(MealValidationResult.Valid("Apple", 53), MealValidator.validate("Apple", "52.6"))
    }

    @Test
    fun `allows zero calories`() {
        assertEquals(MealValidationResult.Valid("Water", 0), MealValidator.validate("Water", "0"))
    }

    @Test
    fun `requires both fields`() {
        assertEquals(
            MealValidationResult.Invalid(FieldError.Required, FieldError.Required),
            MealValidator.validate(" ", ""),
        )
    }

    @Test
    fun `limits the name length`() {
        val result = MealValidator.validate("x".repeat(MealValidator.NAME_MAX_LENGTH + 1), "100")

        assertEquals(MealValidationResult.Invalid(FieldError.TooLong(MealValidator.NAME_MAX_LENGTH), null), result)
    }

    @Test
    fun `rejects calories that are not a number or out of range`() {
        val outOfRange = FieldError.OutOfRange(0, MealValidator.MAX_KCAL)
        assertEquals(MealValidationResult.Invalid(null, FieldError.NotANumber), MealValidator.validate("Rice", "lots"))
        assertEquals(MealValidationResult.Invalid(null, outOfRange), MealValidator.validate("Rice", "-1"))
        assertEquals(MealValidationResult.Invalid(null, outOfRange), MealValidator.validate("Rice", "5001"))
    }
}
