package com.fernandoyz.lottie.data.privacy

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.fernandoyz.lottie.data.InMemoryDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** SECURITY §2. */
class DataEraserTest {

    private val dataStore = InMemoryDataStore()
    private val key = booleanPreferencesKey("any_setting")
    private val steps = mutableListOf<String>()

    private fun eraser(failDatabase: Boolean = false) = DataEraser(
        clearDatabase = { if (failDatabase) error("disk") else steps += "database" },
        dataStore = dataStore,
        cancelReminders = { steps += "reminders" },
    )

    @Test
    fun `erasing clears the database, the settings and the reminders`() = runTest {
        dataStore.edit { it[key] = true }

        eraser().eraseAll()

        assertEquals(listOf("database", "reminders"), steps)
        assertTrue(dataStore.data.first().asMap().isEmpty())
    }

    @Test
    fun `if the database cannot be cleared the settings stay, so the app does not restart as new with data left`() = runTest {
        dataStore.edit { it[key] = true }

        val result = runCatching { eraser(failDatabase = true).eraseAll() }

        assertTrue(result.isFailure)
        assertEquals(true, dataStore.data.first()[key])
        assertTrue(steps.isEmpty())
    }
}
