package com.fernandoyz.lottie.presentation.onboarding.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fernandoyz.lottie.data.repository.SetupRepository
import com.fernandoyz.lottie.domain.CycleLengthValidation
import com.fernandoyz.lottie.domain.SetupValidation
import com.fernandoyz.lottie.domain.model.InitialSetup
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

enum class EndDateError { BeforeStart, InFuture }

data class SetupUiState(
    val today: LocalDate,
    val lastMenstruationDate: LocalDate? = null,
    /** Optional end of the last menstruation. */
    val lastMenstruationEndDate: LocalDate? = null,
    val cycleLengthInput: String = "",
    val cycleLengthEdited: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val saveFailed: Boolean = false,
) {
    private val cycleLengthValidation: CycleLengthValidation =
        SetupValidation.validateCycleLength(cycleLengthInput)

    val isDateInFuture: Boolean =
        lastMenstruationDate != null && !SetupValidation.isLastMenstruationDateValid(lastMenstruationDate, today)

    /** Rejection reason to show next to the field, only after the user edited it. */
    val cycleLengthError: CycleLengthValidation? =
        cycleLengthValidation.takeIf { cycleLengthEdited && it !is CycleLengthValidation.Valid }

    val showReferenceRangeWarning: Boolean =
        (cycleLengthValidation as? CycleLengthValidation.Valid)?.outsideReferenceRange == true

    /** Same rules as a history record end date (STAGE_2 §3, rule 2). */
    val endDateError: EndDateError? = when {
        lastMenstruationEndDate == null -> null
        lastMenstruationDate != null && lastMenstruationEndDate.isBefore(lastMenstruationDate) -> EndDateError.BeforeStart
        lastMenstruationEndDate.isAfter(today) -> EndDateError.InFuture
        else -> null
    }

    val canConfirm: Boolean =
        lastMenstruationDate != null && !isDateInFuture && endDateError == null &&
            cycleLengthValidation is CycleLengthValidation.Valid &&
            !isSaving && !isSaved

    internal fun toInitialSetupOrNull(): InitialSetup? {
        val date = lastMenstruationDate ?: return null
        val valid = cycleLengthValidation as? CycleLengthValidation.Valid ?: return null
        return InitialSetup(date, valid.days, lastMenstruationEndDate)
    }
}

class SetupViewModel(
    private val repository: SetupRepository,
    clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState(today = LocalDate.now(clock)))
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    fun onDateSelected(date: LocalDate) {
        _uiState.update { it.copy(lastMenstruationDate = date, saveFailed = false) }
    }

    fun onEndDateSelected(date: LocalDate) {
        _uiState.update { it.copy(lastMenstruationEndDate = date, saveFailed = false) }
    }

    fun onEndDateCleared() {
        _uiState.update { it.copy(lastMenstruationEndDate = null, saveFailed = false) }
    }

    fun onCycleLengthChanged(input: String) {
        _uiState.update { it.copy(cycleLengthInput = input, cycleLengthEdited = true, saveFailed = false) }
    }

    fun onConfirm() {
        val current = _uiState.value
        if (!current.canConfirm) return
        val setup = current.toInitialSetupOrNull() ?: return

        _uiState.update { it.copy(isSaving = true, saveFailed = false) }
        viewModelScope.launch {
            try {
                repository.save(setup)
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Do not log the exception: it may reference personal data.
                _uiState.update { it.copy(isSaving = false, saveFailed = true) }
            }
        }
    }

    companion object {
        fun factory(repository: SetupRepository, clock: Clock): ViewModelProvider.Factory = viewModelFactory {
            initializer { SetupViewModel(repository, clock) }
        }
    }
}
