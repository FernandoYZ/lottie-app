package com.fernandoyz.lottie.presentation.history

import com.fernandoyz.lottie.MainDispatcherRule
import com.fernandoyz.lottie.data.FailingDataStore
import com.fernandoyz.lottie.data.InMemoryDataStore
import com.fernandoyz.lottie.data.history.FakeMenstruationRecordDao
import com.fernandoyz.lottie.data.history.HistoryRepository
import com.fernandoyz.lottie.data.repository.SetupRepository
import com.fernandoyz.lottie.domain.HistoryValidationError
import com.fernandoyz.lottie.domain.model.InitialSetup
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

/** Synthetic dates only; never real user data. */
class HistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2026, 6, 15)
    private val zone = ZoneId.of("America/Lima")
    private val clock = Clock.fixed(today.atStartOfDay(zone).plusHours(10).toInstant(), zone)
    private val setupDate = LocalDate.of(2026, 6, 12)

    private val dao = FakeMenstruationRecordDao()
    private val setupRepository = SetupRepository(InMemoryDataStore())
    private val repository = HistoryRepository(dao, setupRepository)

    private suspend fun completeSetup() = setupRepository.save(InitialSetup(setupDate, 28))

    @Test
    fun `onboarding date is shown as the open record`() = runTest {
        completeSetup()

        val state = HistoryViewModel(repository, setupRepository, clock).uiState.value

        assertFalse(state.isLoading)
        assertEquals(setupDate, state.openRecord?.startDate)
        assertEquals(1, state.records.size)
    }

    @Test
    fun `records are listed newest first`() = runTest {
        repository.add(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 5), today)
        repository.add(LocalDate.of(2026, 4, 2), LocalDate.of(2026, 4, 6), today)

        val state = HistoryViewModel(repository, setupRepository, clock).uiState.value

        assertEquals(listOf(LocalDate.of(2026, 4, 2), LocalDate.of(2026, 3, 1)), state.records.map { it.startDate })
        assertNull(state.openRecord)
    }

    @Test
    fun `mark end closes the open record`() = runTest {
        completeSetup()
        val vm = HistoryViewModel(repository, setupRepository, clock)

        vm.onMarkEnd(LocalDate.of(2026, 6, 15))

        assertNull(vm.uiState.value.openRecord)
        assertEquals(4, vm.uiState.value.records.single().durationDays)
    }

    @Test
    fun `mark end with an invalid date shows the broken rule`() = runTest {
        completeSetup()
        val vm = HistoryViewModel(repository, setupRepository, clock)

        vm.onMarkEnd(LocalDate.of(2026, 6, 10))

        assertEquals(HistoryValidationError.EndBeforeStart, vm.uiState.value.markEndError)
        assertEquals(setupDate, vm.uiState.value.openRecord?.startDate)
    }

    @Test
    fun `mark end write failure is reported`() = runTest {
        completeSetup()
        val vm = HistoryViewModel(repository, setupRepository, clock)
        dao.failWrites = true

        vm.onMarkEnd(today)

        assertTrue(vm.uiState.value.markEndFailed)
        assertEquals(setupDate, vm.uiState.value.openRecord?.startDate)
    }

    @Test
    fun `load failure is reported`() {
        val failingRepository = HistoryRepository(dao, SetupRepository(FailingDataStore(failReads = true)))

        val state = HistoryViewModel(failingRepository, setupRepository, clock).uiState.value

        assertTrue(state.loadFailed)
        assertFalse(state.isLoading)
    }

    @Test
    fun `estimate uses latest start plus declared cycle length`() = runTest {
        completeSetup() // 12/06, cycle 28, open record

        val estimate = HistoryViewModel(repository, setupRepository, clock).uiState.value.estimate

        assertEquals(LocalDate.of(2026, 7, 10), estimate?.start)
        assertNull(estimate?.end) // no record with end yet
        assertFalse(estimate!!.isPast)
    }

    @Test
    fun `estimate follows a changed cycle length`() = runTest {
        completeSetup()
        val vm = HistoryViewModel(repository, setupRepository, clock)

        setupRepository.updateUsualCycleLength(30)

        assertEquals(LocalDate.of(2026, 7, 12), vm.uiState.value.estimate?.start)
    }

    @Test
    fun `estimate gets an end after marking the end`() = runTest {
        completeSetup()
        val vm = HistoryViewModel(repository, setupRepository, clock)

        vm.onMarkEnd(LocalDate.of(2026, 6, 15)) // 4 days

        assertEquals(LocalDate.of(2026, 7, 13), vm.uiState.value.estimate?.end)
    }

    @Test
    fun `no records means no estimate`() = runTest {
        completeSetup()
        val vm = HistoryViewModel(repository, setupRepository, clock)

        vm.uiState.value.records.forEach { repository.delete(it.id) }

        assertNull(vm.uiState.value.estimate)
    }
}
