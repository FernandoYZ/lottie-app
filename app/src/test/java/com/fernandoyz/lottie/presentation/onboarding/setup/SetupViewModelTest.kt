package com.fernandoyz.lottie.presentation.onboarding.setup

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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

class SetupViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2026, 11, 19)
    private val zone = ZoneId.of("America/Lima")
    private val clock = Clock.fixed(today.atStartOfDay(zone).plusHours(10).toInstant(), zone)

    private val repository = SetupRepository(InMemoryDataStore())

    private fun viewModel(repo: SetupRepository = repository) = SetupViewModel(repo, clock)

    @Test
    fun `today comes from the injected clock`() {
        assertEquals(today, viewModel().uiState.value.today)
    }

    @Test
    fun `empty form cannot be confirmed`() {
        val state = viewModel().uiState.value

        assertFalse(state.canConfirm)
        assertNull(state.cycleLengthError)
        assertFalse(state.isDateInFuture)
    }

    @Test
    fun `valid date and cycle length can be confirmed`() {
        val vm = viewModel()

        vm.onDateSelected(today)
        vm.onCycleLengthChanged("28")

        assertTrue(vm.uiState.value.canConfirm)
        assertFalse(vm.uiState.value.showReferenceRangeWarning)
    }

    @Test
    fun `future date is flagged and cannot be confirmed`() {
        val vm = viewModel()

        vm.onDateSelected(today.plusDays(1))
        vm.onCycleLengthChanged("28")

        assertTrue(vm.uiState.value.isDateInFuture)
        assertFalse(vm.uiState.value.canConfirm)
    }

    @Test
    fun `missing date cannot be confirmed`() {
        val vm = viewModel()

        vm.onCycleLengthChanged("28")

        assertFalse(vm.uiState.value.canConfirm)
    }

    @Test
    fun `empty or non numeric cycle length shows error and cannot be confirmed`() {
        val vm = viewModel()
        vm.onDateSelected(today)

        vm.onCycleLengthChanged("abc")
        assertEquals(CycleLengthValidation.NotAWholeNumber, vm.uiState.value.cycleLengthError)
        assertFalse(vm.uiState.value.canConfirm)

        vm.onCycleLengthChanged("")
        assertEquals(CycleLengthValidation.Empty, vm.uiState.value.cycleLengthError)
        assertFalse(vm.uiState.value.canConfirm)
    }

    @Test
    fun `cycle length outside technical limit shows error and cannot be confirmed`() {
        val vm = viewModel()
        vm.onDateSelected(today)

        vm.onCycleLengthChanged("100")

        assertEquals(CycleLengthValidation.OutOfRange, vm.uiState.value.cycleLengthError)
        assertFalse(vm.uiState.value.canConfirm)
    }

    @Test
    fun `cycle length outside reference range shows warning and can be confirmed`() {
        val vm = viewModel()
        vm.onDateSelected(today)

        vm.onCycleLengthChanged("50")

        assertNull(vm.uiState.value.cycleLengthError)
        assertTrue(vm.uiState.value.showReferenceRangeWarning)
        assertTrue(vm.uiState.value.canConfirm)
    }

    @Test
    fun `confirm saves the setup and reports success`() = runTest {
        val vm = viewModel()
        vm.onDateSelected(today.minusDays(5))
        vm.onCycleLengthChanged("30")

        vm.onConfirm()

        assertTrue(vm.uiState.value.isSaved)
        assertFalse(vm.uiState.value.saveFailed)
        assertEquals(InitialSetup(today.minusDays(5), 30), repository.initialSetup.first())
    }

    @Test
    fun `confirm with invalid form does not save`() = runTest {
        val vm = viewModel()
        vm.onCycleLengthChanged("30")

        vm.onConfirm()

        assertFalse(vm.uiState.value.isSaved)
        assertNull(repository.initialSetup.first())
    }

    @Test
    fun `save failure is reported and never shown as success`() {
        val vm = viewModel(SetupRepository(FailingDataStore(failWrites = true)))
        vm.onDateSelected(today)
        vm.onCycleLengthChanged("28")

        vm.onConfirm()

        assertTrue(vm.uiState.value.saveFailed)
        assertFalse(vm.uiState.value.isSaved)
        assertFalse(vm.uiState.value.isSaving)
        assertTrue(vm.uiState.value.canConfirm)
    }

    @Test
    fun `optional end date is saved with the setup`() = runTest {
        val vm = viewModel()
        vm.onDateSelected(today.minusDays(5))
        vm.onEndDateSelected(today.minusDays(2))
        vm.onCycleLengthChanged("30")

        vm.onConfirm()

        assertEquals(InitialSetup(today.minusDays(5), 30, today.minusDays(2)), repository.initialSetup.first())
    }

    @Test
    fun `end date before start is flagged and cannot be confirmed`() {
        val vm = viewModel()
        vm.onDateSelected(today.minusDays(2))
        vm.onEndDateSelected(today.minusDays(5))
        vm.onCycleLengthChanged("28")

        assertEquals(EndDateError.BeforeStart, vm.uiState.value.endDateError)
        assertFalse(vm.uiState.value.canConfirm)
    }

    @Test
    fun `end date in the future is flagged and cannot be confirmed`() {
        val vm = viewModel()
        vm.onDateSelected(today)
        vm.onEndDateSelected(today.plusDays(1))
        vm.onCycleLengthChanged("28")

        assertEquals(EndDateError.InFuture, vm.uiState.value.endDateError)
        assertFalse(vm.uiState.value.canConfirm)
    }

    @Test
    fun `end date can be cleared`() {
        val vm = viewModel()
        vm.onDateSelected(today)
        vm.onEndDateSelected(today.plusDays(1))
        vm.onCycleLengthChanged("28")

        vm.onEndDateCleared()

        assertNull(vm.uiState.value.endDateError)
        assertTrue(vm.uiState.value.canConfirm)
    }
}
