package com.fernandoyz.lottie.domain

import com.fernandoyz.lottie.domain.model.MenstruationRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/** Synthetic dates only; never real user data. */
class CycleStatisticsTest {

    private fun day(month: Int, dayOfMonth: Int) = LocalDate.of(2026, month, dayOfMonth)
    private val today = day(6, 15)

    @Test
    fun `cycle length is unknown with fewer than two starts`() {
        assertNull(CycleStatistics.averageCycleLengthDays(emptyList()))
        assertNull(CycleStatistics.averageCycleLengthDays(listOf(MenstruationRecord(1, day(6, 1), day(6, 4)))))
    }

    @Test
    fun `cycle length is the mean interval between consecutive starts, rounded half up`() {
        val records = listOf(
            MenstruationRecord(3, day(5, 30), day(6, 3)), // 29 days after 01/05
            MenstruationRecord(1, day(4, 1), day(4, 4)),
            MenstruationRecord(2, day(5, 1), day(5, 4)), // 30 days after 01/04 -> mean 29.5 -> 30
        )

        assertEquals(30, CycleStatistics.averageCycleLengthDays(records))
    }

    @Test
    fun `period length defaults to 6 without finished records`() {
        assertEquals(6, CycleStatistics.periodLengthDays(emptyList(), today))
        assertEquals(6, CycleStatistics.periodLengthDays(listOf(MenstruationRecord(1, day(6, 14), null)), today))
    }

    @Test
    fun `period length is the mean of finished records, ignoring ends after today`() {
        val records = listOf(
            MenstruationRecord(1, day(4, 1), day(4, 4)), // 4 days
            MenstruationRecord(2, day(5, 1), day(5, 5)), // 5 days -> mean 4.5 -> 5
            MenstruationRecord(3, day(6, 14), day(6, 19)), // still auto-filled into the future: ignored
        )

        assertEquals(5, CycleStatistics.periodLengthDays(records, today))
    }
}
