package com.fernandoyz.lottie

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.fernandoyz.lottie.data.dailylog.DailyLogRepository
import com.fernandoyz.lottie.data.history.HistoryRepository
import com.fernandoyz.lottie.data.history.LottieDatabase
import com.fernandoyz.lottie.data.privacy.DataEraser
import com.fernandoyz.lottie.data.privacy.PrivacyRepository
import com.fernandoyz.lottie.data.reminders.ReminderCheck
import com.fernandoyz.lottie.data.reminders.ReminderRepository
import com.fernandoyz.lottie.data.repository.SetupRepository
import com.fernandoyz.lottie.domain.ReminderSettings
import com.fernandoyz.lottie.reminders.ReminderScheduler
import com.fernandoyz.lottie.security.AppLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Clock

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "lottie_settings")

/** Manual dependency composition (ARCHITECTURE.md §4). */
class LottieApplication : Application() {

    private val clock: Clock = Clock.systemDefaultZone()
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val setupRepository: SetupRepository by lazy { SetupRepository(settingsDataStore) }

    private val database: LottieDatabase by lazy {
        Room.databaseBuilder(this, LottieDatabase::class.java, "lottie.db").build()
    }

    val historyRepository: HistoryRepository by lazy {
        HistoryRepository(database.menstruationRecordDao(), setupRepository)
    }

    val dailyLogRepository: DailyLogRepository by lazy { DailyLogRepository(database.dailyLogDao()) }

    val reminderRepository: ReminderRepository by lazy { ReminderRepository(settingsDataStore) }

    val privacyRepository: PrivacyRepository by lazy { PrivacyRepository(settingsDataStore) }

    /** "Borrar todos los datos" (SECURITY §2). Room clears its tables off the main thread. */
    val dataEraser: DataEraser by lazy {
        DataEraser(
            clearDatabase = { withContext(Dispatchers.IO) { database.clearAllTables() } },
            dataStore = settingsDataStore,
            cancelReminders = { reminderScheduler.schedule(ReminderSettings(), ReminderScheduler.Mode.Replace) },
        )
    }

    /** Process-wide, so a new process starts locked again (SECURITY §1). */
    val appLock = AppLock()

    val reminderScheduler: ReminderScheduler by lazy { ReminderScheduler(this, clock) }

    val reminderCheck: ReminderCheck by lazy {
        ReminderCheck(reminderRepository, historyRepository, setupRepository, dailyLogRepository, clock)
    }

    override fun onCreate() {
        super.onCreate()
        // Restores the daily check if it was lost; a pending one is kept (REMINDERS §4).
        appScope.launch {
            try {
                reminderScheduler.schedule(reminderRepository.settings.first(), ReminderScheduler.Mode.KeepPending)
            } catch (e: Exception) {
                // Do not log: settings are personal data. The next start tries again.
            }
        }
    }
}
