package com.jumincho.beatingyesterday.core.profile

import com.jumincho.beatingyesterday.core.model.ActivityLevel
import com.jumincho.beatingyesterday.core.model.Sex
import com.jumincho.beatingyesterday.core.model.UserProfile
import com.jumincho.beatingyesterday.core.validation.FieldError
import com.jumincho.beatingyesterday.core.validation.numberError
import com.jumincho.beatingyesterday.core.validation.textError
import com.jumincho.beatingyesterday.core.validation.toDecimalOrNull
import java.time.LocalDate

/** The fields of the profile form. */
enum class ProfileField { NAME, SEX, BIRTH_YEAR, HEIGHT, WEIGHT, ACTIVITY_LEVEL }

/** Raw, possibly invalid profile form input exactly as the user typed it. */
data class ProfileDraft(
    val name: String = "",
    val sex: Sex? = null,
    val birthYear: String = "",
    val heightCm: String = "",
    val weightKg: String = "",
    val activityLevel: ActivityLevel? = null,
) {
    companion object {
        /** A draft pre-filled with a saved [profile], for editing it. */
        fun from(profile: UserProfile): ProfileDraft = ProfileDraft(
            name = profile.name,
            sex = profile.sex,
            birthYear = profile.birthYear.toString(),
            heightCm = profile.heightCm.toPlainString(),
            weightKg = profile.weightKg.toPlainString(),
            activityLevel = profile.activityLevel,
        )

        private fun Double.toPlainString(): String = if (this % 1.0 == 0.0) toLong().toString() else toString()
    }
}

/** Outcome of [ProfileValidator.validate]. */
sealed interface ProfileValidationResult {
    /** Every field is valid; [profile] is ready to be saved. */
    data class Valid(val profile: UserProfile) : ProfileValidationResult

    /** At least one field is invalid; [errors] holds one error per invalid field. */
    data class Invalid(val errors: Map<ProfileField, FieldError>) : ProfileValidationResult
}

/**
 * Validates [ProfileDraft]s. The ranges keep the health formulas within sensible bounds: the
 * Mifflin–St Jeor equation was derived from adults, so ages outside 14–100 are rejected.
 */
object ProfileValidator {
    const val NAME_MAX_LENGTH = 30
    const val MIN_AGE = 14
    const val MAX_AGE = 100
    const val MIN_HEIGHT_CM = 100
    const val MAX_HEIGHT_CM = 250
    const val MIN_WEIGHT_KG = 30
    const val MAX_WEIGHT_KG = 300

    /** Validates [draft], using [today] to work out the allowed birth years. */
    fun validate(draft: ProfileDraft, today: LocalDate): ProfileValidationResult {
        val name = draft.name.trim()
        val sex = draft.sex
        val birthYear = draft.birthYear.trim().toIntOrNull()
        val heightCm = draft.heightCm.toDecimalOrNull()
        val weightKg = draft.weightKg.toDecimalOrNull()
        val activityLevel = draft.activityLevel

        val errors = buildMap {
            textError(name, NAME_MAX_LENGTH)?.let { put(ProfileField.NAME, it) }
            if (sex == null) put(ProfileField.SEX, FieldError.Required)
            numberError(draft.birthYear, birthYear?.toDouble(), today.year - MAX_AGE, today.year - MIN_AGE)
                ?.let { put(ProfileField.BIRTH_YEAR, it) }
            numberError(draft.heightCm, heightCm, MIN_HEIGHT_CM, MAX_HEIGHT_CM)?.let { put(ProfileField.HEIGHT, it) }
            numberError(draft.weightKg, weightKg, MIN_WEIGHT_KG, MAX_WEIGHT_KG)?.let { put(ProfileField.WEIGHT, it) }
            if (activityLevel == null) put(ProfileField.ACTIVITY_LEVEL, FieldError.Required)
        }

        return if (errors.isEmpty() && sex != null && birthYear != null && heightCm != null &&
            weightKg != null && activityLevel != null
        ) {
            ProfileValidationResult.Valid(UserProfile(name, sex, birthYear, heightCm, weightKg, activityLevel))
        } else {
            ProfileValidationResult.Invalid(errors)
        }
    }
}
