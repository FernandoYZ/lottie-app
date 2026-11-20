package com.fernandoyz.lottie.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fernandoyz.lottie.data.history.HistoryRepository
import com.fernandoyz.lottie.domain.HistoryValidationError
import com.fernandoyz.lottie.domain.model.MenstruationRecord
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val markEndError: HistoryValidationError? = null,
    val markEndFailed: Boolean = false,
) {
    /** The ongoing menstruation: only the latest record may lack an end date (STAGE_2 §3). */
    val openRecord: MenstruationRecord? = records.firstOrNull()?.takeIf { it.endDate == null }
}

class HistoryViewModel(
    private val repository: HistoryRepository,
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
                repository.records.collect { records ->
                    _uiState.update { it.copy(isLoading = false, records = records) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Do not log the exception: it may reference personal data.
                _uiState.update { it.copy(isLoading = false, loadFailed = true) }
            }
        }
    }

    fun onMarkEnd(endDate: LocalDate) {
        val open = _uiState.value.openRecord ?: return
        _uiState.update { it.copy(markEndError = null, markEndFailed = false) }
        viewModelScope.launch {
            try {
                val error = repository.update(open.id, open.startDate, endDate, _uiState.value.today)
                _uiState.update { it.copy(markEndError = error) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(markEndFailed = true) }
            }
        }
    }

    companion object {
        fun factory(repository: HistoryRepository, clock: Clock): ViewModelProvider.Factory = viewModelFactory {
            initializer { HistoryViewModel(repository, clock) }
        }
    }
}
