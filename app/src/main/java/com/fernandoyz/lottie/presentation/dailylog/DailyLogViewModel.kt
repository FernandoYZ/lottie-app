package com.fernandoyz.lottie.presentation.dailylog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fernandoyz.lottie.data.dailylog.DailyLogRepository
import com.fernandoyz.lottie.data.history.HistoryRepository
import com.fernandoyz.lottie.data.repository.SetupRepository
import com.fernandoyz.lottie.domain.CyclePhases
import com.fernandoyz.lottie.domain.PeriodEstimate
import com.fernandoyz.lottie.domain.PeriodEstimation
import com.fernandoyz.lottie.domain.model.DailyLog
import com.fernandoyz.lottie.domain.model.MenstruationRecord
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.LocalDate

data class DailyLogUiState(
    val today: LocalDate,
    val date: LocalDate,
    val log: DailyLog = DailyLog(date),
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val writeFailed: Boolean = false,
    val records: List<MenstruationRecord> = emptyList(),
    val estimate: PeriodEstimate? = null,
) {
    /** Future days cannot be logged (DAILY_LOG §3). */
    val canGoNext: Boolean = date.isBefore(today)

    val cycleDay: Int? = CyclePhases.dayInfo(date, records, estimate, today)?.cycleDay
}

/**
 * Log screen for one day (DAILY_LOG §3). Each tap is shown at once and saved in order; when a save
 * fails the screen goes back to the last stored state and shows the error.
 */
class DailyLogViewModel(
    private val repository: DailyLogRepository,
    historyRepository: HistoryRepository,
    setupRepository: SetupRepository,
    clock: Clock,
    initialDate: LocalDate,
) : ViewModel() {

    private val today = LocalDate.now(clock)
    private val _uiState = MutableStateFlow(DailyLogUiState(today = today, date = minOf(initialDate, today)))
    val uiState: StateFlow<DailyLogUiState> = _uiState.asStateFlow()

    private val writeLock = Mutex()
    private var stored: DailyLog = _uiState.value.log
    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            try {
                combine(historyRepository.records, setupRepository.manualCycleLengthDays) { records, manual ->
                    records to PeriodEstimation.estimate(records, manual, today)
                }.collect { (records, estimate) -> _uiState.update { it.copy(records = records, estimate = estimate) } }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // The cycle day is optional: the log still works without it. Never log personal data.
            }
        }
        load(_uiState.value.date)
    }

    fun onPreviousDay() = load(_uiState.value.date.minusDays(1))

    fun onNextDay() {
        if (_uiState.value.canGoNext) load(_uiState.value.date.plusDays(1))
    }

    fun retry() = load(_uiState.value.date)

    /** Applies a chip or note change (see DailyLogEditing) and saves it. */
    fun onEdit(edit: (DailyLog) -> DailyLog) {
        val state = _uiState.value
        if (state.isLoading || state.loadFailed) return
        val updated = edit(state.log)
        _uiState.update { it.copy(log = updated, writeFailed = false) }

        viewModelScope.launch {
            writeLock.withLock {
                try {
                    repository.save(updated)
                    if (updated.date == _uiState.value.date) stored = updated
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Do not log the exception: it may reference personal data.
                    _uiState.update { if (it.date == updated.date) it.copy(log = stored, writeFailed = true) else it }
                }
            }
        }
    }

    private fun load(date: LocalDate) {
        loadJob?.cancel()
        _uiState.update { it.copy(date = date, log = DailyLog(date), isLoading = true, loadFailed = false, writeFailed = false) }
        loadJob = viewModelScope.launch {
            try {
                // Wait for pending saves so a day re-opened right away shows them.
                val log = writeLock.withLock { repository.logs.first()[date] } ?: DailyLog(date)
                stored = log
                _uiState.update { it.copy(log = log, isLoading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Do not log the exception: it may reference personal data.
                _uiState.update { it.copy(isLoading = false, loadFailed = true) }
            }
        }
    }

    companion object {
        fun factory(
            repository: DailyLogRepository,
            historyRepository: HistoryRepository,
            setupRepository: SetupRepository,
            clock: Clock,
            date: LocalDate,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { DailyLogViewModel(repository, historyRepository, setupRepository, clock, date) }
        }
    }
}
