package com.fernandoyz.lottie.presentation.settings

import com.fernandoyz.lottie.MainDispatcherRule
import com.fernandoyz.lottie.data.FailingDataStore
import com.fernandoyz.lottie.data.InMemoryDataStore
import com.fernandoyz.lottie.data.repository.SetupRepository
import com.fernandoyz.lottie.domain.CycleLengthValidation
import com.fernandoyz.lottie.domain.model.InitialSetup
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/** Synthetic values only; never real user data. */
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val setupDate = LocalDate.of(2026, 6, 1)
    private val repository = SetupRepository(InMemoryDataStore())

    private suspend fun completeSetup(days: Int = 28) = repository.save(InitialSetup(setupDate, days))

    @Test
    fun `current cycle length is loaded`() = runTest {
        completeSetup(28)

        val state = SettingsViewModel(repository).uiState.value

        assertFalse(state.isLoading)
        assertEquals("28", state.cycleLengthInput)
        assertFalse(state.canSave) // unchanged value
    }

    @Test
    fun `new valid value is saved and keeps the rest of the setup`() = runTest {
        completeSetup(28)
        val vm = SettingsViewModel(repository)

        vm.onCycleLengthChanged("31")
        vm.onSave()

        assertTrue(vm.uiState.value.isSaved)
        assertEquals(InitialSetup(setupDate, 31), repository.initialSetup.first())
    }

    @Test
    fun `value outside technical limit is rejected`() = runTest {
        completeSetup()
        val vm = SettingsViewModel(repository)

        vm.onCycleLengthChanged("100")

        assertEquals(CycleLengthValidation.OutOfRange, vm.uiState.value.cycleLengthError)
        assertFalse(vm.uiState.value.canSave)
    }

    @Test
    fun `value outside reference range shows warning and can be saved`() = runTest {
        completeSetup()
        val vm = SettingsViewModel(repository)

        vm.onCycleLengthChanged("50")

        assertTrue(vm.uiState.value.showReferenceRangeWarning)
        assertTrue(vm.uiState.value.canSave)
    }

    @Test
    fun `save failure is reported and not shown as saved`() = runTest {
        val failing = SetupRepository(FailingDataStore(failReads = false, failWrites = true))
        val vm = SettingsViewModel(failing)

        vm.onCycleLengthChanged("30")
        vm.onSave()

        assertTrue(vm.uiState.value.saveFailed)
        assertFalse(vm.uiState.value.isSaved)
    }
}
