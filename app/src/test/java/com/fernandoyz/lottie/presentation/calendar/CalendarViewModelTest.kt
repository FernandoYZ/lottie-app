package com.fernandoyz.lottie.presentation.calendar

import com.fernandoyz.lottie.MainDispatcherRule
import com.fernandoyz.lottie.data.InMemoryDataStore
import com.fernandoyz.lottie.data.history.FakeMenstruationRecordDao
import com.fernandoyz.lottie.data.history.HistoryRepository
import com.fernandoyz.lottie.data.repository.SetupRepository
import com.fernandoyz.lottie.domain.CyclePhase
import com.fernandoyz.lottie.domain.DayMark
import com.fernandoyz.lottie.domain.HistoryValidationError
import com.fernandoyz.lottie.domain.model.MenstruationRecord
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** Synthetic dates only; never real user data. */
class CalendarViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2026, 6, 15)
    private val zone = ZoneId.of("America/Lima")
    private val clock = Clock.fixed(today.atStartOfDay(zone).plusHours(10).toInstant(), zone)
    private fun day(month: Int, dayOfMonth: Int) = LocalDate.of(2026, month, dayOfMonth)

    private val dao = FakeMenstruationRecordDao()
    private val setupRepository = SetupRepository(InMemoryDataStore())
    private val historyRepository = HistoryRepository(dao, setupRepository)

    private fun viewModel() = CalendarViewModel(historyRepository, setupRepository, clock)
    private suspend fun stored(): List<MenstruationRecord> = historyRepository.records.first()

    @Test
    fun `opens on the current month and follows the shown month`() {
        val vm = viewModel()
        assertEquals(YearMonth.of(2026, 6), vm.uiState.value.month)

        vm.onMonthChanged(YearMonth.of(2026, 4))

        assertEquals(YearMonth.of(2026, 4), vm.uiState.value.month)
    }

    @Test
    fun `marks are available for any month shown while swiping`() = runTest {
        historyRepository.add(day(5, 30), day(6, 2), today)
        val vm = viewModel()

        assertEquals(DayMark.Registered, vm.uiState.value.marksFor(YearMonth.of(2026, 5))[day(5, 31)])
    }

    @Test
    fun `shows registered and estimated days with the 28 day default`() = runTest {
        historyRepository.add(day(6, 1), day(6, 4), today)
        val vm = viewModel()

        assertEquals(DayMark.Registered, vm.uiState.value.marks[day(6, 2)])
        assertEquals(DayMark.Estimated, vm.uiState.value.marks[day(6, 29)]) // 01/06 + 28
        assertEquals(DayMark.Estimated, vm.uiState.value.marks[day(6, 30)])
    }

    @Test
    fun `tapping an empty day registers six days`() = runTest {
        val vm = viewModel()

        vm.onDayTapped(day(6, 10))

        assertEquals(listOf(MenstruationRecord(1, day(6, 10), day(6, 15))), stored())
    }

    @Test
    fun `the fill adapts to the mean of finished records`() = runTest {
        historyRepository.add(day(5, 1), day(5, 4), today) // 4 days
        val vm = viewModel()

        vm.onDayTapped(day(6, 1))

        assertEquals(day(6, 4), stored().first().endDate)
    }

    @Test
    fun `tapping a marked day ends the menstruation the day before`() = runTest {
        val vm = viewModel()
        vm.onDayTapped(day(6, 10))

        vm.onDayTapped(day(6, 13))

        assertEquals(day(6, 12), stored().single().endDate)
    }

    @Test
    fun `tapping the start day deletes only after confirmation`() = runTest {
        val vm = viewModel()
        vm.onDayTapped(day(6, 10))

        vm.onDayTapped(day(6, 10))
        assertEquals(day(6, 10), vm.uiState.value.pendingDelete?.startDate)
        assertEquals(1, stored().size)

        vm.onConfirmDelete()
        assertTrue(stored().isEmpty())
        assertNull(vm.uiState.value.pendingDelete)
    }

    @Test
    fun `dismissing the delete keeps the record`() = runTest {
        val vm = viewModel()
        vm.onDayTapped(day(6, 10))
        vm.onDayTapped(day(6, 10))

        vm.onDismissDelete()

        assertNull(vm.uiState.value.pendingDelete)
        assertEquals(1, stored().size)
    }

    @Test
    fun `tapping an empty future day is rejected and stores nothing`() = runTest {
        val vm = viewModel()

        vm.onDayTapped(day(6, 20))

        assertEquals(HistoryValidationError.StartInFuture, vm.uiState.value.rejection)
        assertTrue(stored().isEmpty())
    }

    @Test
    fun `days auto-filled after today are shown as upcoming`() = runTest {
        val vm = viewModel()

        vm.onDayTapped(today)

        assertEquals(DayMark.Registered, vm.uiState.value.marks[today])
        assertEquals(DayMark.Upcoming, vm.uiState.value.marks[day(6, 20)])
    }

    @Test
    fun `long press opens the day detail with its cycle phase and changes nothing`() = runTest {
        historyRepository.add(day(5, 18), day(5, 21), today) // next start estimated 15/06, ovulation 01/06
        val vm = viewModel()

        vm.onDayLongPressed(day(6, 1))

        assertEquals(day(6, 1), vm.uiState.value.selectedDay)
        assertEquals(CyclePhase.Ovulation, vm.uiState.value.selectedDayInfo?.phase)
        assertEquals(setOf(day(6, 1)), vm.uiState.value.ovulationDaysFor(YearMonth.of(2026, 6)))
        assertEquals(1, stored().size)

        vm.onDismissDayDetail()
        assertNull(vm.uiState.value.selectedDay)
    }

    @Test
    fun `write failure is reported`() = runTest {
        val vm = viewModel()
        dao.failWrites = true

        vm.onDayTapped(day(6, 10))

        assertTrue(vm.uiState.value.writeFailed)
    }
}
