package com.fernandoyz.lottie.data.reminders

import com.fernandoyz.lottie.data.dailylog.DailyLogRepository
import com.fernandoyz.lottie.data.history.HistoryRepository
import com.fernandoyz.lottie.data.repository.SetupRepository
import com.fernandoyz.lottie.domain.Reminder
import com.fernandoyz.lottie.domain.ReminderRules
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalDate

/** The daily check (REMINDERS §1): which reminders to send today, with the current data, once per day. */
class ReminderCheck(
    private val reminderRepository: ReminderRepository,
    private val historyRepository: HistoryRepository,
    private val setupRepository: SetupRepository,
    private val dailyLogRepository: DailyLogRepository,
    private val clock: Clock,
) {
    suspend fun run(): Set<Reminder> {
        val today = LocalDate.now(clock)
        val settings = reminderRepository.settings.first()
        if (settings.enabled.isEmpty() || !reminderRepository.markRun(today)) return emptySet()

        return ReminderRules.due(
            enabled = settings.enabled,
            records = historyRepository.records.first(),
            manualCycleLengthDays = setupRepository.manualCycleLengthDays.first(),
            loggedToday = today in dailyLogRepository.logs.first(),
            today = today,
        )
    }
}
