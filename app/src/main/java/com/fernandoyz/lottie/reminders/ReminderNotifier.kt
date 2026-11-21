package com.fernandoyz.lottie.reminders

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.StringRes
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.fernandoyz.lottie.MainActivity
import com.fernandoyz.lottie.R
import com.fernandoyz.lottie.domain.Reminder

/** Shows the reminders (REMINDERS §1, §3). On the lock screen only the neutral public version is shown. */
object ReminderNotifier {

    const val EXTRA_OPEN_DAILY_LOG = "com.fernandoyz.lottie.OPEN_DAILY_LOG"
    private const val CHANNEL_ID = "reminders"

    fun show(context: Context, reminders: Set<Reminder>) {
        if (reminders.isEmpty() || !canNotify(context)) return
        val manager = NotificationManagerCompat.from(context)
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.reminders_channel_name))
                .build(),
        )
        reminders.forEach { reminder ->
            try {
                manager.notify(reminder.ordinal + 1, build(context, reminder))
            } catch (e: SecurityException) {
                // The permission was revoked between the check and the call.
            }
        }
    }

    fun canNotify(context: Context): Boolean {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun build(context: Context, reminder: Reminder) =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(reminder.textRes()))
            .setContentIntent(openApp(context, reminder))
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(
                NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(context.getString(R.string.app_name))
                    .setContentText(context.getString(R.string.reminder_public_text))
                    .build(),
            )
            .build()

    /** The daily log reminder opens today's log; the others open Home (REMINDERS §1). */
    private fun openApp(context: Context, reminder: Reminder): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            .putExtra(EXTRA_OPEN_DAILY_LOG, reminder == Reminder.DailyLog)
        return PendingIntent.getActivity(
            context,
            reminder.ordinal,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    @StringRes
    private fun Reminder.textRes(): Int = when (this) {
        Reminder.PeriodSoon -> R.string.reminder_period_soon
        Reminder.FertileWindow -> R.string.reminder_fertile_window
        Reminder.DailyLog -> R.string.reminder_daily_log
    }
}
