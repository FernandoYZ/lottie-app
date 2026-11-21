package com.fernandoyz.lottie.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fernandoyz.lottie.data.dailylog.DailyLogRepository
import com.fernandoyz.lottie.data.history.HistoryRepository
import com.fernandoyz.lottie.data.repository.SetupRepository
import com.fernandoyz.lottie.domain.CalendarEditing
import com.fernandoyz.lottie.domain.CycleStatistics
import com.fernandoyz.lottie.domain.DayAction
import com.fernandoyz.lottie.domain.HistoryValidationError
import com.fernandoyz.lottie.domain.HomeAction
import com.fernandoyz.lottie.domain.HomeStatus
import com.fernandoyz.lottie.domain.PeriodEstimate
import com.fernandoyz.lottie.domain.PeriodEstimation
import com.fernandoyz.lottie.domain.model.MenstruationRecord
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

data class HistoryUiState(
    val today: LocalDate,
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    /** Newest first. */
    val records: List<MenstruationRecord> = emptyList(),
    /** Null without records (STAGE_3 §3). */
    val estimate: PeriodEstimate? = null,
    /** Menstruation that "Terminó mi menstruación" would leave at one day, waiting for confirmation (HOME §5). */
    val pendingOneDay: MenstruationRecord? = null,
    /** Today already has a daily log: Home says "Editar registro de hoy" (DAILY_LOG §5). */
    val hasTodayLog: Boolean = false,
    val isWriting: Boolean = false,
    val writeFailed: Boolean = false,
    val rejection: HistoryValidationError? = null,
) {
    /** Ring, headline, phase and main action of Home (HOME §2-§5). */
    val homeStatus: HomeStatus = HomeStatus.of(records, estimate, today)
}

/** Home and History: estimate, history and the Home main action (HOME §5). Other edits happen in the calendar. */
class HistoryViewModel(
    private val repository: HistoryRepository,
    private val setupRepository: SetupRepository,
    private val dailyLogRepository: DailyLogRepository,
    clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState(today = LocalDate.now(clock)))
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun retry() = load()

    /** "Empezó mi menstruación" taps today on the calendar; "Terminó mi menstruación" ends it today (HOME §5). */
    fun onMainActionTapped() {
        val state = _uiState.value
        if (state.isLoading || state.loadFailed || state.isWriting || state.pendingOneDay != null) return
        _uiState.update { it.copy(writeFailed = false, rejection = null) }

        when (val action = state.homeStatus.action) {
            is HomeAction.Finish ->
                if (action.record.startDate == state.today) {
                    _uiState.update { it.copy(pendingOneDay = action.record) }
                } else {
                    finish(action.record)
                }

            HomeAction.Start -> {
                val periodLength = CycleStatistics.periodLengthDays(state.records, state.today)
                when (val day = CalendarEditing.actionFor(state.today, state.records, state.today, periodLength)) {
                    is DayAction.Add -> write { repository.add(day.start, day.end, state.today) }
                    is DayAction.ChangeEnd -> write { repository.update(day.record.id, day.record.startDate, day.newEnd, state.today) }
                    // With the Start action today is never inside a record nor in the future.
                    is DayAction.Delete, DayAction.StartInFuture -> Unit
                }
            }
        }
    }

    fun onConfirmOneDay() {
        val record = _uiState.value.pendingOneDay ?: return
        _uiState.update { it.copy(pendingOneDay = null) }
        finish(record)
    }

    fun onDismissOneDay() = _uiState.update { it.copy(pendingOneDay = null) }

    private fun finish(record: MenstruationRecord) {
        val today = _uiState.value.today
        write { repository.update(record.id, record.startDate, today, today) }
    }

    private fun write(block: suspend () -> HistoryValidationError?) {
        _uiState.update { it.copy(isWriting = true) }
        viewModelScope.launch {
            try {
                val error = block()
                _uiState.update { it.copy(isWriting = false, rejection = error) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Do not log the exception: it may reference personal data.
                _uiState.update { it.copy(isWriting = false, writeFailed = true) }
            }
        }
    }

    private fun load() {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, loadFailed = false) }
        loadJob = viewModelScope.launch {
            try {
                repository.seedInitialRecordIfNeeded()
                combine(
                    repository.records,
                    setupRepository.manualCycleLengthDays,
                    dailyLogRepository.logs,
                ) { records, manual, logs -> Triple(records, manual, logs) }
                    .collect { (records, manual, logs) ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                records = records,
                                estimate = PeriodEstimation.estimate(records, manual, it.today),
                                hasTodayLog = it.today in logs,
                            )
                        }
                    }
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
            repository: HistoryRepository,
            setupRepository: SetupRepository,
            dailyLogRepository: DailyLogRepository,
            clock: Clock,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { HistoryViewModel(repository, setupRepository, dailyLogRepository, clock) }
        }
    }
}
