package com.fernandoyz.lottie.domain

import com.fernandoyz.lottie.domain.model.MenstruationRecord
import java.time.LocalDate
import java.time.YearMonth

enum class DayMark {
    Registered,

    /** Registered days after today: auto-filled and still expected (STAGE_4 rule 2). */
    Upcoming,
    Estimated,
}

/** Which days of a month the calendar marks (STAGE_3 §3-4). */
object CalendarMarks {

    fun marksFor(
        month: YearMonth,
        records: List<MenstruationRecord>,
        estimate: PeriodEstimate?,
        today: LocalDate,
    ): Map<LocalDate, DayMark> {
        val marks = mutableMapOf<LocalDate, DayMark>()

        estimate?.let { addRange(marks, month, it.start, it.end, DayMark.Estimated) }
        // Registered days are added last so they win over estimated ones.
        records.forEach { record ->
            val end = record.shownEnd(today)
            addRange(marks, month, record.startDate, minOf(end, today), DayMark.Registered)
            addRange(marks, month, maxOf(record.startDate, today.plusDays(1)), end, DayMark.Upcoming)
        }
        return marks
    }

    private fun addRange(
        marks: MutableMap<LocalDate, DayMark>,
        month: YearMonth,
        from: LocalDate,
        to: LocalDate,
        mark: DayMark,
    ) {
        val first = maxOf(from, month.atDay(1))
        val last = minOf(to, month.atEndOfMonth())
        var date = first
        while (!date.isAfter(last)) {
            marks[date] = mark
            date = date.plusDays(1)
        }
    }
}
