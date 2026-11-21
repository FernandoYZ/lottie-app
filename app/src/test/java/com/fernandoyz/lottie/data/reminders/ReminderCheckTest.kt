package com.fernandoyz.lottie.data.reminders

import com.fernandoyz.lottie.data.InMemoryDataStore
import com.fernandoyz.lottie.data.dailylog.DailyLogRepository
import com.fernandoyz.lottie.data.dailylog.FakeDailyLogDao
import com.fernandoyz.lottie.data.history.FakeMenstruationRecordDao
import com.fernandoyz.lottie.data.history.HistoryRepository
import com.fernandoyz.lottie.data.repository.SetupRepository
import com.fernandoyz.lottie.domain.Reminder
import com.fernandoyz.lottie.domain.model.DailyLog
import com.fernandoyz.lottie.domain.model.Mood
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

/** REMINDERS §1 through the stored data. Synthetic dates only; never real user data. */
class ReminderCheckTest {

    private val dataStore = InMemoryDataStore()
    private val setup = SetupRepository(dataStore)
    private val reminders = ReminderRepository(dataStore)
    private val history = HistoryRepository(FakeMenstruationRecordDao(), setup)
    private val dailyLogs = DailyLogRepository(FakeDailyLogDao())

    private fun check(today: LocalDate) = ReminderCheck(
        reminders, history, setup, dailyLogs,
        Clock.fixed(today.atTime(9, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC),
    )

    @Test
    fun `sends the enabled reminders due today with the stored data`() = runTest {
        // 01/06 - 04/06 with a manual 26 day cycle: next start 27/06, two days of notice on 25/06.
        val today = LocalDate.of(2026, 6, 25)
        history.add(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 4), today)
        setup.setManualCycleLength(26)
        Reminder.entries.forEach { reminders.setEnabled(it, true) }

        assertEquals(setOf(Reminder.PeriodSoon, Reminder.DailyLog), check(today).run())
    }

    @Test
    fun `a day with a log does not get the daily log reminder`() = runTest {
        val today = LocalDate.of(2026, 6, 20)
        reminders.setEnabled(Reminder.DailyLog, true)
        dailyLogs.save(DailyLog(today, moods = setOf(Mood.Calm)))

        assertEquals(emptySet<Reminder>(), check(today).run())
    }

    @Test
    fun `runs only once per day`() = runTest {
        val today = LocalDate.of(2026, 6, 20)
        reminders.setEnabled(Reminder.DailyLog, true)

        assertEquals(setOf(Reminder.DailyLog), check(today).run())
        assertEquals(emptySet<Reminder>(), check(today).run())
        assertEquals(setOf(Reminder.DailyLog), check(today.plusDays(1)).run())
    }

    @Test
    fun `with every reminder off nothing is sent`() = runTest {
        assertEquals(emptySet<Reminder>(), check(LocalDate.of(2026, 6, 20)).run())
    }
}
