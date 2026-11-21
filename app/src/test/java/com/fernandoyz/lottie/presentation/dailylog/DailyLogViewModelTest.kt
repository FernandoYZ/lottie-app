package com.fernandoyz.lottie.presentation.dailylog

import com.fernandoyz.lottie.MainDispatcherRule
import com.fernandoyz.lottie.data.InMemoryDataStore
import com.fernandoyz.lottie.data.dailylog.DailyLogRepository
import com.fernandoyz.lottie.data.dailylog.FakeDailyLogDao
import com.fernandoyz.lottie.data.history.FakeMenstruationRecordDao
import com.fernandoyz.lottie.data.history.HistoryRepository
import com.fernandoyz.lottie.data.repository.SetupRepository
import com.fernandoyz.lottie.domain.DailyLogEditing
import com.fernandoyz.lottie.domain.model.DailyLog
import com.fernandoyz.lottie.domain.model.MenstrualFlow
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

/** DAILY_LOG §3. Synthetic data only; never real user data. */
class DailyLogViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2026, 6, 15)
    private val zone = ZoneId.of("America/Lima")
    private val clock = Clock.fixed(today.atStartOfDay(zone).plusHours(10).toInstant(), zone)

    private val dao = FakeDailyLogDao()
    private val repository = DailyLogRepository(dao)
    private val setupRepository = SetupRepository(InMemoryDataStore())
    private val historyRepository = HistoryRepository(FakeMenstruationRecordDao(), setupRepository)

    private fun viewModel(date: LocalDate = today) =
        DailyLogViewModel(repository, historyRepository, setupRepository, clock, date)

    @Test
    fun `a day without log opens empty`() = runTest {
        val state = viewModel().uiState.value

        assertFalse(state.isLoading)
        assertEquals(DailyLog(today), state.log)
    }

    @Test
    fun `a tap is saved at once`() = runTest {
        val vm = viewModel()

        vm.onEdit { DailyLogEditing.toggleMood(it, Mood.Calm) }

        assertEquals(setOf(Mood.Calm), vm.uiState.value.log.moods)
        assertEquals("Calm", dao.rows.value.getValue(today.toEpochDay()).moods)
    }

    @Test
    fun `unmarking everything deletes the day`() = runTest {
        val vm = viewModel()

        vm.onEdit { DailyLogEditing.toggleFlow(it, MenstrualFlow.Light) }
        vm.onEdit { DailyLogEditing.toggleFlow(it, MenstrualFlow.Light) }

        assertTrue(dao.rows.value.isEmpty())
    }

    @Test
    fun `a failed save shows the error and restores the previous state`() = runTest {
        val vm = viewModel()
        dao.failWrites = true

        vm.onEdit { DailyLogEditing.toggleMood(it, Mood.Sad) }

        assertTrue(vm.uiState.value.writeFailed)
        assertEquals(emptySet<Mood>(), vm.uiState.value.log.moods)
    }

    @Test
    fun `the previous day opens its own log`() = runTest {
        val yesterday = today.minusDays(1)
        repository.save(DailyLog(yesterday, flow = MenstrualFlow.Heavy))
        val vm = viewModel()

        vm.onPreviousDay()

        assertEquals(yesterday, vm.uiState.value.date)
        assertEquals(MenstrualFlow.Heavy, vm.uiState.value.log.flow)
    }

    @Test
    fun `it never moves to a future day`() = runTest {
        val vm = viewModel()

        vm.onNextDay()

        assertEquals(today, vm.uiState.value.date)
        assertFalse(vm.uiState.value.canGoNext)
    }

    @Test
    fun `cycle day is shown when there is a menstruation`() = runTest {
        historyRepository.add(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 4), today)

        assertEquals(15, viewModel().uiState.value.cycleDay)
        assertNull(viewModel(LocalDate.of(2026, 5, 20)).uiState.value.cycleDay)
    }
}
