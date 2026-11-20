package com.fernandoyz.lottie.domain

import com.fernandoyz.lottie.domain.model.MenstruationRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Synthetic dates only; never real user data. */
class PeriodEstimationTest {

    private fun day(month: Int, dayOfMonth: Int) = LocalDate.of(2026, month, dayOfMonth)
    private val today = day(6, 15)

    @Test
    fun `no records means no estimate`() {
        assertNull(PeriodEstimation.estimate(emptyList(), usualCycleLengthDays = 28, today = today))
    }

    @Test
    fun `start is latest start plus declared cycle length`() {
        val records = listOf(
            MenstruationRecord(1, day(5, 1), day(5, 4)),
            MenstruationRecord(2, day(6, 1), day(6, 4)),
        )

        val estimate = PeriodEstimation.estimate(records, usualCycleLengthDays = 28, today = today)

        assertEquals(day(6, 29), estimate?.start)
    }

    @Test
    fun `end uses mean duration of records with end, counting both days`() {
        val records = listOf(
            MenstruationRecord(1, day(5, 1), day(5, 4)), // 4 days
            MenstruationRecord(2, day(6, 1), day(6, 4)), // 4 days
        )

        val estimate = PeriodEstimation.estimate(records, usualCycleLengthDays = 28, today = today)

        assertEquals(day(7, 2), estimate?.end) // 29/06 + 4 - 1
    }

    @Test
    fun `mean duration rounds half up`() {
        val records = listOf(
            MenstruationRecord(1, day(4, 1), day(4, 4)), // 4 days
            MenstruationRecord(2, day(5, 1), day(5, 5)), // 5 days -> mean 4.5 -> 5
            MenstruationRecord(3, day(6, 1), null),
        )

        val estimate = PeriodEstimation.estimate(records, usualCycleLengthDays = 30, today = today)

        assertEquals(day(7, 1), estimate?.start)
        assertEquals(day(7, 5), estimate?.end)
    }

    @Test
    fun `open latest record is the base and has no duration`() {
        val records = listOf(
            MenstruationRecord(1, day(5, 1), day(5, 3)), // 3 days
            MenstruationRecord(2, day(6, 12), null),
        )

        val estimate = PeriodEstimation.estimate(records, usualCycleLengthDays = 28, today = today)

        assertEquals(day(7, 10), estimate?.start)
        assertEquals(day(7, 12), estimate?.end)
    }

    @Test
    fun `no end estimate when no record has an end`() {
        val records = listOf(MenstruationRecord(1, day(6, 12), null))

        val estimate = PeriodEstimation.estimate(records, usualCycleLengthDays = 28, today = today)

        assertEquals(day(7, 10), estimate?.start)
        assertNull(estimate?.end)
    }

    @Test
    fun `estimate on or before today's start is not past, after it is past`() {
        val records = listOf(MenstruationRecord(1, day(5, 18), day(5, 21)))

        assertFalse(PeriodEstimation.estimate(records, 28, today = day(6, 15))!!.isPast) // start 15/06
        assertTrue(PeriodEstimation.estimate(records, 28, today = day(6, 16))!!.isPast)
    }
}
