package com.fernandoyz.lottie.presentation.calendar

import com.fernandoyz.lottie.MainDispatcherRule
import com.fernandoyz.lottie.data.InMemoryDataStore
import com.fernandoyz.lottie.data.history.FakeMenstruationRecordDao
import com.fernandoyz.lottie.data.history.HistoryRepository
import com.fernandoyz.lottie.data.repository.SetupRepository
import com.fernandoyz.lottie.domain.DayMark
import com.fernandoyz.lottie.domain.model.InitialSetup
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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

    private val setupRepository = SetupRepository(InMemoryDataStore())
    private val historyRepository = HistoryRepository(FakeMenstruationRecordDao(), setupRepository)

    @Test
    fun `opens on the current month and navigates months`() {
        val vm = CalendarViewModel(historyRepository, setupRepository, clock)
        assertEquals(YearMonth.of(2026, 6), vm.uiState.value.month)

        vm.onNextMonth()
        assertEquals(YearMonth.of(2026, 7), vm.uiState.value.month)

        vm.onPreviousMonth()
        vm.onPreviousMonth()
        assertEquals(YearMonth.of(2026, 5), vm.uiState.value.month)
    }

    @Test
    fun `shows registered and estimated days`() = runTest {
        setupRepository.save(InitialSetup(LocalDate.of(2026, 5, 20), 28))
        historyRepository.add(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 4), today)
        val vm = CalendarViewModel(historyRepository, setupRepository, clock)

        assertEquals(DayMark.Registered, vm.uiState.value.marks[LocalDate.of(2026, 6, 2)])
        assertEquals(DayMark.Estimated, vm.uiState.value.marks[LocalDate.of(2026, 6, 29)]) // 01/06 + 28
        assertEquals(DayMark.Estimated, vm.uiState.value.marks[LocalDate.of(2026, 6, 30)])
    }
}
