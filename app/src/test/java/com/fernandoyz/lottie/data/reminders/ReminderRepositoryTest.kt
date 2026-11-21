package com.fernandoyz.lottie.data.reminders

import com.fernandoyz.lottie.data.InMemoryDataStore
import com.fernandoyz.lottie.domain.Reminder
import com.fernandoyz.lottie.domain.ReminderSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/** REMINDERS §2. Synthetic values only. */
class ReminderRepositoryTest {

    private val repository = ReminderRepository(InMemoryDataStore())

    @Test
    fun `all reminders are off at 09_00 by default`() = runTest {
        assertEquals(ReminderSettings(enabled = emptySet(), time = LocalTime.of(9, 0)), repository.settings.first())
    }

    @Test
    fun `switches and time are stored`() = runTest {
        repository.setEnabled(Reminder.DailyLog, true)
        repository.setEnabled(Reminder.PeriodSoon, true)
        repository.setEnabled(Reminder.PeriodSoon, false)
        repository.setTime(LocalTime.of(20, 30))

        assertEquals(ReminderSettings(setOf(Reminder.DailyLog), LocalTime.of(20, 30)), repository.settings.first())
    }

    @Test
    fun `a day is marked as run only once`() = runTest {
        val day = LocalDate.of(2026, 6, 10)

        assertTrue(repository.markRun(day))
        assertFalse(repository.markRun(day))
        assertTrue(repository.markRun(day.plusDays(1)))
    }
}
