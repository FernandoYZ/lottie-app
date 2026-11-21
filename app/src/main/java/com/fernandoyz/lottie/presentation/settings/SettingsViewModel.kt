package com.fernandoyz.lottie.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fernandoyz.lottie.data.history.HistoryRepository
import com.fernandoyz.lottie.data.privacy.DataEraser
import com.fernandoyz.lottie.data.privacy.PrivacyRepository
import com.fernandoyz.lottie.data.repository.SetupRepository
import com.fernandoyz.lottie.data.reminders.ReminderRepository
import com.fernandoyz.lottie.domain.Reminder
import com.fernandoyz.lottie.domain.ReminderSettings
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
import java.time.LocalTime

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
    /** Reminder switches and time (REMINDERS §2). */
    val reminders: ReminderSettings = ReminderSettings(),
    val reminderSaveFailed: Boolean = false,
    val notificationPermissionDenied: Boolean = false,
    /** "Bloquear Lottie" (SECURITY §1). */
    val appLockEnabled: Boolean = false,
    /** The phone has no screen lock, so the switch cannot be turned on. */
    val appLockUnavailable: Boolean = false,
    val appLockSaveFailed: Boolean = false,
    /** "Borrar todos los datos" (SECURITY §2). */
    val confirmErase: Boolean = false,
    val isErasing: Boolean = false,
    val eraseFailed: Boolean = false,
    /** Everything was erased: the app restarts at the welcome screen. */
    val erased: Boolean = false,
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
    private val reminderRepository: ReminderRepository,
    private val privacyRepository: PrivacyRepository,
    private val dataEraser: DataEraser,
    /** Called with the saved settings after every reminder change, to reschedule the daily check. */
    private val onRemindersChanged: (ReminderSettings) -> Unit,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                privacyRepository.appLockEnabled.collect { enabled -> _uiState.update { it.copy(appLockEnabled = enabled) } }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(appLockSaveFailed = true) }
            }
        }
        viewModelScope.launch {
            try {
                reminderRepository.settings.collect { settings -> _uiState.update { it.copy(reminders = settings) } }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, loadFailed = true) }
            }
        }
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

    fun onReminderToggled(reminder: Reminder, enabled: Boolean) {
        _uiState.update { it.copy(notificationPermissionDenied = false) }
        saveReminders { reminderRepository.setEnabled(reminder, enabled) }
    }

    fun onReminderTimeChanged(time: LocalTime) = saveReminders { reminderRepository.setTime(time) }

    /** The switch stays off and the reason is shown (REMINDERS §2). */
    fun onNotificationPermissionDenied() {
        _uiState.update { it.copy(notificationPermissionDenied = true) }
    }

    /** Called after the phone lock was confirmed when turning it on, or directly when turning it off. */
    fun onAppLockChanged(enabled: Boolean) {
        _uiState.update { it.copy(appLockEnabled = enabled, appLockUnavailable = false, appLockSaveFailed = false) }
        viewModelScope.launch {
            try {
                privacyRepository.setAppLockEnabled(enabled)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(appLockEnabled = !enabled, appLockSaveFailed = true) }
            }
        }
    }

    fun onAppLockUnavailable() = _uiState.update { it.copy(appLockUnavailable = true) }

    fun onEraseRequested() = _uiState.update { it.copy(confirmErase = true, eraseFailed = false) }

    fun onEraseDismissed() = _uiState.update { it.copy(confirmErase = false) }

    fun onEraseConfirmed() {
        if (_uiState.value.isErasing) return
        _uiState.update { it.copy(confirmErase = false, isErasing = true, eraseFailed = false) }
        viewModelScope.launch {
            try {
                dataEraser.eraseAll()
                _uiState.update { it.copy(isErasing = false, erased = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Do not log the exception: it may reference personal data.
                _uiState.update { it.copy(isErasing = false, eraseFailed = true) }
            }
        }
    }

    private fun saveReminders(write: suspend () -> Unit) {
        _uiState.update { it.copy(reminderSaveFailed = false) }
        viewModelScope.launch {
            try {
                write()
                onRemindersChanged(reminderRepository.settings.first())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Do not log the exception: it may reference personal data.
                _uiState.update { it.copy(reminderSaveFailed = true) }
            }
        }
    }

    companion object {
        fun factory(
            repository: SetupRepository,
            historyRepository: HistoryRepository,
            reminderRepository: ReminderRepository,
            privacyRepository: PrivacyRepository,
            dataEraser: DataEraser,
            onRemindersChanged: (ReminderSettings) -> Unit,
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { SettingsViewModel(repository, historyRepository, reminderRepository, privacyRepository, dataEraser, onRemindersChanged) }
            }
    }
}
