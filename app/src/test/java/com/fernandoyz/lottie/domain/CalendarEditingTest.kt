package com.fernandoyz.lottie.domain

import com.fernandoyz.lottie.domain.model.MenstruationRecord
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** Synthetic dates only; never real user data. */
class CalendarEditingTest {

    private fun day(month: Int, dayOfMonth: Int) = LocalDate.of(2026, month, dayOfMonth)
    private val today = day(6, 15)
    private val may = MenstruationRecord(1, day(5, 10), day(5, 14))

    private fun tap(date: LocalDate, records: List<MenstruationRecord> = listOf(may), periodLength: Int = 6) =
        CalendarEditing.actionFor(date, records, today, periodLength)

    @Test
    fun `tapping an empty day fills the period length from it`() =
        assertEquals(DayAction.Add(day(6, 1), day(6, 6)), tap(day(6, 1)))

    @Test
    fun `the fill uses the adaptive period length`() =
        assertEquals(DayAction.Add(day(6, 1), day(6, 4)), tap(day(6, 1), periodLength = 4))

    @Test
    fun `tapping today fills into the future`() =
        assertEquals(DayAction.Add(today, day(6, 20)), tap(today))

    @Test
    fun `the fill stops before an already registered menstruation`() =
        assertEquals(DayAction.Add(day(5, 6), day(5, 9)), tap(day(5, 6)))

    @Test
    fun `tapping an empty future day is rejected`() =
        assertEquals(DayAction.StartInFuture, tap(today.plusDays(1)))

    @Test
    fun `tapping the start day deletes the record`() =
        assertEquals(DayAction.Delete(may), tap(day(5, 10)))

    @Test
    fun `tapping a marked day ends the menstruation the day before`() =
        assertEquals(DayAction.ChangeEnd(may, day(5, 11)), tap(day(5, 12)))

    @Test
    fun `tapping the day right after the end extends it`() =
        assertEquals(DayAction.ChangeEnd(may, day(5, 15)), tap(day(5, 15)))

    @Test
    fun `an open record covers its start to today`() {
        val open = MenstruationRecord(2, day(6, 12), null)

        assertEquals(DayAction.ChangeEnd(open, day(6, 14)), tap(today, listOf(open)))
        assertEquals(DayAction.ChangeEnd(open, day(6, 16)), tap(day(6, 16), listOf(open)))
    }
}
