package com.fernandoyz.lottie.data.privacy

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit

/**
 * "Borrar todos los datos" (SECURITY §2): menstruations, daily logs, reminders and settings, as a fresh install.
 * The database goes first: if it fails, the settings (and the completed setup) stay, so the app never
 * restarts as new while data is left behind. Failures are propagated to the caller.
 */
class DataEraser(
    private val clearDatabase: suspend () -> Unit,
    private val dataStore: DataStore<Preferences>,
    private val cancelReminders: () -> Unit,
) {

    suspend fun eraseAll() {
        clearDatabase()
        dataStore.edit { it.clear() }
        cancelReminders()
    }
}
