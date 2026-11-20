package com.fernandoyz.lottie.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fernandoyz.lottie.data.history.HistoryRepository
import com.fernandoyz.lottie.domain.HistoryValidation
import com.fernandoyz.lottie.domain.HistoryValidationError
import com.fernandoyz.lottie.domain.model.MenstruationRecord
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

data class RecordFormUiState(
    val today: LocalDate,
    /** Null when creating a new record. */
    val recordId: Long? = null,
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val records: List<MenstruationRecord> = emptyList(),
    /** Rule reported by the repository at save time, e.g. if the history changed meanwhile. */
    val rejection: HistoryValidationError? = null,
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false,
    val deleteFailed: Boolean = false,
    val isDone: Boolean = false,
) {
    val isEditing: Boolean = recordId != null

    /** Not computed once done: the saved record is already in [records] and would overlap itself. */
    val validationError: HistoryValidationError? =
        if (isDone) null else startDate?.let { HistoryValidation.validate(it, endDate, records, today, recordId) } ?: rejection

    val canSave: Boolean =
        startDate != null && validationError == null && !isLoading && !isSaving && !isDone
}

class RecordFormViewModel(
    private val repository: HistoryRepository,
    clock: Clock,
    recordId: Long?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordFormUiState(today = LocalDate.now(clock), recordId = recordId))
    val uiState: StateFlow<RecordFormUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                if (recordId != null) {
                    val record = repository.getById(recordId)
                    if (record == null) {
                        _uiState.update { it.copy(isLoading = false, loadFailed = true) }
                        return@launch
                    }
                    _uiState.update { it.copy(startDate = record.startDate, endDate = record.endDate) }
                }
                repository.records.collect { records ->
                    _uiState.update { it.copy(isLoading = false, records = records) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, loadFailed = true) }
            }
        }
    }

    fun onStartSelected(date: LocalDate) = edit { it.copy(startDate = date) }

    fun onEndSelected(date: LocalDate) = edit { it.copy(endDate = date) }

    fun onEndCleared() = edit { it.copy(endDate = null) }

    private fun edit(change: (RecordFormUiState) -> RecordFormUiState) {
        _uiState.update { change(it).copy(rejection = null, saveFailed = false, deleteFailed = false) }
    }

    fun onSave() {
        val current = _uiState.value
        if (!current.canSave) return
        val start = current.startDate ?: return

        _uiState.update { it.copy(isSaving = true, saveFailed = false) }
        viewModelScope.launch {
            try {
                val id = current.recordId
                val error = if (id != null) {
                    repository.update(id, start, current.endDate, current.today)
                } else {
                    repository.add(start, current.endDate, current.today)
                }
                _uiState.update { it.copy(isSaving = false, rejection = error, isDone = error == null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Do not log the exception: it may reference personal data.
                _uiState.update { it.copy(isSaving = false, saveFailed = true) }
            }
        }
    }

    fun onDelete() {
        val id = _uiState.value.recordId ?: return
        viewModelScope.launch {
            try {
                repository.delete(id)
                _uiState.update { it.copy(isDone = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(deleteFailed = true) }
            }
        }
    }

    companion object {
        fun factory(repository: HistoryRepository, clock: Clock, recordId: Long?): ViewModelProvider.Factory =
            viewModelFactory { initializer { RecordFormViewModel(repository, clock, recordId) } }
    }
}
