package com.fernandoyz.lottie

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fernandoyz.lottie.presentation.start.LottieApp
import com.fernandoyz.lottie.reminders.ReminderNotifier
import com.fernandoyz.lottie.reminders.ReminderScheduler
import com.fernandoyz.lottie.ui.theme.LottieTheme
import java.time.Clock

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as LottieApplication
        val clock = Clock.systemDefaultZone()
        // Only on a fresh start: after a configuration change the user is already where they want.
        val openDailyLogToday = savedInstanceState == null && intent.getBooleanExtra(ReminderNotifier.EXTRA_OPEN_DAILY_LOG, false)
        setContent {
            LottieTheme {
                LottieApp(
                    setupRepository = app.setupRepository,
                    historyRepository = app.historyRepository,
                    dailyLogRepository = app.dailyLogRepository,
                    reminderRepository = app.reminderRepository,
                    onRemindersChanged = { app.reminderScheduler.schedule(it, ReminderScheduler.Mode.Replace) },
                    clock = clock,
                    openDailyLogToday = openDailyLogToday,
                )
            }
        }
    }
}
