package com.fernandoyz.lottie.presentation.onboarding.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fernandoyz.lottie.data.repository.SetupRepository
import com.fernandoyz.lottie.domain.CalendarEditing
import com.fernandoyz.lottie.domain.CalendarMarks
import com.fernandoyz.lottie.domain.CycleStatistics
import com.fernandoyz.lottie.domain.DayAction
import com.fernandoyz.lottie.domain.DayMark
import com.fernandoyz.lottie.domain.HistoryValidationError
import com.fernandoyz.lottie.domain.model.InitialSetup
import com.fernandoyz.lottie.domain.model.MenstruationRecord
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth

data class SetupUiState(
    val today: LocalDate,
    val month: YearMonth = YearMonth.from(today),
    /** Start and end of the last menstruation, marked on the calendar. */
    val start: LocalDate? = null,
    val end: LocalDate? = null,
    val rejection: HistoryValidationError? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val saveFailed: Boolean = false,
) {
    internal val draft: List<MenstruationRecord> =
        if (start != null && end != null) listOf(MenstruationRecord(DRAFT_ID, start, end)) else emptyList()

    val marks: Map<LocalDate, DayMark> = CalendarMarks.marksFor(month, draft, estimate = null, today = today)

    val canConfirm: Boolean = draft.isNotEmpty() && !isSaving && !isSaved

    private companion object {
        const val DRAFT_ID = 0L
    }
}

/** Initial setup: tap the day the last menstruation started (STAGE_4 §4). */
class SetupViewModel(
    private val repository: SetupRepository,
    clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState(today = LocalDate.now(clock)))
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    fun onPreviousMonth() = _uiState.update { it.copy(month = it.month.minusMonths(1)) }

    fun onNextMonth() = _uiState.update { it.copy(month = it.month.plusMonths(1)) }

    /** Same tap rules as the calendar; there is only one menstruation, so a new start replaces it. */
    fun onDayTapped(date: LocalDate) {
        val state = _uiState.value
        if (state.isSaving || state.isSaved) return
        val action = CalendarEditing.actionFor(date, state.draft, state.today, CycleStatistics.DEFAULT_PERIOD_LENGTH_DAYS)
        val cleared = state.copy(rejection = null, saveFailed = false)
        _uiState.value = when (action) {
            is DayAction.Add -> cleared.copy(start = action.start, end = action.end)
            is DayAction.ChangeEnd -> cleared.copy(end = action.newEnd)
            is DayAction.Delete -> cleared.copy(start = null, end = null)
            DayAction.StartInFuture -> cleared.copy(rejection = HistoryValidationError.StartInFuture)
        }
    }

    fun onConfirm() {
        val current = _uiState.value
        if (!current.canConfirm) return
        val setup = InitialSetup(current.start ?: return, current.end)

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
