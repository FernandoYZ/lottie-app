package com.fernandoyz.lottie.data.privacy

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Privacy settings in the same Preferences DataStore as the setup (SECURITY §1).
 *
 * Key (keep stable, it is the persisted format): `app_lock_enabled` (Boolean), off by default.
 * Read and write failures are propagated to the caller.
 */
class PrivacyRepository(private val dataStore: DataStore<Preferences>) {

    val appLockEnabled: Flow<Boolean> = dataStore.data.map { it[APP_LOCK_ENABLED] ?: false }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        dataStore.edit { it[APP_LOCK_ENABLED] = enabled }
    }

    private companion object {
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
    }
}
