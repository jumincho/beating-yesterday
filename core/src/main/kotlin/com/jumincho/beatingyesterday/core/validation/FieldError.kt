package com.jumincho.beatingyesterday.core.validation

/** Why a single form field was rejected. The UI turns these into localized messages. */
sealed interface FieldError {
    /** Nothing was entered or selected. */
    data object Required : FieldError

    /** The text is not a number. */
    data object NotANumber : FieldError

    /** The text is longer than [maxLength] characters. */
    data class TooLong(val maxLength: Int) : FieldError

    /** The number is outside `[min, max]` (both inclusive). */
    data class OutOfRange(val min: Int, val max: Int) : FieldError
}

/** Parses user-typed decimals, accepting either `.` or `,` as the separator. */
internal fun String.toDecimalOrNull(): Double? = trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }

/** Error for a mandatory, already trimmed [text] of at most [maxLength] characters. */
internal fun textError(text: String, maxLength: Int): FieldError? = when {
    text.isEmpty() -> FieldError.Required
    text.length > maxLength -> FieldError.TooLong(maxLength)
    else -> null
}

/** Error for a mandatory number typed as [text] and parsed to [value] (`null` if unparseable). */
internal fun numberError(text: String, value: Double?, min: Int, max: Int): FieldError? = when {
    text.isBlank() -> FieldError.Required
    value == null -> FieldError.NotANumber
    value < min || value > max -> FieldError.OutOfRange(min, max)
    else -> null
}
