package com.fernandoyz.lottie.domain

import com.fernandoyz.lottie.domain.model.MenstruationRecord
import java.time.LocalDate

/** What tapping a calendar day does (STAGE_4 §2, approved 2026-11-20). */
sealed interface DayAction {
    /** Creates a menstruation auto-filled from [start] to [end]. */
    data class Add(val start: LocalDate, val end: LocalDate) : DayAction

    /** Shortens or extends [record] so it ends on [newEnd]. */
    data class ChangeEnd(val record: MenstruationRecord, val newEnd: LocalDate) : DayAction

    /** Deletes [record]; the UI asks for confirmation first. */
    data class Delete(val record: MenstruationRecord) : DayAction

    /** A new menstruation cannot start in the future. */
    data object StartInFuture : DayAction
}

object CalendarEditing {

    /**
     * Rules, in order:
     * 1. Tapping the start day of a record deletes it.
     * 2. Tapping another marked day ends the record the day before, so it stays one continuous block.
     * 3. Tapping the day right after a record's end extends it by that day.
     * 4. Tapping an empty future day is rejected.
     * 5. Tapping an empty day starts a record filled with [periodLengthDays], stopping before the next record.
     */
    fun actionFor(
        date: LocalDate,
        records: List<MenstruationRecord>,
        today: LocalDate,
        periodLengthDays: Int,
    ): DayAction {
        records.firstOrNull { !date.isBefore(it.startDate) && !date.isAfter(it.shownEnd(today)) }?.let { record ->
            return if (date == record.startDate) DayAction.Delete(record) else DayAction.ChangeEnd(record, date.minusDays(1))
        }
        records.firstOrNull { it.shownEnd(today).plusDays(1) == date }?.let { record ->
            return DayAction.ChangeEnd(record, date)
        }
        if (date.isAfter(today)) return DayAction.StartInFuture

        val filledEnd = date.plusDays(periodLengthDays - 1L)
        val nextStart = records.map { it.startDate }.filter { it.isAfter(date) }.minOrNull()
        val end = if (nextStart != null && !filledEnd.isBefore(nextStart)) nextStart.minusDays(1) else filledEnd
        return DayAction.Add(date, end)
    }
}

/** Last day shown as menstruation: its end, or today while it is open (STAGE_3 §3). */
fun MenstruationRecord.shownEnd(today: LocalDate): LocalDate = endDate ?: maxOf(startDate, today)
