package com.fernandoyz.lottie.presentation.onboarding.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fernandoyz.lottie.data.history.HistoryRepository
import com.fernandoyz.lottie.domain.CalendarEditing
import com.fernandoyz.lottie.domain.CalendarMarks
import com.fernandoyz.lottie.domain.CycleStatistics
import com.fernandoyz.lottie.domain.DayAction
import com.fernandoyz.lottie.domain.DayMark
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
import java.time.YearMonth

data class SetupUiState(
    val today: LocalDate,
    val month: YearMonth = YearMonth.from(today),
    /** Menstruations marked on the calendar, not saved yet. Ids are local to this screen. */
    val menstruations: List<MenstruationRecord> = emptyList(),
    val rejection: HistoryValidationError? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val saveFailed: Boolean = false,
) {
    fun marksFor(month: YearMonth): Map<LocalDate, DayMark> =
        CalendarMarks.marksFor(month, menstruations, estimate = null, today = today)

    val marks: Map<LocalDate, DayMark> = marksFor(month)

    val canConfirm: Boolean = menstruations.isNotEmpty() && !isSaving && !isSaved
}

/** Initial setup: mark one or more past menstruations on the calendar (STAGE_4 §4). */
class SetupViewModel(
    private val historyRepository: HistoryRepository,
    clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState(today = LocalDate.now(clock)))
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    private var nextDraftId = 1L

    fun onMonthChanged(month: YearMonth) = _uiState.update { it.copy(month = month) }

    /** Same tap rules as the calendar screen; deleting needs no confirmation because nothing is saved yet. */
    fun onDayTapped(date: LocalDate) {
        val state = _uiState.value
        if (state.isSaving || state.isSaved) return
        val list = state.menstruations
        val fill = CycleStatistics.periodLengthDays(list, state.today)
        val cleared = state.copy(rejection = null, saveFailed = false)
        _uiState.value = when (val action = CalendarEditing.actionFor(date, list, state.today, fill)) {
            is DayAction.Add -> cleared.copy(menstruations = list + MenstruationRecord(nextDraftId++, action.start, action.end))
            is DayAction.ChangeEnd -> cleared.copy(
                menstruations = list.map { if (it.id == action.record.id) it.copy(endDate = action.newEnd) else it },
            )
            is DayAction.Delete -> cleared.copy(menstruations = list.filterNot { it.id == action.record.id })
            DayAction.StartInFuture -> cleared.copy(rejection = HistoryValidationError.StartInFuture)
        }
    }

    fun onConfirm() {
        val current = _uiState.value
        if (!current.canConfirm) return
        val marked = current.menstruations.mapNotNull { record -> record.endDate?.let { record.startDate to it } }

        _uiState.update { it.copy(isSaving = true, saveFailed = false) }
        viewModelScope.launch {
            try {
                historyRepository.completeOnboarding(marked)
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
        fun factory(historyRepository: HistoryRepository, clock: Clock): ViewModelProvider.Factory = viewModelFactory {
            initializer { SetupViewModel(historyRepository, clock) }
        }
    }
}
