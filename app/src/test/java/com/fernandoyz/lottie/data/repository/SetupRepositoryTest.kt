package com.fernandoyz.lottie.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.fernandoyz.lottie.data.FailingDataStore
import com.fernandoyz.lottie.domain.model.InitialSetup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import java.time.LocalDate

class SetupRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val scopes = mutableListOf<CoroutineScope>()

    private val setup = InitialSetup(
        lastMenstruationDate = LocalDate.of(2026, 11, 2),
        usualCycleLengthDays = 28,
    )

    private fun dataStoreFile(): File = File(tempFolder.root, "lottie_settings.preferences_pb")

    /** Opens a DataStore on the shared file; closing its scope simulates the process ending. */
    private fun openDataStore(): Pair<DataStore<Preferences>, CoroutineScope> {
        val scope = CoroutineScope(Dispatchers.IO + Job()).also(scopes::add)
        return PreferenceDataStoreFactory.create(scope = scope, produceFile = ::dataStoreFile) to scope
    }

    @After
    fun tearDown() {
        scopes.forEach { it.cancel() }
    }

    @Test
    fun `fresh install has no completed setup`() = runTest {
        val (dataStore, _) = openDataStore()

        assertNull(SetupRepository(dataStore).initialSetup.first())
    }

    @Test
    fun `saved setup is read back`() = runTest {
        val (dataStore, _) = openDataStore()
        val repository = SetupRepository(dataStore)

        repository.save(setup)

        assertEquals(setup, repository.initialSetup.first())
    }

    @Test
    fun `saved setup survives a new DataStore instance on the same file`() = runTest {
        val (firstDataStore, firstScope) = openDataStore()
        SetupRepository(firstDataStore).save(setup)
        firstScope.cancel()
        firstScope.coroutineContext[Job]?.join()

        val (secondDataStore, _) = openDataStore()

        assertEquals(setup, SetupRepository(secondDataStore).initialSetup.first())
    }

    @Test
    fun `completed flag without stored values is treated as not completed`() = runTest {
        val (dataStore, _) = openDataStore()
        dataStore.edit { it[booleanPreferencesKey("onboarding_completed")] = true }

        assertNull(SetupRepository(dataStore).initialSetup.first())
    }

    @Test
    fun `write failure is propagated to the caller`() = runTest {
        val repository = SetupRepository(FailingDataStore(failWrites = true))

        try {
            repository.save(setup)
            fail("Expected save to throw")
        } catch (e: IOException) {
            assertTrue(e.message.orEmpty().contains("write"))
        }
    }

    @Test(expected = IOException::class)
    fun `read failure is propagated to the collector`() = runTest {
        SetupRepository(FailingDataStore(failReads = true)).initialSetup.first()
    }
}
