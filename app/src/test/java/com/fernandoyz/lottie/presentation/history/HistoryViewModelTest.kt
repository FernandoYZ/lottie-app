package com.fernandoyz.lottie.presentation.history

import com.fernandoyz.lottie.MainDispatcherRule
import com.fernandoyz.lottie.data.FailingDataStore
import com.fernandoyz.lottie.data.InMemoryDataStore
import com.fernandoyz.lottie.data.history.FakeMenstruationRecordDao
import com.fernandoyz.lottie.data.history.HistoryRepository
import com.fernandoyz.lottie.data.repository.SetupRepository
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
    private val setupStart = LocalDate.of(2026, 6, 1)
    private val setupEnd = LocalDate.of(2026, 6, 4)

    private val dao = FakeMenstruationRecordDao()
    private val setupRepository = SetupRepository(InMemoryDataStore())
    private val repository = HistoryRepository(dao, setupRepository)

    private suspend fun completeSetup() = setupRepository.save(InitialSetup(setupStart, setupEnd))

    @Test
    fun `menstruation marked during setup is the first record`() = runTest {
        completeSetup()

        val state = HistoryViewModel(repository, setupRepository, clock).uiState.value

        assertFalse(state.isLoading)
        assertEquals(setupStart, state.records.single().startDate)
        assertEquals(setupEnd, state.records.single().endDate)
    }

    @Test
    fun `records are listed newest first`() = runTest {
        repository.add(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 5), today)
        repository.add(LocalDate.of(2026, 4, 2), LocalDate.of(2026, 4, 6), today)

        val state = HistoryViewModel(repository, setupRepository, clock).uiState.value

        assertEquals(listOf(LocalDate.of(2026, 4, 2), LocalDate.of(2026, 3, 1)), state.records.map { it.startDate })
    }

    @Test
    fun `load failure is reported`() {
        val failingRepository = HistoryRepository(dao, SetupRepository(FailingDataStore(failReads = true)))

        val state = HistoryViewModel(failingRepository, setupRepository, clock).uiState.value

        assertTrue(state.loadFailed)
        assertFalse(state.isLoading)
    }

    @Test
    fun `a single record uses the 28 day default and is approximate`() = runTest {
        completeSetup() // 01/06 - 04/06, 4 days

        val estimate = HistoryViewModel(repository, setupRepository, clock).uiState.value.estimate!!

        assertEquals(LocalDate.of(2026, 6, 29), estimate.start)
        assertEquals(LocalDate.of(2026, 7, 2), estimate.end)
        assertTrue(estimate.isApproximate)
    }

    @Test
    fun `estimate adapts when a second start is registered`() = runTest {
        completeSetup()
        val vm = HistoryViewModel(repository, setupRepository, clock)

        repository.add(LocalDate.of(2026, 5, 2), LocalDate.of(2026, 5, 5), today) // 30 days before 01/06

        assertEquals(LocalDate.of(2026, 7, 1), vm.uiState.value.estimate?.start)
        assertFalse(vm.uiState.value.estimate!!.isApproximate)
    }

    @Test
    fun `estimate follows a manual cycle length`() = runTest {
        completeSetup()
        val vm = HistoryViewModel(repository, setupRepository, clock)

        setupRepository.setManualCycleLength(30)

        assertEquals(LocalDate.of(2026, 7, 1), vm.uiState.value.estimate?.start)
    }

    @Test
    fun `no records means no estimate`() = runTest {
        completeSetup()
        val vm = HistoryViewModel(repository, setupRepository, clock)

        vm.uiState.value.records.forEach { repository.delete(it.id) }

        assertNull(vm.uiState.value.estimate)
    }
}
