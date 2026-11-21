package com.fernandoyz.lottie.domain

import com.fernandoyz.lottie.domain.model.DailyLog
import com.fernandoyz.lottie.domain.model.MenstruationRecord
import com.fernandoyz.lottie.domain.model.Mood
import com.fernandoyz.lottie.domain.model.Symptom
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** CYCLE_SUMMARY §2. Synthetic data only; never real user data. */
class CycleSummaryStatsTest {

    private fun day(month: Int, dayOfMonth: Int) = LocalDate.of(2026, month, dayOfMonth)
    private val today = day(7, 10)

    // Past cycle 01/06 - 28/06: menstrual 01-05, follicular 06-14, ovulation 15, luteal 16-28.
    private val june = MenstruationRecord(1, day(6, 1), day(6, 5))
    private val july = MenstruationRecord(2, day(6, 29), day(7, 2))
    private val records = listOf(june, july)
    private val estimate = PeriodEstimation.estimate(records, manualCycleLengthDays = null, today = today)
    private val juneCycle = CycleHistory.cycles(records, estimate, today).last()

    private val logs = listOf(
        DailyLog(day(6, 2), symptoms = setOf(Symptom.Cramps), moodLevel = 2, energyLevel = 1),
        DailyLog(day(6, 3), symptoms = setOf(Symptom.Cramps, Symptom.Headache), moods = setOf(Mood.Sad)),
        DailyLog(day(6, 10), moods = setOf(Mood.Happy), moodLevel = 4, energyLevel = 5),
        DailyLog(day(6, 20), symptoms = setOf(Symptom.Cramps), energyLevel = 3),
        DailyLog(day(7, 5), symptoms = setOf(Symptom.Acne)), // next cycle: never counted in June
    ).associateBy { it.date }

    private fun summary(range: SummaryRange) = CycleSummaryStats.summarize(juneCycle, range, records, estimate, logs, today)

    @Test
    fun `the whole cycle counts logged days out of its days`() {
        val whole = summary(SummaryRange.WholeCycle)

        assertEquals(28, whole.days.size)
        assertEquals(4, whole.loggedDays)
    }

    @Test
    fun `the most logged options are counted across categories, highest first, ties in log screen order`() {
        val whole = summary(SummaryRange.WholeCycle)

        assertEquals(listOf<Any>(Symptom.Cramps, Mood.Happy, Mood.Sad, Symptom.Headache), whole.topOptions.map { it.first })
        assertEquals(3, whole.topOptions.first().second)
    }

    @Test
    fun `each phase only looks at its own days`() {
        val menstrual = summary(SummaryRange.Menstrual)
        assertEquals((1..5).map { day(6, it) }, menstrual.days)
        assertEquals(2, menstrual.loggedDays)
        assertEquals(Symptom.Cramps to 2, menstrual.topOptions.first())

        assertEquals(listOf(day(6, 15)), summary(SummaryRange.Ovulation).days)
        assertEquals(0, summary(SummaryRange.Ovulation).loggedDays)
        assertEquals(listOf(day(6, 10)), summary(SummaryRange.Follicular).levels.filter { it.mood != null }.map { it.date })
    }

    @Test
    fun `levels are kept per day, empty when not logged`() {
        val levels = summary(SummaryRange.Menstrual).levels

        assertEquals(DayLevels(day(6, 2), mood = 2, energy = 1), levels[1])
        assertEquals(DayLevels(day(6, 4), mood = null, energy = null), levels[3])
    }

    @Test
    fun `the current cycle only counts days until today`() {
        val current = CycleHistory.cycles(records, estimate, today).first() // 29/06 - estimated end

        assertEquals(12, CycleSummaryStats.summarize(current, SummaryRange.WholeCycle, records, estimate, logs, today).days.size)
    }
}
