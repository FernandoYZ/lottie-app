package com.fernandoyz.lottie.presentation.common

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.fernandoyz.lottie.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val spanish: Locale = Locale.forLanguageTag("es")
private val longDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", spanish)
private val shortDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", spanish)

/** "2 de noviembre de 2026". */
fun formatLongDate(date: LocalDate): String = date.format(longDateFormatter)

/** "2 nov 2026". */
fun formatShortDate(date: LocalDate): String = date.format(shortDateFormatter)

/**
 * Date picker limited to [minDate]..[maxDate].
 * Material3 works with UTC midnight milliseconds; both conversions use UTC to keep the calendar day.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarDatePickerDialog(
    initialDate: LocalDate?,
    maxDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    minDate: LocalDate? = null,
) {
    val maxMillis = remember(maxDate) { maxDate.toUtcMillis() }
    val minMillis = remember(minDate) { minDate?.toUtcMillis() }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialDate?.toUtcMillis(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) =
                utcTimeMillis <= maxMillis && (minMillis == null || utcTimeMillis >= minMillis)

            override fun isSelectableYear(year: Int) =
                year <= maxDate.year && (minDate == null || year >= minDate.year)
        },
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { state.selectedDateMillis?.let { onDateSelected(it.utcMillisToLocalDate()) } ?: onDismiss() },
            ) { Text(stringResource(R.string.action_accept)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    ) {
        DatePicker(state = state)
    }
}

private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.utcMillisToLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
