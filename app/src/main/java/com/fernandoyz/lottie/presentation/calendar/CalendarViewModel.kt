package com.fernandoyz.lottie.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fernandoyz.lottie.data.history.HistoryRepository
import com.fernandoyz.lottie.data.repository.SetupRepository
import com.fernandoyz.lottie.domain.CalendarEditing
import com.fernandoyz.lottie.domain.CalendarMarks
import com.fernandoyz.lottie.domain.CycleDayInfo
import com.fernandoyz.lottie.domain.CyclePhases
import com.fernandoyz.lottie.domain.CycleStatistics
import com.fernandoyz.lottie.domain.DayAction
import com.fernandoyz.lottie.domain.DayMark
import com.fernandoyz.lottie.domain.HistoryValidationError
import com.fernandoyz.lottie.domain.PeriodEstimate
import com.fernandoyz.lottie.domain.PeriodEstimation
import com.fernandoyz.lottie.domain.model.MenstruationRecord
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth

data class CalendarUiState(
    val today: LocalDate,
    val month: YearMonth = YearMonth.from(today),
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val records: List<MenstruationRecord> = emptyList(),
    val estimate: PeriodEstimate? = null,
    /** Record waiting for delete confirmation after its start day was tapped. */
    val pendingDelete: MenstruationRecord? = null,
    val rejection: HistoryValidationError? = null,
    val writeFailed: Boolean = false,
    val isWriting: Boolean = false,
    /** Day whose detail panel is open after a long press (day-detail S1). */
    val selectedDay: LocalDate? = null,
) {
    val selectedDayInfo: CycleDayInfo? = selectedDay?.let { CyclePhases.dayInfo(it, records, estimate, today) }

    fun ovulationDaysFor(month: YearMonth): Set<LocalDate> = CyclePhases.ovulationDays(month, records, estimate, today)

    /** Marks for any month, so the neighbour pages show them while swiping. */
    fun marksFor(month: YearMonth): Map<LocalDate, DayMark> = CalendarMarks.marksFor(month, records, estimate, today)

    val marks: Map<LocalDate, DayMark> = marksFor(month)

    /** Days filled when a start is tapped: adapts to the history, 6 by default (STAGE_4 §3). */
    val periodLengthDays: Int = CycleStatistics.periodLengthDays(records, today)
}

/** Month calendar where menstruations are registered by tapping days (STAGE_4 §2). */
class CalendarViewModel(
    private val historyRepository: HistoryRepository,
    setupRepository: SetupRepository,
    clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState(today = LocalDate.now(clock)))
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                combine(historyRepository.records, setupRepository.manualCycleLengthDays) { records, manual -> records to manual }
                    .collect { (records, manual) ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                records = records,
                                estimate = PeriodEstimation.estimate(records, manual, it.today),
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

    fun onMonthChanged(month: YearMonth) = _uiState.update { it.copy(month = month) }

    fun onDayTapped(date: LocalDate) {
        val state = _uiState.value
        if (state.isLoading || state.loadFailed || state.isWriting || state.pendingDelete != null) return
        _uiState.update { it.copy(rejection = null, writeFailed = false) }

        when (val action = CalendarEditing.actionFor(date, state.records, state.today, state.periodLengthDays)) {
            is DayAction.Add -> write { historyRepository.add(action.start, action.end, state.today) }
            is DayAction.ChangeEnd -> write {
                historyRepository.update(action.record.id, action.record.startDate, action.newEnd, state.today)
            }
            is DayAction.Delete -> _uiState.update { it.copy(pendingDelete = action.record) }
            DayAction.StartInFuture -> _uiState.update { it.copy(rejection = HistoryValidationError.StartInFuture) }
        }
    }

    fun onDayLongPressed(date: LocalDate) {
        if (_uiState.value.isLoading || _uiState.value.loadFailed) return
        _uiState.update { it.copy(selectedDay = date) }
    }

    fun onDismissDayDetail() = _uiState.update { it.copy(selectedDay = null) }

    fun onConfirmDelete() {
        val record = _uiState.value.pendingDelete ?: return
        _uiState.update { it.copy(pendingDelete = null) }
        write {
            historyRepository.delete(record.id)
            null
        }
    }

    fun onDismissDelete() = _uiState.update { it.copy(pendingDelete = null) }

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

    companion object {
        fun factory(
            historyRepository: HistoryRepository,
            setupRepository: SetupRepository,
            clock: Clock,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { CalendarViewModel(historyRepository, setupRepository, clock) }
        }
    }
}
