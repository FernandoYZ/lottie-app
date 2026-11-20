package com.fernandoyz.lottie.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fernandoyz.lottie.data.history.HistoryRepository
import com.fernandoyz.lottie.data.repository.SetupRepository
import com.fernandoyz.lottie.domain.CalendarMarks
import com.fernandoyz.lottie.domain.DayMark
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
) {
    val marks: Map<LocalDate, DayMark> = CalendarMarks.marksFor(month, records, estimate, today)
}

/** Read-only month calendar (STAGE_3 §3). */
class CalendarViewModel(
    historyRepository: HistoryRepository,
    setupRepository: SetupRepository,
    clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState(today = LocalDate.now(clock)))
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                combine(historyRepository.records, setupRepository.initialSetup) { records, setup -> records to setup }
                    .collect { (records, setup) ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                records = records,
                                estimate = setup?.let { s -> PeriodEstimation.estimate(records, s.usualCycleLengthDays, it.today) },
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

    fun onPreviousMonth() = _uiState.update { it.copy(month = it.month.minusMonths(1)) }

    fun onNextMonth() = _uiState.update { it.copy(month = it.month.plusMonths(1)) }

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
