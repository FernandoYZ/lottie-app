package com.fernandoyz.lottie.data.reminders

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.fernandoyz.lottie.domain.Reminder
import com.fernandoyz.lottie.domain.ReminderSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime

/**
 * Reminder settings in the same Preferences DataStore as the setup (REMINDERS §2).
 *
 * Keys (keep stable, they are the persisted format):
 * - `reminders_enabled` (Set<String>): [Reminder] names that are on.
 * - `reminders_time_minutes` (Int): minutes after midnight of the daily check.
 * - `reminders_last_run_epoch_day` (Long): last day the check sent its reminders.
 *
 * Read and write failures are propagated to the caller.
 */
class ReminderRepository(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<ReminderSettings> = dataStore.data.map { preferences ->
        ReminderSettings(
            enabled = preferences[ENABLED].orEmpty().mapNotNull { name -> Reminder.entries.firstOrNull { it.name == name } }.toSet(),
            time = preferences[TIME_MINUTES]?.let { LocalTime.of(it / 60, it % 60) } ?: ReminderSettings.DEFAULT_TIME,
        )
    }

    suspend fun setEnabled(reminder: Reminder, enabled: Boolean) {
        dataStore.edit {
            val current = it[ENABLED].orEmpty()
            it[ENABLED] = if (enabled) current + reminder.name else current - reminder.name
        }
    }

    suspend fun setTime(time: LocalTime) {
        dataStore.edit { it[TIME_MINUTES] = time.hour * 60 + time.minute }
    }

    /** Marks [day] as run; false when it had already run that day. */
    suspend fun markRun(day: LocalDate): Boolean {
        var firstRun = false
        dataStore.edit {
            firstRun = it[LAST_RUN_EPOCH_DAY] != day.toEpochDay()
            it[LAST_RUN_EPOCH_DAY] = day.toEpochDay()
        }
        return firstRun
    }

    private companion object {
        val ENABLED = stringSetPreferencesKey("reminders_enabled")
        val TIME_MINUTES = intPreferencesKey("reminders_time_minutes")
        val LAST_RUN_EPOCH_DAY = longPreferencesKey("reminders_last_run_epoch_day")
    }
}
