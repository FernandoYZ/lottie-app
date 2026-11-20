package com.fernandoyz.lottie.presentation.onboarding.setup

import com.fernandoyz.lottie.MainDispatcherRule
import com.fernandoyz.lottie.data.FailingDataStore
import com.fernandoyz.lottie.data.InMemoryDataStore
import com.fernandoyz.lottie.data.repository.SetupRepository
import com.fernandoyz.lottie.domain.DayMark
import com.fernandoyz.lottie.domain.HistoryValidationError
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
import java.time.YearMonth
import java.time.ZoneId

/** Synthetic dates only; never real user data. */
class SetupViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2026, 11, 19)
    private val zone = ZoneId.of("America/Lima")
    private val clock = Clock.fixed(today.atStartOfDay(zone).plusHours(10).toInstant(), zone)
    private fun day(dayOfMonth: Int) = LocalDate.of(2026, 11, dayOfMonth)

    private val repository = SetupRepository(InMemoryDataStore())

    private fun viewModel(repo: SetupRepository = repository) = SetupViewModel(repo, clock)

    @Test
    fun `opens on the current month with nothing marked`() {
        val state = viewModel().uiState.value

        assertEquals(YearMonth.of(2026, 11), state.month)
        assertTrue(state.marks.isEmpty())
        assertFalse(state.canConfirm)
    }

    @Test
    fun `tapping the start fills six days`() {
        val vm = viewModel()

        vm.onDayTapped(day(2))

        assertEquals(day(2), vm.uiState.value.start)
        assertEquals(day(7), vm.uiState.value.end)
        assertEquals((2..7).map(::day).toSet(), vm.uiState.value.marks.keys)
        assertTrue(vm.uiState.value.canConfirm)
    }

    @Test
    fun `tapping a marked day ends the menstruation the day before`() {
        val vm = viewModel()
        vm.onDayTapped(day(2))

        vm.onDayTapped(day(6))

        assertEquals(day(5), vm.uiState.value.end)
    }

    @Test
    fun `tapping another empty day moves the start`() {
        val vm = viewModel()
        vm.onDayTapped(day(2))

        vm.onDayTapped(day(12))

        assertEquals(day(12), vm.uiState.value.start)
        assertEquals(day(17), vm.uiState.value.end)
    }

    @Test
    fun `tapping the start day clears the selection`() {
        val vm = viewModel()
        vm.onDayTapped(day(2))

        vm.onDayTapped(day(2))

        assertNull(vm.uiState.value.start)
        assertFalse(vm.uiState.value.canConfirm)
    }

    @Test
    fun `days after today are upcoming when starting today`() {
        val vm = viewModel()

        vm.onDayTapped(today)

        assertEquals(DayMark.Upcoming, vm.uiState.value.marks[day(24)])
    }

    @Test
    fun `future start is rejected`() {
        val vm = viewModel()

        vm.onDayTapped(day(20))

        assertEquals(HistoryValidationError.StartInFuture, vm.uiState.value.rejection)
        assertNull(vm.uiState.value.start)
    }

    @Test
    fun `confirm saves the marked menstruation`() = runTest {
        val vm = viewModel()
        vm.onDayTapped(day(2))
        vm.onDayTapped(day(6))

        vm.onConfirm()

        assertTrue(vm.uiState.value.isSaved)
        assertEquals(InitialSetup(day(2), day(5)), repository.initialSetup.first())
    }

    @Test
    fun `save failure is reported and not shown as saved`() = runTest {
        val vm = viewModel(SetupRepository(FailingDataStore(failWrites = true)))
        vm.onDayTapped(day(2))

        vm.onConfirm()

        assertTrue(vm.uiState.value.saveFailed)
        assertFalse(vm.uiState.value.isSaved)
    }
}
