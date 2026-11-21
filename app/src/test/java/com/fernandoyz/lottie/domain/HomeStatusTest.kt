package com.fernandoyz.lottie.domain

import com.fernandoyz.lottie.domain.model.MenstruationRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** HOME §3-§5. Synthetic dates only; never real user data. */
class HomeStatusTest {

    // 01/06 - 04/06 with a 28 day cycle: next start 29/06, ovulation 15/06, fertile 10/06 - 15/06.
    private val record = MenstruationRecord(1, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 4))

    private fun status(today: LocalDate, records: List<MenstruationRecord> = listOf(record)): HomeStatus =
        HomeStatus.of(records, PeriodEstimation.estimate(records, manualCycleLengthDays = null, today = today), today)

    @Test
    fun `without records asks to register`() {
        val status = status(LocalDate.of(2026, 6, 10), records = emptyList())

        assertEquals(HomeHeadline.NoRecords, status.headline)
        assertNull(status.ring)
        assertEquals(HomeAction.Start, status.action)
    }

    @Test
    fun `during a menstruation counts its day and offers to finish it`() {
        val status = status(LocalDate.of(2026, 6, 3))

        assertEquals(HomeHeadline.MenstruationDay(3), status.headline)
        assertEquals(HomeAction.Finish(record), status.action)
    }

    @Test
    fun `before the ovulation counts down to it`() {
        val status = status(LocalDate.of(2026, 6, 11))

        assertEquals(HomeHeadline.OvulationIn(4), status.headline)
        assertEquals(CyclePhase.Follicular, status.phase)
        assertEquals(11, status.cycleDay)
        assertEquals(HomeAction.Start, status.action)
    }

    @Test
    fun `on the ovulation day says so`() {
        assertEquals(HomeHeadline.OvulationToday, status(LocalDate.of(2026, 6, 15)).headline)
    }

    @Test
    fun `after the ovulation counts down to the menstruation`() {
        val status = status(LocalDate.of(2026, 6, 20))

        assertEquals(HomeHeadline.PeriodIn(9), status.headline)
        assertEquals(CyclePhase.Luteal, status.phase)
    }

    @Test
    fun `on the estimated start says so without a phase`() {
        val status = status(LocalDate.of(2026, 6, 29))

        assertEquals(HomeHeadline.PeriodToday, status.headline)
        assertEquals(29, status.cycleDay)
        assertNull(status.phase)
    }

    @Test
    fun `after the estimated start without a new menstruation is a delay`() {
        val status = status(LocalDate.of(2026, 7, 2))

        assertEquals(HomeHeadline.Delay(3), status.headline)
        assertTrue(status.ring!!.delayed)
        assertEquals(32, status.cycleDay)
        assertNull(status.phase)
        assertNull(status.nextFertileWindow)
    }

    @Test
    fun `too short cycle without ovulation counts down to the menstruation`() {
        // 01/06 - 20/06 with a 28 day cycle: the ovulation would fall inside the menstruation.
        val long = MenstruationRecord(1, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 20))

        val status = status(LocalDate.of(2026, 6, 22), records = listOf(long))

        assertEquals(HomeHeadline.PeriodIn(7), status.headline)
        assertNull(status.nextFertileWindow)
        assertEquals(listOf(RingKind.Menstrual, RingKind.Other), status.ring!!.segments.map { it.kind })
    }

    @Test
    fun `ring splits the cycle by phase and marks today`() {
        val ring = status(LocalDate.of(2026, 6, 11)).ring!!

        assertEquals(28, ring.lengthDays)
        assertEquals(10, ring.todayIndex)
        assertEquals(
            listOf(
                RingSegment(RingKind.Menstrual, firstDay = 0, days = 4),
                RingSegment(RingKind.Follicular, firstDay = 4, days = 5),
                RingSegment(RingKind.Fertile, firstDay = 9, days = 6),
                RingSegment(RingKind.Luteal, firstDay = 15, days = 13),
            ),
            ring.segments,
        )
    }

    @Test
    fun `next fertile window is the current one until it ends`() {
        val window = status(LocalDate.of(2026, 6, 15)).nextFertileWindow

        assertEquals(LocalDate.of(2026, 6, 10) to LocalDate.of(2026, 6, 15), window)
    }

    @Test
    fun `next fertile window moves to the next cycle after the current one ends`() {
        // Next cycle starts 29/06: ovulation 13/07, fertile 08/07 - 13/07.
        val window = status(LocalDate.of(2026, 6, 20)).nextFertileWindow

        assertEquals(LocalDate.of(2026, 7, 8) to LocalDate.of(2026, 7, 13), window)
    }

    @Test
    fun `a menstruation whose last marked day is today offers to edit it`() {
        val status = status(LocalDate.of(2026, 6, 4))

        assertEquals(HomeAction.Edit, status.action)
    }

    @Test
    fun `a menstruation without end date still offers to finish it`() {
        val open = MenstruationRecord(1, LocalDate.of(2026, 6, 1), null)

        assertEquals(HomeAction.Finish(open), status(LocalDate.of(2026, 6, 4), records = listOf(open)).action)
    }

    @Test
    fun `knows the cycle length and the days to the next menstruation`() {
        val status = status(LocalDate.of(2026, 6, 11))

        assertEquals(28, status.cycleLengthDays)
        assertEquals(18, status.daysToNextPeriod)
        assertEquals(0, status(LocalDate.of(2026, 6, 29)).daysToNextPeriod)
    }

    @Test
    fun `during a delay there is no cycle length nor countdown`() {
        val status = status(LocalDate.of(2026, 7, 2))

        assertNull(status.cycleLengthDays)
        assertNull(status.daysToNextPeriod)
    }
}
