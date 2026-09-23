package com.jumincho.beatingyesterday.core.meal

import com.jumincho.beatingyesterday.core.validation.FieldError
import com.jumincho.beatingyesterday.core.validation.numberError
import com.jumincho.beatingyesterday.core.validation.textError
import com.jumincho.beatingyesterday.core.validation.toDecimalOrNull
import kotlin.math.roundToInt

/** Outcome of [MealValidator.validate]. */
sealed interface MealValidationResult {
    /** The input is valid: a trimmed [name] and whole-number [kcal]. */
    data class Valid(val name: String, val kcal: Int) : MealValidationResult

    /** At least one of the fields is invalid. */
    data class Invalid(val nameError: FieldError?, val kcalError: FieldError?) : MealValidationResult
}

/** Validates the meal editor form. Decimal calories are accepted and rounded to whole kcal. */
object MealValidator {
    const val NAME_MAX_LENGTH = 60
    const val MAX_KCAL = 5_000

    /** Validates a meal [name] and its calories typed as [kcal]. */
    fun validate(name: String, kcal: String): MealValidationResult {
        val trimmedName = name.trim()
        val kcalValue = kcal.toDecimalOrNull()
        val nameError = textError(trimmedName, NAME_MAX_LENGTH)
        val kcalError = numberError(kcal, kcalValue, 0, MAX_KCAL)
        return if (nameError == null && kcalError == null && kcalValue != null) {
            MealValidationResult.Valid(trimmedName, kcalValue.roundToInt())
        } else {
            MealValidationResult.Invalid(nameError, kcalError)
        }
    }
}
