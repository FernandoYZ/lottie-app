package com.fernandoyz.lottie.presentation.history

import com.fernandoyz.lottie.MainDispatcherRule
import com.fernandoyz.lottie.data.FailingDataStore
import com.fernandoyz.lottie.data.InMemoryDataStore
import com.fernandoyz.lottie.data.dailylog.DailyLogRepository
import com.fernandoyz.lottie.data.dailylog.FakeDailyLogDao
import com.fernandoyz.lottie.data.history.FakeMenstruationRecordDao
import com.fernandoyz.lottie.data.history.HistoryRepository
import com.fernandoyz.lottie.data.repository.SetupRepository
import com.fernandoyz.lottie.domain.CyclePhase
import com.fernandoyz.lottie.domain.HomeHeadline
import com.fernandoyz.lottie.domain.model.DailyLog
import com.fernandoyz.lottie.domain.model.InitialSetup
import com.fernandoyz.lottie.domain.model.Mood
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
    private val dailyLogRepository = DailyLogRepository(FakeDailyLogDao())

    private suspend fun completeSetup() = setupRepository.save(InitialSetup(setupStart, setupEnd))

    @Test
    fun `home knows when today already has a log`() = runTest {
        val vm = HistoryViewModel(repository, setupRepository, dailyLogRepository, clock)
        assertFalse(vm.uiState.value.hasTodayLog)

        dailyLogRepository.save(DailyLog(today, moods = setOf(Mood.Happy)))

        assertTrue(vm.uiState.value.hasTodayLog)
    }

    @Test
    fun `menstruation marked during setup is the first record`() = runTest {
        completeSetup()

        val state = HistoryViewModel(repository, setupRepository, dailyLogRepository, clock).uiState.value

        assertFalse(state.isLoading)
        assertEquals(setupStart, state.records.single().startDate)
        assertEquals(setupEnd, state.records.single().endDate)
    }

    @Test
    fun `records are listed newest first`() = runTest {
        repository.add(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 5), today)
        repository.add(LocalDate.of(2026, 4, 2), LocalDate.of(2026, 4, 6), today)

        val state = HistoryViewModel(repository, setupRepository, dailyLogRepository, clock).uiState.value

        assertEquals(listOf(LocalDate.of(2026, 4, 2), LocalDate.of(2026, 3, 1)), state.records.map { it.startDate })
    }

    @Test
    fun `load failure is reported`() {
        val failingRepository = HistoryRepository(dao, SetupRepository(FailingDataStore(failReads = true)))

        val state = HistoryViewModel(failingRepository, setupRepository, dailyLogRepository, clock).uiState.value

        assertTrue(state.loadFailed)
        assertFalse(state.isLoading)
    }

    @Test
    fun `a single record uses the 28 day default and is approximate`() = runTest {
        completeSetup() // 01/06 - 04/06, 4 days

        val estimate = HistoryViewModel(repository, setupRepository, dailyLogRepository, clock).uiState.value.estimate!!

        assertEquals(LocalDate.of(2026, 6, 29), estimate.start)
        assertEquals(LocalDate.of(2026, 7, 2), estimate.end)
        assertTrue(estimate.isApproximate)
    }

    @Test
    fun `estimate adapts when a second start is registered`() = runTest {
        completeSetup()
        val vm = HistoryViewModel(repository, setupRepository, dailyLogRepository, clock)

        repository.add(LocalDate.of(2026, 5, 2), LocalDate.of(2026, 5, 5), today) // 30 days before 01/06

        assertEquals(LocalDate.of(2026, 7, 1), vm.uiState.value.estimate?.start)
        assertFalse(vm.uiState.value.estimate!!.isApproximate)
    }

    @Test
    fun `estimate follows a manual cycle length`() = runTest {
        completeSetup()
        val vm = HistoryViewModel(repository, setupRepository, dailyLogRepository, clock)

        setupRepository.setManualCycleLength(30)

        assertEquals(LocalDate.of(2026, 7, 1), vm.uiState.value.estimate?.start)
    }

    @Test
    fun `today's cycle day and phase are shown`() = runTest {
        completeSetup() // 01/06 - 04/06, next start estimated 29/06, ovulation 15/06 = today

        val status = HistoryViewModel(repository, setupRepository, dailyLogRepository, clock).uiState.value.homeStatus

        assertEquals(15, status.cycleDay)
        assertEquals(CyclePhase.Ovulation, status.phase)
        assertEquals(HomeHeadline.OvulationToday, status.headline)
    }

    @Test
    fun `start button registers today filled with the period length`() = runTest {
        completeSetup() // 4 day menstruation, so the fill is 4 days
        val vm = HistoryViewModel(repository, setupRepository, dailyLogRepository, clock)

        vm.onMainActionTapped()

        val latest = vm.uiState.value.records.first()
        assertEquals(today, latest.startDate)
        assertEquals(LocalDate.of(2026, 6, 18), latest.endDate)
    }

    @Test
    fun `finish button ends the ongoing menstruation today`() = runTest {
        repository.add(LocalDate.of(2026, 6, 13), LocalDate.of(2026, 6, 18), today)
        val vm = HistoryViewModel(repository, setupRepository, dailyLogRepository, clock)

        vm.onMainActionTapped()

        assertEquals(today, vm.uiState.value.records.single().endDate)
        assertNull(vm.uiState.value.pendingOneDay)
    }

    @Test
    fun `finishing on the start day asks before saving a one day menstruation`() = runTest {
        repository.add(today, LocalDate.of(2026, 6, 20), today)
        val vm = HistoryViewModel(repository, setupRepository, dailyLogRepository, clock)

        vm.onMainActionTapped()

        assertEquals(LocalDate.of(2026, 6, 20), vm.uiState.value.records.single().endDate)
        assertEquals(today, vm.uiState.value.pendingOneDay?.startDate)

        vm.onConfirmOneDay()

        assertEquals(today, vm.uiState.value.records.single().endDate)
        assertNull(vm.uiState.value.pendingOneDay)
    }

    @Test
    fun `cancelling the one day confirmation changes nothing`() = runTest {
        repository.add(today, LocalDate.of(2026, 6, 20), today)
        val vm = HistoryViewModel(repository, setupRepository, dailyLogRepository, clock)

        vm.onMainActionTapped()
        vm.onDismissOneDay()

        assertEquals(LocalDate.of(2026, 6, 20), vm.uiState.value.records.single().endDate)
        assertNull(vm.uiState.value.pendingOneDay)
    }

    @Test
    fun `no records means no estimate`() = runTest {
        completeSetup()
        val vm = HistoryViewModel(repository, setupRepository, dailyLogRepository, clock)

        vm.uiState.value.records.forEach { repository.delete(it.id) }

        assertNull(vm.uiState.value.estimate)
    }

    @Test
    fun `home shows the current week from monday to sunday`() {
        val vm = HistoryViewModel(repository, setupRepository, dailyLogRepository, clock)

        assertEquals((15..21).map { LocalDate.of(2026, 6, it) }, vm.uiState.value.week) // 15/06/2026 is a Monday
    }

    @Test
    fun `tapping a week day opens its detail and changes nothing`() = runTest {
        repository.add(LocalDate.of(2026, 6, 13), LocalDate.of(2026, 6, 16), today)
        dailyLogRepository.save(DailyLog(LocalDate.of(2026, 6, 14), moods = setOf(Mood.Happy)))
        val vm = HistoryViewModel(repository, setupRepository, dailyLogRepository, clock)

        vm.onDayTapped(LocalDate.of(2026, 6, 14))

        assertEquals(LocalDate.of(2026, 6, 14), vm.uiState.value.selectedDay)
        assertEquals(2, vm.uiState.value.selectedDayInfo?.cycleDay)
        assertEquals(setOf(Mood.Happy), vm.uiState.value.selectedDayLog?.moods)
        assertEquals(LocalDate.of(2026, 6, 16), vm.uiState.value.records.single().endDate)

        vm.onDismissDayDetail()
        assertNull(vm.uiState.value.selectedDay)
    }

    @Test
    fun `the main action changes nothing when the menstruation already ends today`() = runTest {
        repository.add(LocalDate.of(2026, 6, 12), today, today)
        val vm = HistoryViewModel(repository, setupRepository, dailyLogRepository, clock)

        vm.onMainActionTapped()

        assertEquals(today, vm.uiState.value.records.single().endDate)
        assertNull(vm.uiState.value.pendingOneDay)
    }

    @Test
    fun `history averages come from the history only`() = runTest {
        repository.add(LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 8), today)
        repository.add(LocalDate.of(2026, 6, 3), LocalDate.of(2026, 6, 6), today)
        setupRepository.setManualCycleLength(35)
        val vm = HistoryViewModel(repository, setupRepository, dailyLogRepository, clock)

        assertEquals(30, vm.uiState.value.averageCycleLengthDays)
        assertEquals(5, vm.uiState.value.averagePeriodLengthDays) // 5 and 4 days, rounded half up
        assertEquals(2, vm.uiState.value.cycles.size)
    }
}
