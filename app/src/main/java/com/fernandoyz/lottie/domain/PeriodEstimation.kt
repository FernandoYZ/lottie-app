package com.fernandoyz.lottie.domain

import com.fernandoyz.lottie.domain.model.MenstruationRecord
import java.time.LocalDate
import java.time.temporal.ChronoUnit

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

    /** Predictions are shown for cycles starting within this many months after today (PREDICTIONS §1). */
    private const val UPCOMING_MONTHS = 12L

    /**
     * The next [estimate] followed by its repetitions (PREDICTIONS §1, approved 2026-11-21): each start is the
     * previous one plus the estimated cycle length, each lasts like [estimate]; starts up to 12 months after today.
     */
    fun upcoming(records: List<MenstruationRecord>, estimate: PeriodEstimate?, today: LocalDate): List<PeriodEstimate> {
        if (estimate == null) return emptyList()
        val latest = records.maxByOrNull { it.startDate } ?: return listOf(estimate)
        val cycleDays = ChronoUnit.DAYS.between(latest.startDate, estimate.start)
        if (cycleDays <= 0) return listOf(estimate)
        val periodDays = ChronoUnit.DAYS.between(estimate.start, estimate.end)
        val limit = today.plusMonths(UPCOMING_MONTHS)
        return generateSequence(estimate.start) { it.plusDays(cycleDays) }
            .takeWhile { it == estimate.start || !it.isAfter(limit) }
            .map { start -> estimate.copy(start = start, end = start.plusDays(periodDays), isPast = today.isAfter(start)) }
            .toList()
    }
}
