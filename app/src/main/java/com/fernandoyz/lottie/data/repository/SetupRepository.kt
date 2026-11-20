package com.fernandoyz.lottie.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.fernandoyz.lottie.domain.model.InitialSetup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * Stores the initial setup in Preferences DataStore.
 *
 * Keys (keep stable, they are the persisted format):
 * - `last_menstruation_epoch_day` (Long): [LocalDate.toEpochDay], a calendar day with no time zone.
 * - `last_menstruation_end_epoch_day` (Long, optional): end of that menstruation.
 * - `usual_cycle_length_days` (Int, optional): manual cycle length set in Settings. Since STAGE_4 it is
 *   no longer asked during setup; a value saved by an earlier version is kept as the manual value.
 * - `onboarding_completed` (Boolean).
 * - `history_seeded` (Boolean): the onboarding date was turned into the first history record.
 *
 * Read and write failures are propagated to the caller; they are never hidden.
 */
class SetupRepository(private val dataStore: DataStore<Preferences>) {

    /** Emits the completed setup, or `null` while the onboarding is not completed. */
    val initialSetup: Flow<InitialSetup?> = dataStore.data.map { preferences ->
        val completed = preferences[ONBOARDING_COMPLETED] ?: false
        val epochDay = preferences[LAST_MENSTRUATION_EPOCH_DAY]
        if (completed && epochDay != null) {
            InitialSetup(
                lastMenstruationDate = LocalDate.ofEpochDay(epochDay),
                lastMenstruationEndDate = preferences[LAST_MENSTRUATION_END_EPOCH_DAY]?.let(LocalDate::ofEpochDay),
            )
        } else {
            null
        }
    }

    /** Manual cycle length; null means it is learned from the history (STAGE_4 §3). */
    val manualCycleLengthDays: Flow<Int?> = dataStore.data.map { it[USUAL_CYCLE_LENGTH_DAYS] }

    /** Saves all values atomically and marks the onboarding as completed. */
    suspend fun save(setup: InitialSetup) {
        dataStore.edit { preferences ->
            preferences[LAST_MENSTRUATION_EPOCH_DAY] = setup.lastMenstruationDate.toEpochDay()
            val end = setup.lastMenstruationEndDate
            if (end != null) preferences[LAST_MENSTRUATION_END_EPOCH_DAY] = end.toEpochDay()
            else preferences.remove(LAST_MENSTRUATION_END_EPOCH_DAY)
            preferences[ONBOARDING_COMPLETED] = true
        }
    }

    /** Sets the manual cycle length, or removes it with null so it is learned again. */
    suspend fun setManualCycleLength(days: Int?) {
        dataStore.edit { if (days != null) it[USUAL_CYCLE_LENGTH_DAYS] = days else it.remove(USUAL_CYCLE_LENGTH_DAYS) }
    }

    /** Whether the onboarding date was already turned into the first history record (STAGE_2 §2). */
    suspend fun isHistorySeeded(): Boolean = dataStore.data.first()[HISTORY_SEEDED] ?: false

    suspend fun markHistorySeeded() {
        dataStore.edit { it[HISTORY_SEEDED] = true }
    }

    private companion object {
        val HISTORY_SEEDED = booleanPreferencesKey("history_seeded")
        val LAST_MENSTRUATION_EPOCH_DAY = longPreferencesKey("last_menstruation_epoch_day")
        val LAST_MENSTRUATION_END_EPOCH_DAY = longPreferencesKey("last_menstruation_end_epoch_day")
        val USUAL_CYCLE_LENGTH_DAYS = intPreferencesKey("usual_cycle_length_days")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }
}
