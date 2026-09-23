package com.jumincho.beatingyesterday.core.profile

import com.jumincho.beatingyesterday.core.model.ActivityLevel
import com.jumincho.beatingyesterday.core.model.Sex
import com.jumincho.beatingyesterday.core.model.UserProfile
import com.jumincho.beatingyesterday.core.validation.FieldError
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate

class ProfileValidatorTest {

    private val today = LocalDate.of(2026, 9, 23)

    private val validDraft = ProfileDraft(
        name = "  Minji  ",
        sex = Sex.FEMALE,
        birthYear = "1998",
        heightCm = "162.5",
        weightKg = "54,2",
        activityLevel = ActivityLevel.LIGHT,
    )

    private fun errorsOf(draft: ProfileDraft): Map<ProfileField, FieldError> =
        assertInstanceOf<ProfileValidationResult.Invalid>(ProfileValidator.validate(draft, today)).errors

    @Test
    fun `accepts a complete draft, trimming the name and allowing a decimal comma`() {
        val result = ProfileValidator.validate(validDraft, today)

        assertEquals(
            ProfileValidationResult.Valid(UserProfile("Minji", Sex.FEMALE, 1998, 162.5, 54.2, ActivityLevel.LIGHT)),
            result,
        )
    }

    @Test
    fun `reports every missing field`() {
        val errors = errorsOf(ProfileDraft(name = "   "))

        assertEquals(ProfileField.entries.associateWith { FieldError.Required }, errors)
    }

    @Test
    fun `rejects a name that is too long`() {
        val errors = errorsOf(validDraft.copy(name = "x".repeat(ProfileValidator.NAME_MAX_LENGTH + 1)))

        assertEquals(mapOf(ProfileField.NAME to FieldError.TooLong(ProfileValidator.NAME_MAX_LENGTH)), errors)
    }

    @ParameterizedTest
    @ValueSource(strings = ["1925", "2013", "1998.5", "nineteen"])
    fun `limits the birth year to ages 14 to 100`(birthYear: String) {
        val errors = errorsOf(validDraft.copy(birthYear = birthYear))

        val expected = if (birthYear.toIntOrNull() == null) FieldError.NotANumber else FieldError.OutOfRange(1926, 2012)
        assertEquals(mapOf(ProfileField.BIRTH_YEAR to expected), errors)
    }

    @ParameterizedTest
    @ValueSource(strings = ["1926", "2012"])
    fun `accepts the birth year bounds`(birthYear: String) {
        assertInstanceOf<ProfileValidationResult.Valid>(
            ProfileValidator.validate(validDraft.copy(birthYear = birthYear), today),
        )
    }

    @Test
    fun `checks height and weight ranges inclusively`() {
        assertInstanceOf<ProfileValidationResult.Valid>(
            ProfileValidator.validate(validDraft.copy(heightCm = "100", weightKg = "300"), today),
        )
        assertEquals(
            mapOf(
                ProfileField.HEIGHT to
                    FieldError.OutOfRange(ProfileValidator.MIN_HEIGHT_CM, ProfileValidator.MAX_HEIGHT_CM),
                ProfileField.WEIGHT to
                    FieldError.OutOfRange(ProfileValidator.MIN_WEIGHT_KG, ProfileValidator.MAX_WEIGHT_KG),
            ),
            errorsOf(validDraft.copy(heightCm = "99.9", weightKg = "300.1")),
        )
    }

    @Test
    fun `rejects text that is not a number`() {
        assertEquals(mapOf(ProfileField.HEIGHT to FieldError.NotANumber), errorsOf(validDraft.copy(heightCm = "tall")))
    }

    @Test
    fun `a draft made from a profile shows whole numbers without decimals and validates back to it`() {
        val profile = UserProfile("Jumin", Sex.MALE, 1999, 175.0, 70.5, ActivityLevel.ACTIVE)

        val draft = ProfileDraft.from(profile)

        assertEquals("175", draft.heightCm)
        assertEquals("70.5", draft.weightKg)
        assertEquals(ProfileValidationResult.Valid(profile), ProfileValidator.validate(draft, today))
    }
}
