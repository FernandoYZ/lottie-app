package com.fernandoyz.lottie.domain

import com.fernandoyz.lottie.domain.model.MenstruationRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** Synthetic dates only; never real user data. */
class CyclePhasesTest {

    private fun day(month: Int, dayOfMonth: Int) = LocalDate.of(2026, month, dayOfMonth)
    private val today = day(7, 10)

    // 28-day cycle, 5 + 4 days of bleeding.
    private val june = MenstruationRecord(1, day(6, 1), day(6, 5))
    private val july = MenstruationRecord(2, day(6, 29), day(7, 2))
    private val records = listOf(july, june)
    private val estimate = PeriodEstimation.estimate(records, manualCycleLengthDays = null, today = today) // 27/07 - 31/07

    private fun info(date: LocalDate, list: List<MenstruationRecord> = records) =
        CyclePhases.dayInfo(date, list, PeriodEstimation.estimate(list, null, today), today)

    @Test
    fun `marked days are menstrual and count the cycle day from the start`() =
        assertEquals(CycleDayInfo(cycleDay = 3, phase = CyclePhase.Menstrual, basedOnEstimate = false), info(day(6, 3)))

    @Test
    fun `ovulation is 14 days before the next real start`() =
        assertEquals(CycleDayInfo(15, CyclePhase.Ovulation, basedOnEstimate = false), info(day(6, 15)))

    @Test
    fun `days between menstruation and ovulation are follicular, after it luteal`() {
        assertEquals(CyclePhase.Follicular, info(day(6, 10))?.phase)
        assertEquals(CyclePhase.Luteal, info(day(6, 20))?.phase)
        assertEquals(CyclePhase.Luteal, info(day(6, 28))?.phase)
    }

    @Test
    fun `the current cycle uses the estimated next start`() =
        assertEquals(CycleDayInfo(15, CyclePhase.Ovulation, basedOnEstimate = true), info(day(7, 13)))

    @Test
    fun `cycles after the estimate repeat the estimated length`() {
        // Estimated cycle starting 27/07: menstruation 27/07 - 31/07, next start 24/08, ovulation 10/08.
        assertEquals(CycleDayInfo(7, CyclePhase.Follicular, basedOnEstimate = true), info(day(8, 2)))
        assertEquals(CyclePhase.Menstrual, info(day(7, 29))?.phase)
        assertEquals(CyclePhase.Ovulation, info(day(8, 10))?.phase)
    }

    @Test
    fun `days before the first record have no cycle information`() = assertNull(info(day(5, 20)))

    @Test
    fun `no phase outside menstruation when the ovulation falls inside it`() {
        val short = listOf(MenstruationRecord(2, day(6, 15), day(6, 18)), MenstruationRecord(1, day(6, 1), day(6, 6)))

        assertEquals(CycleDayInfo(10, phase = null, basedOnEstimate = false), info(day(6, 10), short))
        assertEquals(CyclePhase.Menstrual, info(day(6, 3), short)?.phase)
    }

    @Test
    fun `ovulation days of a month include real and estimated cycles`() {
        assertEquals(setOf(day(6, 15)), CyclePhases.ovulationDays(YearMonth.of(2026, 6), records, estimate, today))
        assertEquals(setOf(day(7, 13)), CyclePhases.ovulationDays(YearMonth.of(2026, 7), records, estimate, today))
    }
}
