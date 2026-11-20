package com.fernandoyz.lottie.presentation.settings

import com.fernandoyz.lottie.MainDispatcherRule
import com.fernandoyz.lottie.data.FailingDataStore
import com.fernandoyz.lottie.data.InMemoryDataStore
import com.fernandoyz.lottie.data.history.FakeMenstruationRecordDao
import com.fernandoyz.lottie.data.history.HistoryRepository
import com.fernandoyz.lottie.data.repository.SetupRepository
import com.fernandoyz.lottie.domain.CycleLengthValidation
import com.fernandoyz.lottie.domain.model.InitialSetup
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/** Synthetic values only; never real user data. */
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val setupDate = LocalDate.of(2026, 6, 1)
    private val today = LocalDate.of(2026, 6, 15)
    private val repository = SetupRepository(InMemoryDataStore())
    private val history = HistoryRepository(FakeMenstruationRecordDao(), repository)

    private suspend fun completeSetup() = repository.save(InitialSetup(setupDate, setupDate.plusDays(3)))

    @Test
    fun `without a manual value the field is empty and the learned length is shown`() = runTest {
        completeSetup()
        history.add(LocalDate.of(2026, 5, 2), LocalDate.of(2026, 5, 5), today)
        history.add(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 4), today) // 30 days later

        val state = SettingsViewModel(repository, history).uiState.value

        assertFalse(state.isLoading)
        assertEquals("", state.cycleLengthInput)
        assertEquals(30, state.learnedCycleLengthDays)
        assertFalse(state.canSave) // unchanged
    }

    @Test
    fun `learned length is the default 28 with fewer than two starts`() = runTest {
        completeSetup()

        assertEquals(28, SettingsViewModel(repository, history).uiState.value.learnedCycleLengthDays)
    }

    @Test
    fun `manual value is saved`() = runTest {
        completeSetup()
        val vm = SettingsViewModel(repository, history)

        vm.onCycleLengthChanged("31")
        vm.onSave()

        assertTrue(vm.uiState.value.isSaved)
        assertEquals(31, repository.manualCycleLengthDays.first())
    }

    @Test
    fun `clearing the field goes back to the learned value`() = runTest {
        completeSetup()
        repository.setManualCycleLength(31)
        val vm = SettingsViewModel(repository, history)
        assertEquals("31", vm.uiState.value.cycleLengthInput)

        vm.onCycleLengthChanged("")
        assertNull(vm.uiState.value.cycleLengthError)
        vm.onSave()

        assertNull(repository.manualCycleLengthDays.first())
    }

    @Test
    fun `value outside technical limit is rejected`() = runTest {
        completeSetup()
        val vm = SettingsViewModel(repository, history)

        vm.onCycleLengthChanged("100")

        assertEquals(CycleLengthValidation.OutOfRange, vm.uiState.value.cycleLengthError)
        assertFalse(vm.uiState.value.canSave)
    }

    @Test
    fun `value outside reference range shows warning and can be saved`() = runTest {
        completeSetup()
        val vm = SettingsViewModel(repository, history)

        vm.onCycleLengthChanged("50")

        assertTrue(vm.uiState.value.showReferenceRangeWarning)
        assertTrue(vm.uiState.value.canSave)
    }

    @Test
    fun `save failure is reported and not shown as saved`() = runTest {
        val failing = SetupRepository(FailingDataStore(failReads = false, failWrites = true))
        val vm = SettingsViewModel(failing, HistoryRepository(FakeMenstruationRecordDao(), failing))

        vm.onCycleLengthChanged("30")
        vm.onSave()

        assertTrue(vm.uiState.value.saveFailed)
        assertFalse(vm.uiState.value.isSaved)
    }

    @Test
    fun `cycle length not longer than the longest recorded bleeding is rejected`() = runTest {
        completeSetup()
        history.add(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 5), today) // 5 days
        val vm = SettingsViewModel(repository, history)

        vm.onCycleLengthChanged("5")

        assertEquals(CycleLengthValidation.NotLongerThanBleeding(5), vm.uiState.value.cycleLengthError)
        assertFalse(vm.uiState.value.canSave)
    }
}
