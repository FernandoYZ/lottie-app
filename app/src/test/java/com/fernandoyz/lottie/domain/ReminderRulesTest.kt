package com.fernandoyz.lottie.domain

import com.fernandoyz.lottie.domain.model.MenstruationRecord
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** REMINDERS §1. Synthetic dates only; never real user data. */
class ReminderRulesTest {

    // 01/06 - 04/06 with a 28 day cycle: next start 29/06, fertile window 10/06 - 15/06.
    private val record = MenstruationRecord(1, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 4))
    private val all = Reminder.entries.toSet()

    private fun due(
        today: LocalDate,
        records: List<MenstruationRecord> = listOf(record),
        loggedToday: Boolean = true,
        enabled: Set<Reminder> = all,
    ) = ReminderRules.due(enabled, records, manualCycleLengthDays = null, loggedToday = loggedToday, today = today)

    @Test
    fun `period reminder is due two days before the estimated start`() {
        assertEquals(setOf(Reminder.PeriodSoon), due(LocalDate.of(2026, 6, 27)))
        assertEquals(emptySet<Reminder>(), due(LocalDate.of(2026, 6, 26)))
        assertEquals(emptySet<Reminder>(), due(LocalDate.of(2026, 6, 28)))
    }

    @Test
    fun `period reminder is not due during a menstruation in progress`() {
        // A long ongoing menstruation reaching two days before the estimated start.
        val ongoing = MenstruationRecord(1, LocalDate.of(2026, 6, 1), null)

        assertEquals(emptySet<Reminder>(), due(LocalDate.of(2026, 6, 27), records = listOf(ongoing)))
    }

    @Test
    fun `fertile window reminder is due only on its first day`() {
        assertEquals(setOf(Reminder.FertileWindow), due(LocalDate.of(2026, 6, 10)))
        assertEquals(emptySet<Reminder>(), due(LocalDate.of(2026, 6, 9)))
        assertEquals(emptySet<Reminder>(), due(LocalDate.of(2026, 6, 11)))
    }

    @Test
    fun `daily log reminder is due only when today has no log`() {
        assertEquals(setOf(Reminder.DailyLog), due(LocalDate.of(2026, 6, 20), loggedToday = false))
        assertEquals(emptySet<Reminder>(), due(LocalDate.of(2026, 6, 20), loggedToday = true))
    }

    @Test
    fun `without records only the daily log reminder can be due`() {
        assertEquals(setOf(Reminder.DailyLog), due(LocalDate.of(2026, 6, 20), records = emptyList(), loggedToday = false))
    }

    @Test
    fun `disabled reminders are never due`() {
        assertEquals(
            emptySet<Reminder>(),
            due(LocalDate.of(2026, 6, 10), loggedToday = false, enabled = setOf(Reminder.PeriodSoon)),
        )
    }

    @Test
    fun `next run is today at the chosen time when it has not passed`() {
        assertEquals(
            LocalDateTime.of(2026, 6, 10, 9, 0),
            ReminderRules.nextRun(LocalDateTime.of(2026, 6, 10, 7, 30), LocalTime.of(9, 0)),
        )
    }

    @Test
    fun `next run is tomorrow when the time has passed or is about to`() {
        val nine = LocalTime.of(9, 0)
        assertEquals(LocalDateTime.of(2026, 6, 11, 9, 0), ReminderRules.nextRun(LocalDateTime.of(2026, 6, 10, 9, 5), nine))
        // A worker that wakes a few seconds early must not schedule itself again for the same day.
        assertEquals(LocalDateTime.of(2026, 6, 11, 9, 0), ReminderRules.nextRun(LocalDateTime.of(2026, 6, 10, 8, 59, 50), nine))
    }
}
