package com.jumincho.beatingyesterday.ui.format

import android.text.format.DateFormat
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.jumincho.beatingyesterday.R
import com.jumincho.beatingyesterday.core.validation.FieldError
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.time.Duration

/** The locale of the current configuration, so formatting follows language changes. */
@Composable
@ReadOnlyComposable
fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

/** Calories such as "1,850 kcal". */
@Composable
@ReadOnlyComposable
fun kcalText(kcal: Int): String =
    stringResource(R.string.kcal_amount, NumberFormat.getIntegerInstance(currentLocale()).format(kcal))

/** A BMI value with one decimal, such as "22.9". */
@Composable
@ReadOnlyComposable
fun bmiText(bmi: Double): String = NumberFormat.getNumberInstance(currentLocale()).apply {
    minimumFractionDigits = 1
    maximumFractionDigits = 1
}.format(bmi)

/** A duration in hours and whole minutes, such as "1 h 35 min". */
@Composable
@ReadOnlyComposable
fun durationText(duration: Duration): String {
    val totalMinutes = duration.inWholeMinutes
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours == 0L -> stringResource(R.string.duration_minutes, minutes)
        minutes == 0L -> stringResource(R.string.duration_hours, hours)
        else -> stringResource(R.string.duration_hours_minutes, hours, minutes)
    }
}

/**
 * A timer face such as "24:59" or "1:02:03".
 *
 * @param roundUp count partial seconds as whole ones, as a countdown should.
 */
fun clockText(duration: Duration, roundUp: Boolean = false): String {
    val millis = duration.inWholeMilliseconds.coerceAtLeast(0)
    val totalSeconds = if (roundUp) (millis + 999) / 1000 else millis / 1000
    val hours = totalSeconds / 3600
    val minutes = totalSeconds % 3600 / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
    }
}

/** A localized date built from a date-format [skeleton], e.g. "Wednesday, September 23". */
@Composable
fun dateText(date: LocalDate, skeleton: String = "EEEEMMMMd"): String {
    val locale = currentLocale()
    return remember(date, locale, skeleton) {
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale).format(date)
    }
}

/** A localized short time, e.g. "9:05 AM". */
@Composable
fun timeText(time: LocalTime): String {
    val locale = currentLocale()
    return remember(time, locale) {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale).format(time)
    }
}

/** The message explaining a form [FieldError]. */
@Composable
@ReadOnlyComposable
fun FieldError.message(): String = when (this) {
    FieldError.Required -> stringResource(R.string.error_required)
    FieldError.NotANumber -> stringResource(R.string.error_not_a_number)
    is FieldError.TooLong -> pluralStringResource(R.plurals.error_too_long, maxLength, maxLength)
    is FieldError.OutOfRange -> stringResource(R.string.error_out_of_range, min, max)
}

/** Supporting-text slot content for a text field showing [error], or `null` when there is none. */
fun errorSupportingText(error: FieldError?): (@Composable () -> Unit)? = if (error == null) {
    null
} else {
    { Text(error.message()) }
}
