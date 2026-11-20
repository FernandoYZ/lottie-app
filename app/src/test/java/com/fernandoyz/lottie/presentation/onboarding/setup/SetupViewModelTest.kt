package com.fernandoyz.lottie.presentation.onboarding.setup

import com.fernandoyz.lottie.MainDispatcherRule
import com.fernandoyz.lottie.data.FailingDataStore
import com.fernandoyz.lottie.data.InMemoryDataStore
import com.fernandoyz.lottie.data.history.FakeMenstruationRecordDao
import com.fernandoyz.lottie.data.history.HistoryRepository
import com.fernandoyz.lottie.data.repository.SetupRepository
import com.fernandoyz.lottie.domain.DayMark
import com.fernandoyz.lottie.domain.HistoryValidationError
import com.fernandoyz.lottie.domain.model.InitialSetup
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    private fun day(month: Int, dayOfMonth: Int) = LocalDate.of(2026, month, dayOfMonth)
    private fun day(dayOfMonth: Int) = day(11, dayOfMonth)

    private val setupRepository = SetupRepository(InMemoryDataStore())
    private val historyRepository = HistoryRepository(FakeMenstruationRecordDao(), setupRepository)

    private fun viewModel(history: HistoryRepository = historyRepository) = SetupViewModel(history, clock)
    private fun ranges(vm: SetupViewModel) = vm.uiState.value.menstruations.map { it.startDate to it.endDate }

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

        assertEquals(listOf(day(2) to day(7)), ranges(vm))
        assertEquals((2..7).map(::day).toSet(), vm.uiState.value.marks.keys)
        assertTrue(vm.uiState.value.canConfirm)
    }

    @Test
    fun `tapping a marked day ends the menstruation the day before`() {
        val vm = viewModel()
        vm.onDayTapped(day(2))

        vm.onDayTapped(day(6))

        assertEquals(listOf(day(2) to day(5)), ranges(vm))
    }

    @Test
    fun `several menstruations can be marked and the fill adapts to them`() {
        val vm = viewModel()
        vm.onDayTapped(day(10, 5))
        vm.onDayTapped(day(10, 9)) // 05/10 - 08/10: 4 days

        vm.onMonthChanged(YearMonth.of(2026, 11))
        vm.onDayTapped(day(2))

        assertEquals(listOf(day(10, 5) to day(10, 8), day(2) to day(5)), ranges(vm).sortedBy { it.first })
    }

    @Test
    fun `tapping a start day removes only that menstruation`() {
        val vm = viewModel()
        vm.onDayTapped(day(10, 5))
        vm.onDayTapped(day(2))

        vm.onDayTapped(day(2))

        assertEquals(listOf(day(10, 5) to day(10, 10)), ranges(vm))
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
        assertTrue(vm.uiState.value.menstruations.isEmpty())
    }

    @Test
    fun `confirm saves every marked menstruation to the history`() = runTest {
        val vm = viewModel()
        vm.onDayTapped(day(10, 5))
        vm.onDayTapped(day(2))
        vm.onDayTapped(day(6))

        vm.onConfirm()

        assertTrue(vm.uiState.value.isSaved)
        assertEquals(listOf(day(2), day(10, 5)), historyRepository.records.first().map { it.startDate })
        assertEquals(InitialSetup(day(2), day(5)), setupRepository.initialSetup.first())
    }

    @Test
    fun `save failure is reported and not shown as saved`() = runTest {
        val failing = SetupRepository(FailingDataStore(failWrites = true))
        val vm = viewModel(HistoryRepository(FakeMenstruationRecordDao(), failing))
        vm.onDayTapped(day(2))

        vm.onConfirm()

        assertTrue(vm.uiState.value.saveFailed)
        assertFalse(vm.uiState.value.isSaved)
    }
}
