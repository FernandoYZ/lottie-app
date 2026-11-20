package com.fernandoyz.lottie.domain

import com.fernandoyz.lottie.domain.model.MenstruationRecord
import java.time.LocalDate
import kotlin.math.roundToInt

/** Orientative estimate of the next menstruation. Never a certainty (PRODUCT §3, RNF-012). */
data class PeriodEstimate(
    val start: LocalDate,
    /** Null when no record has an end date yet. */
    val end: LocalDate?,
    /** Today is after the estimated start and no newer record exists. */
    val isPast: Boolean,
)

/**
 * Estimation rules (STAGE_3 §2, approved 2026-11-20).
 * A statistical formula based on recorded cycles is a planned future improvement.
 */
object PeriodEstimation {

    fun estimate(records: List<MenstruationRecord>, usualCycleLengthDays: Int, today: LocalDate): PeriodEstimate? {
        val latest = records.maxByOrNull { it.startDate } ?: return null
        val start = latest.startDate.plusDays(usualCycleLengthDays.toLong())

        val durations = records.mapNotNull { it.durationDays }
        // roundToInt rounds x.5 up for positive values.
        val end = if (durations.isEmpty()) null else start.plusDays(durations.average().roundToInt() - 1L)

        return PeriodEstimate(start = start, end = end, isPast = today.isAfter(start))
    }
}
