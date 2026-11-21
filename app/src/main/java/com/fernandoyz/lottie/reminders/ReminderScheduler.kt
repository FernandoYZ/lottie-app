package com.fernandoyz.lottie.reminders

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.fernandoyz.lottie.domain.ReminderRules
import com.fernandoyz.lottie.domain.ReminderSettings
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Schedules the daily check with WorkManager (REMINDERS §4): one pending run at the chosen time,
 * which schedules the next one. WorkManager keeps it across reboots; the time is not exact.
 */
class ReminderScheduler(private val context: Context, private val clock: Clock) {

    /** How a new run relates to the pending one. */
    enum class Mode {
        /** Settings changed: the new time replaces the pending run. */
        Replace,

        /** App start: keep the pending run if there is one. */
        KeepPending,

        /** From the running check: queue after it, so it does not cancel itself. */
        AfterCurrent,
    }

    fun schedule(settings: ReminderSettings, mode: Mode) {
        val workManager = WorkManager.getInstance(context)
        if (settings.enabled.isEmpty()) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }
        val now = LocalDateTime.now(clock)
        val delay = Duration.between(now, ReminderRules.nextRun(now, settings.time))
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .build()
        val policy = when (mode) {
            Mode.Replace -> ExistingWorkPolicy.REPLACE
            Mode.KeepPending -> ExistingWorkPolicy.KEEP
            Mode.AfterCurrent -> ExistingWorkPolicy.APPEND_OR_REPLACE
        }
        workManager.enqueueUniqueWork(WORK_NAME, policy, request)
    }

    private companion object {
        const val WORK_NAME = "daily_reminders"
    }
}
