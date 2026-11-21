package com.fernandoyz.lottie.domain

import com.fernandoyz.lottie.domain.model.MenstruationRecord
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * One cycle of the History list (STATISTICS §3): from a menstruation start to the day before the next one.
 * For the current cycle [end] and [lengthDays] come from the estimate and are null during a delay.
 */
data class CycleSummary(
    val start: LocalDate,
    val end: LocalDate?,
    val lengthDays: Int?,
    /** Day of the cycle today; only for the current cycle. */
    val dayOfCycle: Int?,
    /** Null while the menstruation is in progress (no end, or an end after today). */
    val menstruationDays: Int?,
    val isCurrent: Boolean,
)

object CycleHistory {

    /** Cycles newest first; empty without records. */
    fun cycles(records: List<MenstruationRecord>, estimate: PeriodEstimate?, today: LocalDate): List<CycleSummary> {
        val sorted = records.sortedBy { it.startDate }
        return sorted.mapIndexed { index, record ->
            val menstruationDays = record.durationDays.takeIf { record.endDate != null && !record.endDate.isAfter(today) }
            val next = sorted.getOrNull(index + 1)
            if (next != null) {
                CycleSummary(record.startDate, next.startDate.minusDays(1), days(record.startDate, next.startDate), null, menstruationDays, isCurrent = false)
            } else {
                val nextStart = estimate?.start?.takeIf { it.isAfter(record.startDate) && !today.isAfter(it) }
                CycleSummary(
                    start = record.startDate,
                    end = nextStart?.minusDays(1),
                    lengthDays = nextStart?.let { days(record.startDate, it) },
                    dayOfCycle = days(record.startDate, today) + 1,
                    menstruationDays = menstruationDays,
                    isCurrent = true,
                )
            }
        }.reversed()
    }

    private fun days(from: LocalDate, to: LocalDate): Int = ChronoUnit.DAYS.between(from, to).toInt()
}
