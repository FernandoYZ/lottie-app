package com.fernandoyz.lottie.domain

import com.fernandoyz.lottie.domain.model.MenstruationRecord
import java.time.LocalDate

/** Orientative estimate of the next menstruation. Never a certainty (PRODUCT §3, RNF-012). */
data class PeriodEstimate(
    val start: LocalDate,
    val end: LocalDate,
    /** Today is after the estimated start and no newer record exists. */
    val isPast: Boolean,
    /** The cycle length is the default because there are fewer than two registered starts. */
    val isApproximate: Boolean,
)

/**
 * Estimation rules (STAGE_4 §3, approved 2026-11-20).
 * Start = latest start + cycle length (manual value, else history mean, else 28).
 * End = start + period length (history mean, else 6) - 1.
 */
object PeriodEstimation {

    fun estimate(records: List<MenstruationRecord>, manualCycleLengthDays: Int?, today: LocalDate): PeriodEstimate? {
        val latest = records.maxByOrNull { it.startDate } ?: return null
        val learnedCycle = CycleStatistics.averageCycleLengthDays(records)
        val cycle = manualCycleLengthDays ?: learnedCycle ?: CycleStatistics.DEFAULT_CYCLE_LENGTH_DAYS
        val start = latest.startDate.plusDays(cycle.toLong())
        val end = start.plusDays(CycleStatistics.periodLengthDays(records, today) - 1L)

        return PeriodEstimate(
            start = start,
            end = end,
            isPast = today.isAfter(start),
            isApproximate = manualCycleLengthDays == null && learnedCycle == null,
        )
    }
}
