package com.jumincho.beatingyesterday.ui.format

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.jumincho.beatingyesterday.R
import com.jumincho.beatingyesterday.core.contest.Round
import com.jumincho.beatingyesterday.core.contest.RoundOutcome
import com.jumincho.beatingyesterday.core.contest.Verdict
import com.jumincho.beatingyesterday.core.health.BmiCategory
import com.jumincho.beatingyesterday.core.model.ActivityLevel
import com.jumincho.beatingyesterday.core.model.MealType
import com.jumincho.beatingyesterday.core.model.Sex

/** Display name of the meal type. */
@get:StringRes
val MealType.label: Int
    get() = when (this) {
        MealType.BREAKFAST -> R.string.meal_breakfast
        MealType.LUNCH -> R.string.meal_lunch
        MealType.DINNER -> R.string.meal_dinner
        MealType.SNACK -> R.string.meal_snack
    }

/** Display name of the sex. */
@get:StringRes
val Sex.label: Int
    get() = when (this) {
        Sex.MALE -> R.string.sex_male
        Sex.FEMALE -> R.string.sex_female
    }

/** Display name of the activity level. */
@get:StringRes
val ActivityLevel.label: Int
    get() = when (this) {
        ActivityLevel.SEDENTARY -> R.string.activity_sedentary
        ActivityLevel.LIGHT -> R.string.activity_light
        ActivityLevel.MODERATE -> R.string.activity_moderate
        ActivityLevel.ACTIVE -> R.string.activity_active
        ActivityLevel.VERY_ACTIVE -> R.string.activity_very_active
    }

/** What the activity level means in practice. */
@get:StringRes
val ActivityLevel.description: Int
    get() = when (this) {
        ActivityLevel.SEDENTARY -> R.string.activity_sedentary_description
        ActivityLevel.LIGHT -> R.string.activity_light_description
        ActivityLevel.MODERATE -> R.string.activity_moderate_description
        ActivityLevel.ACTIVE -> R.string.activity_active_description
        ActivityLevel.VERY_ACTIVE -> R.string.activity_very_active_description
    }

/** Display name of the BMI category. */
@get:StringRes
val BmiCategory.label: Int
    get() = when (this) {
        BmiCategory.UNDERWEIGHT -> R.string.bmi_underweight
        BmiCategory.NORMAL -> R.string.bmi_normal
        BmiCategory.OVERWEIGHT -> R.string.bmi_overweight
        BmiCategory.OBESE -> R.string.bmi_obese
    }

/** Display name of the round. */
@get:StringRes
val Round.label: Int
    get() = when (this) {
        Round.DIET -> R.string.round_diet
        Round.FOCUS -> R.string.round_focus
        Round.TASKS -> R.string.round_tasks
    }

/** Icon of the round. */
@get:DrawableRes
val Round.icon: Int
    get() = when (this) {
        Round.DIET -> R.drawable.ic_restaurant
        Round.FOCUS -> R.drawable.ic_timer
        Round.TASKS -> R.drawable.ic_task_alt
    }

/** Display name of the round outcome. */
@get:StringRes
val RoundOutcome.label: Int
    get() = when (this) {
        RoundOutcome.WON -> R.string.outcome_won
        RoundOutcome.LOST -> R.string.outcome_lost
        RoundOutcome.TIED -> R.string.outcome_tied
        RoundOutcome.NO_DATA -> R.string.outcome_no_data
    }

/** Display name of the verdict. */
@get:StringRes
val Verdict.label: Int
    get() = when (this) {
        Verdict.WIN -> R.string.verdict_win
        Verdict.LOSE -> R.string.verdict_lose
        Verdict.DRAW -> R.string.verdict_draw
    }

/** One-letter form of the verdict for the history strip. */
@get:StringRes
val Verdict.shortLabel: Int
    get() = when (this) {
        Verdict.WIN -> R.string.verdict_win_short
        Verdict.LOSE -> R.string.verdict_lose_short
        Verdict.DRAW -> R.string.verdict_draw_short
    }
