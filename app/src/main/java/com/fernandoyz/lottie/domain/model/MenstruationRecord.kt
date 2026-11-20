package com.fernandoyz.lottie.domain.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * A menstruation declared by the user. [endDate] is null while it is ongoing or unknown.
 * Dates are local calendar days.
 */
data class MenstruationRecord(
    val id: Long,
    val startDate: LocalDate,
    val endDate: LocalDate?,
) {
    /** Days between start and end, counting both (STAGE_2 §4). Null while there is no end date. */
    val durationDays: Int? = endDate?.let { ChronoUnit.DAYS.between(startDate, it).toInt() + 1 }
}
