package com.fernandoyz.lottie.domain

import com.fernandoyz.lottie.domain.model.MenstruationRecord
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

enum class CyclePhase { Menstrual, Follicular, Ovulation, Luteal }

data class CycleDayInfo(
    /** 1 on the first day of the menstruation that starts the cycle. */
    val cycleDay: Int,
    /** Null outside menstruation when the phase cannot be calculated (see [CyclePhases]). */
    val phase: CyclePhase?,
    /** The next start comes from the estimate, not from a registered menstruation. */
    val basedOnEstimate: Boolean,
    /** Within the estimated fertile window: the 5 days before ovulation and the ovulation day. */
    val inFertileWindow: Boolean = false,
)

/**
 * Calendar-method approximation of the cycle phases (day-detail S2-S4, approved 2026-11-20).
 * Ovulation = next menstruation start - 14 days. Past cycles use the real next start; the current and
 * later cycles use the estimate, repeating its length. When the ovulation would fall inside the
 * menstruation the cycle is too short for this method and no other phase is given. Never a diagnosis.
 */
object CyclePhases {

    private const val LUTEAL_DAYS = 14L
    private const val FERTILE_DAYS_BEFORE_OVULATION = 5L

    private data class Cycle(
        val start: LocalDate,
        val menstruationEnd: LocalDate,
        val nextStart: LocalDate?,
        val estimated: Boolean,
    ) {
        val ovulation: LocalDate? = nextStart?.minusDays(LUTEAL_DAYS)?.takeIf { it.isAfter(menstruationEnd) }

        fun isFertile(date: LocalDate): Boolean =
            ovulation != null && !date.isAfter(ovulation) && !date.isBefore(ovulation.minusDays(FERTILE_DAYS_BEFORE_OVULATION))

        val fertileDays: List<LocalDate> =
            ovulation?.let { ov -> (FERTILE_DAYS_BEFORE_OVULATION downTo 0).map { ov.minusDays(it) } }.orEmpty()
    }

    fun dayInfo(
        date: LocalDate,
        records: List<MenstruationRecord>,
        estimate: PeriodEstimate?,
        today: LocalDate,
    ): CycleDayInfo? {
        val cycle = cycleAt(date, records, estimate, today) ?: return null
        val ovulation = cycle.ovulation
        val phase = when {
            !date.isAfter(cycle.menstruationEnd) -> CyclePhase.Menstrual
            ovulation == null -> null
            date.isBefore(ovulation) -> CyclePhase.Follicular
            date == ovulation -> CyclePhase.Ovulation
            else -> CyclePhase.Luteal
        }
        return CycleDayInfo(
            cycleDay = ChronoUnit.DAYS.between(cycle.start, date).toInt() + 1,
            phase = phase,
            basedOnEstimate = cycle.estimated,
            inFertileWindow = cycle.isFertile(date),
        )
    }

    /** Estimated ovulation days shown on the calendar: real cycles plus the next estimated one. */
    fun ovulationDays(month: YearMonth, records: List<MenstruationRecord>, estimate: PeriodEstimate?, today: LocalDate): Set<LocalDate> =
        calendarCycles(records, estimate, today).mapNotNull { it.ovulation }.filter { YearMonth.from(it) == month }.toSet()

    /** Estimated fertile days shown on the calendar, for the same cycles as [ovulationDays]. */
    fun fertileDays(month: YearMonth, records: List<MenstruationRecord>, estimate: PeriodEstimate?, today: LocalDate): Set<LocalDate> =
        calendarCycles(records, estimate, today).flatMap { it.fertileDays }.filter { YearMonth.from(it) == month }.toSet()

    private fun calendarCycles(records: List<MenstruationRecord>, estimate: PeriodEstimate?, today: LocalDate): List<Cycle> {
        val sorted = records.sortedBy { it.startDate }
        val cycles = sorted.zipWithNext { current, next ->
            Cycle(current.startDate, current.shownEnd(today), next.startDate, estimated = false)
        } + listOfNotNull(
            sorted.lastOrNull()?.let { latest ->
                Cycle(latest.startDate, latest.shownEnd(today), estimate?.start, estimated = true)
            },
        )
        return cycles
    }

    private fun cycleAt(
        date: LocalDate,
        records: List<MenstruationRecord>,
        estimate: PeriodEstimate?,
        today: LocalDate,
    ): Cycle? {
        val sorted = records.sortedBy { it.startDate }
        val index = sorted.indexOfLast { !it.startDate.isAfter(date) }
        if (index < 0) return null
        val record = sorted[index]
        sorted.getOrNull(index + 1)?.let { next ->
            return Cycle(record.startDate, record.shownEnd(today), next.startDate, estimated = false)
        }

        // Latest record: the next start comes from the estimate.
        if (estimate == null || date.isBefore(estimate.start)) {
            return Cycle(record.startDate, record.shownEnd(today), estimate?.start, estimated = true)
        }
        // Beyond the estimated start: repeat the estimated cycle.
        val length = ChronoUnit.DAYS.between(record.startDate, estimate.start)
        if (length <= 0) return null
        val elapsedCycles = ChronoUnit.DAYS.between(record.startDate, date) / length
        val start = record.startDate.plusDays(elapsedCycles * length)
        val bleedingDays = ChronoUnit.DAYS.between(estimate.start, estimate.end)
        return Cycle(start, start.plusDays(bleedingDays), start.plusDays(length), estimated = true)
    }
}
