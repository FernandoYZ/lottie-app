package com.fernandoyz.lottie.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fernandoyz.lottie.LottieApplication
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

/** Runs the daily check, shows the due reminders and schedules the next check. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as LottieApplication
        try {
            ReminderNotifier.show(applicationContext, app.reminderCheck.run())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Do not log the exception: it may reference personal data. Try again tomorrow.
        }
        try {
            app.reminderScheduler.schedule(app.reminderRepository.settings.first(), ReminderScheduler.Mode.AfterCurrent)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // The next app start schedules it again.
        }
        return Result.success()
    }
}
