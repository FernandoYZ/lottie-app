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
        assertNull(PeriodEstimation.estimate(emptyList(), manualCycleLengthDays = null, today = today))
    }

    @Test
    fun `a single record uses the 28 and 6 day defaults and is approximate`() {
        // Its end is after today (auto-filled), so it does not count as a finished duration yet.
        val records = listOf(MenstruationRecord(1, day(6, 1), day(6, 20)))

        val estimate = PeriodEstimation.estimate(records, manualCycleLengthDays = null, today = today)!!

        assertEquals(day(6, 29), estimate.start)
        assertEquals(day(7, 4), estimate.end)
        assertTrue(estimate.isApproximate)
    }

    @Test
    fun `with two or more starts the cycle adapts to the recorded intervals`() {
        val records = listOf(
            MenstruationRecord(1, day(4, 20), day(4, 23)), // 4 days
            MenstruationRecord(2, day(5, 20), day(5, 23)), // 30 days later, 4 days
        )

        val estimate = PeriodEstimation.estimate(records, manualCycleLengthDays = null, today = today)!!

        assertEquals(day(6, 19), estimate.start)
        assertEquals(day(6, 22), estimate.end)
        assertFalse(estimate.isApproximate)
    }

    @Test
    fun `a manual cycle length wins over the computed one`() {
        val records = listOf(
            MenstruationRecord(1, day(4, 20), day(4, 23)),
            MenstruationRecord(2, day(5, 20), day(5, 23)),
        )

        val estimate = PeriodEstimation.estimate(records, manualCycleLengthDays = 26, today = today)!!

        assertEquals(day(6, 15), estimate.start)
        assertFalse(estimate.isApproximate)
    }

    @Test
    fun `estimate on or before today's start is not past, after it is past`() {
        val records = listOf(MenstruationRecord(1, day(5, 18), day(5, 21)))

        assertFalse(PeriodEstimation.estimate(records, null, today = day(6, 15))!!.isPast) // start 15/06
        assertTrue(PeriodEstimation.estimate(records, null, today = day(6, 16))!!.isPast)
    }
}
