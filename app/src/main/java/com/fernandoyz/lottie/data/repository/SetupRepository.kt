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
 * - `usual_cycle_length_days` (Int).
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
        val cycleLength = preferences[USUAL_CYCLE_LENGTH_DAYS]
        if (completed && epochDay != null && cycleLength != null) {
            InitialSetup(LocalDate.ofEpochDay(epochDay), cycleLength)
        } else {
            null
        }
    }

    /** Saves all values atomically and marks the onboarding as completed. */
    suspend fun save(setup: InitialSetup) {
        dataStore.edit { preferences ->
            preferences[LAST_MENSTRUATION_EPOCH_DAY] = setup.lastMenstruationDate.toEpochDay()
            preferences[USUAL_CYCLE_LENGTH_DAYS] = setup.usualCycleLengthDays
            preferences[ONBOARDING_COMPLETED] = true
        }
    }

    /** Whether the onboarding date was already turned into the first history record (STAGE_2 §2). */
    suspend fun isHistorySeeded(): Boolean = dataStore.data.first()[HISTORY_SEEDED] ?: false

    suspend fun markHistorySeeded() {
        dataStore.edit { it[HISTORY_SEEDED] = true }
    }

    private companion object {
        val HISTORY_SEEDED = booleanPreferencesKey("history_seeded")
        val LAST_MENSTRUATION_EPOCH_DAY = longPreferencesKey("last_menstruation_epoch_day")
        val USUAL_CYCLE_LENGTH_DAYS = intPreferencesKey("usual_cycle_length_days")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }
}
