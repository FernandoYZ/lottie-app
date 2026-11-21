package com.fernandoyz.lottie.domain

import com.fernandoyz.lottie.domain.model.MenstruationRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** STATISTICS §3. Synthetic dates only; never real user data. */
class CycleHistoryTest {

    private fun day(month: Int, dayOfMonth: Int) = LocalDate.of(2026, month, dayOfMonth)

    private val today = day(6, 4)
    private val april = MenstruationRecord(1, day(4, 6), day(4, 10))
    private val may = MenstruationRecord(2, day(5, 6), day(5, 9))
    private val june = MenstruationRecord(3, day(6, 3), day(6, 8)) // auto-filled past today

    private fun cycles(records: List<MenstruationRecord>, today: LocalDate = this.today) =
        CycleHistory.cycles(records, PeriodEstimation.estimate(records, manualCycleLengthDays = null, today = today), today)

    @Test
    fun `past cycles run from a start to the day before the next one, newest first`() {
        val cycles = cycles(listOf(april, may, june))

        assertEquals(listOf(day(6, 3), day(5, 6), day(4, 6)), cycles.map { it.start })
        assertEquals(CycleSummary(day(5, 6), day(6, 2), 28, null, 4, isCurrent = false), cycles[1])
        assertEquals(CycleSummary(day(4, 6), day(5, 5), 30, null, 5, isCurrent = false), cycles[2])
    }

    @Test
    fun `the current cycle ends the day before the estimated start and counts today`() {
        // Mean of 30 and 28 days: next start 02/07.
        val current = cycles(listOf(april, may, june)).first()

        assertEquals(CycleSummary(day(6, 3), day(7, 1), 29, 2, null, isCurrent = true), current)
    }

    @Test
    fun `a menstruation in progress has no duration yet`() {
        val open = MenstruationRecord(1, day(6, 3), null)

        assertEquals(null, cycles(listOf(open)).single().menstruationDays)
        assertEquals(null, cycles(listOf(june)).single().menstruationDays)
    }

    @Test
    fun `during a delay the current cycle has no end`() {
        val current = cycles(listOf(may), today = day(6, 10)).single() // estimated start 03/06

        assertEquals(CycleSummary(day(5, 6), null, null, 36, 4, isCurrent = true), current)
    }

    @Test
    fun `without records there are no cycles`() {
        assertTrue(cycles(emptyList()).isEmpty())
    }
}
