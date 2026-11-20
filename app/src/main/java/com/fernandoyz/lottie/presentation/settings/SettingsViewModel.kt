package com.fernandoyz.lottie.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fernandoyz.lottie.data.history.HistoryRepository
import com.fernandoyz.lottie.data.repository.SetupRepository
import com.fernandoyz.lottie.domain.CycleLengthValidation
import com.fernandoyz.lottie.domain.CycleStatistics
import com.fernandoyz.lottie.domain.SetupValidation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    /** Saved manual cycle length; null means it is learned from the history (STAGE_4 §3). */
    val savedManualDays: Int? = null,
    /** Cycle length used when there is no manual value: history mean, or 28 by default. */
    val learnedCycleLengthDays: Int = CycleStatistics.DEFAULT_CYCLE_LENGTH_DAYS,
    val learnedIsDefault: Boolean = true,
    /** Longest recorded menstruation; the cycle length must be greater (approved 2026-11-20). */
    val longestBleedingDays: Int? = null,
    val cycleLengthInput: String = "",
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val saveFailed: Boolean = false,
) {
    /** Null when the field is empty: the cycle length is learned automatically. */
    private val validation: CycleLengthValidation? =
        if (cycleLengthInput.isBlank()) null else SetupValidation.validateCycleLength(cycleLengthInput, longestBleedingDays)

    val cycleLengthError: CycleLengthValidation? = validation.takeIf { !isLoading && it !is CycleLengthValidation.Valid }

    val showReferenceRangeWarning: Boolean =
        (validation as? CycleLengthValidation.Valid)?.outsideReferenceRange == true

    internal val newManualDays: Int? = (validation as? CycleLengthValidation.Valid)?.days

    val canSave: Boolean = cycleLengthError == null &&
        newManualDays != savedManualDays && !isLoading && !isSaving && !isSaved
}

/** Optional manual cycle length used by the estimate instead of the learned one (STAGE_4 §3). */
class SettingsViewModel(
    private val repository: SetupRepository,
    private val historyRepository: HistoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val manual = repository.manualCycleLengthDays.first()
                _uiState.update {
                    it.copy(savedManualDays = manual, cycleLengthInput = manual?.toString().orEmpty())
                }
                // As a tab the screen stays alive: the learned value follows the history.
                historyRepository.records.collect { records ->
                    val learned = CycleStatistics.averageCycleLengthDays(records)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            learnedCycleLengthDays = learned ?: CycleStatistics.DEFAULT_CYCLE_LENGTH_DAYS,
                            learnedIsDefault = learned == null,
                            longestBleedingDays = records.mapNotNull { r -> r.durationDays }.maxOrNull(),
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, loadFailed = true) }
            }
        }
    }

    fun onCycleLengthChanged(input: String) {
        _uiState.update { it.copy(cycleLengthInput = input, saveFailed = false, isSaved = false) }
    }

    fun onSave() {
        val current = _uiState.value
        if (!current.canSave) return
        val days = current.newManualDays

        _uiState.update { it.copy(isSaving = true, saveFailed = false) }
        viewModelScope.launch {
            try {
                repository.setManualCycleLength(days)
                _uiState.update { it.copy(isSaving = false, isSaved = true, savedManualDays = days) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Do not log the exception: it may reference personal data.
                _uiState.update { it.copy(isSaving = false, saveFailed = true) }
            }
        }
    }

    companion object {
        fun factory(repository: SetupRepository, historyRepository: HistoryRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { SettingsViewModel(repository, historyRepository) }
            }
    }
}
