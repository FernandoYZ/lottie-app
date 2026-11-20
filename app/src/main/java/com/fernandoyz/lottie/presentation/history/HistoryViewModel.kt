package com.fernandoyz.lottie.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fernandoyz.lottie.data.history.HistoryRepository
import com.fernandoyz.lottie.data.repository.SetupRepository
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
)

/** Home: estimate and read-only history. Records are edited in the calendar (STAGE_4 §2). */
class HistoryViewModel(
    private val repository: HistoryRepository,
    private val setupRepository: SetupRepository,
    clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState(today = LocalDate.now(clock)))
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, loadFailed = false) }
        loadJob = viewModelScope.launch {
            try {
                repository.seedInitialRecordIfNeeded()
                combine(repository.records, setupRepository.manualCycleLengthDays) { records, manual -> records to manual }
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

    companion object {
        fun factory(
            repository: HistoryRepository,
            setupRepository: SetupRepository,
            clock: Clock,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { HistoryViewModel(repository, setupRepository, clock) }
        }
    }
}
