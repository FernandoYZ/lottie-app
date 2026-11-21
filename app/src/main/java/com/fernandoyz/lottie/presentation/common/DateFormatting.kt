package com.fernandoyz.lottie.presentation.common

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val spanish: Locale = Locale.forLanguageTag("es")
private val longDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", spanish)
private val shortDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", spanish)

/** "2 de noviembre de 2026". */
fun formatLongDate(date: LocalDate): String = date.format(longDateFormatter)

private val dayHeaderFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM", spanish)

/** "sáb 21 nov". */
fun formatDayHeader(date: LocalDate): String = date.format(dayHeaderFormatter)

/** "2 nov 2026". */
fun formatShortDate(date: LocalDate): String = date.format(shortDateFormatter)
