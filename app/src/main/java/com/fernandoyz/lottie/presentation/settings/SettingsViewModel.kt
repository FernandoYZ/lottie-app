package com.fernandoyz.lottie.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fernandoyz.lottie.data.repository.SetupRepository
import com.fernandoyz.lottie.domain.CycleLengthValidation
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
    val savedCycleLengthDays: Int? = null,
    val cycleLengthInput: String = "",
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val saveFailed: Boolean = false,
) {
    private val validation: CycleLengthValidation = SetupValidation.validateCycleLength(cycleLengthInput)

    val cycleLengthError: CycleLengthValidation? = validation.takeIf { !isLoading && it !is CycleLengthValidation.Valid }

    val showReferenceRangeWarning: Boolean =
        (validation as? CycleLengthValidation.Valid)?.outsideReferenceRange == true

    val canSave: Boolean = validation is CycleLengthValidation.Valid &&
        validation.days != savedCycleLengthDays && !isLoading && !isSaving && !isSaved

    internal val validDays: Int? = (validation as? CycleLengthValidation.Valid)?.days
}

/** Edits the usual cycle length used by the estimate (STAGE_3 §4). */
class SettingsViewModel(private val repository: SetupRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val days = repository.initialSetup.first()?.usualCycleLengthDays
                _uiState.update {
                    it.copy(isLoading = false, savedCycleLengthDays = days, cycleLengthInput = days?.toString().orEmpty())
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, loadFailed = true) }
            }
        }
    }

    fun onCycleLengthChanged(input: String) {
        _uiState.update { it.copy(cycleLengthInput = input, saveFailed = false) }
    }

    fun onSave() {
        val current = _uiState.value
        if (!current.canSave) return
        val days = current.validDays ?: return

        _uiState.update { it.copy(isSaving = true, saveFailed = false) }
        viewModelScope.launch {
            try {
                repository.updateUsualCycleLength(days)
                _uiState.update { it.copy(isSaving = false, isSaved = true, savedCycleLengthDays = days) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Do not log the exception: it may reference personal data.
                _uiState.update { it.copy(isSaving = false, saveFailed = true) }
            }
        }
    }

    companion object {
        fun factory(repository: SetupRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { SettingsViewModel(repository) }
        }
    }
}
