package com.fernandoyz.lottie.domain

import com.fernandoyz.lottie.domain.model.MenstruationRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** Synthetic dates only; never real user data. */
class CalendarMarksTest {

    private fun day(month: Int, dayOfMonth: Int) = LocalDate.of(2026, month, dayOfMonth)
    private val june = YearMonth.of(2026, 6)
    private val today = day(6, 15)

    @Test
    fun `closed record marks every day from start to end`() {
        val marks = CalendarMarks.marksFor(june, listOf(MenstruationRecord(1, day(6, 1), day(6, 4))), null, today)

        assertEquals((1..4).associate { day(6, it) to DayMark.Registered }, marks)
    }

    @Test
    fun `open record is marked from start to today`() {
        val marks = CalendarMarks.marksFor(june, listOf(MenstruationRecord(1, day(6, 13), null)), null, today)

        assertEquals((13..15).associate { day(6, it) to DayMark.Registered }, marks)
    }

    @Test
    fun `estimate marks days from estimated start to end`() {
        val estimate = PeriodEstimate(start = day(6, 20), end = day(6, 23), isPast = false, isApproximate = false)

        val marks = CalendarMarks.marksFor(june, emptyList(), estimate, today)

        assertEquals((20..23).associate { day(6, it) to DayMark.Estimated }, marks)
    }

    @Test
    fun `registered days after today are upcoming`() {
        val marks = CalendarMarks.marksFor(june, listOf(MenstruationRecord(1, day(6, 14), day(6, 17))), null, today)

        assertEquals(
            mapOf(
                day(6, 14) to DayMark.Registered,
                day(6, 15) to DayMark.Registered,
                day(6, 16) to DayMark.Upcoming,
                day(6, 17) to DayMark.Upcoming,
            ),
            marks,
        )
    }

    @Test
    fun `registered days win over estimated days`() {
        val estimate = PeriodEstimate(start = day(6, 3), end = day(6, 6), isPast = true, isApproximate = false)

        val marks = CalendarMarks.marksFor(june, listOf(MenstruationRecord(1, day(6, 1), day(6, 4))), estimate, today)

        assertEquals(DayMark.Registered, marks[day(6, 4)])
        assertEquals(DayMark.Estimated, marks[day(6, 5)])
    }

    @Test
    fun `only days inside the month are returned`() {
        val records = listOf(MenstruationRecord(1, day(5, 30), day(6, 2)))

        val marks = CalendarMarks.marksFor(june, records, null, today)

        assertEquals(setOf(day(6, 1), day(6, 2)), marks.keys)
        assertTrue(CalendarMarks.marksFor(YearMonth.of(2026, 5), records, null, today).keys.containsAll(setOf(day(5, 30), day(5, 31))))
    }

    @Test
    fun `later estimated menstruations are marked too`() {
        val records = listOf(MenstruationRecord(1, day(6, 1), day(6, 4)))
        val estimate = PeriodEstimation.estimate(records, manualCycleLengthDays = null, today = today) // 29/06, then every 28 days

        val marks = CalendarMarks.marksFor(YearMonth.of(2026, 8), records, estimate, today)

        assertEquals((24..27).associate { day(8, it) to DayMark.Estimated }, marks)
    }
}
