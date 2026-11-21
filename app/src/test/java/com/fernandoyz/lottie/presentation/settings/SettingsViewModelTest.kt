package com.fernandoyz.lottie.presentation.settings

import com.fernandoyz.lottie.MainDispatcherRule
import com.fernandoyz.lottie.data.FailingDataStore
import com.fernandoyz.lottie.data.InMemoryDataStore
import com.fernandoyz.lottie.data.privacy.DataEraser
import com.fernandoyz.lottie.data.privacy.PrivacyRepository
import com.fernandoyz.lottie.data.history.FakeMenstruationRecordDao
import com.fernandoyz.lottie.data.history.HistoryRepository
import com.fernandoyz.lottie.data.repository.SetupRepository
import com.fernandoyz.lottie.data.reminders.ReminderRepository
import com.fernandoyz.lottie.domain.Reminder
import com.fernandoyz.lottie.domain.ReminderSettings
import java.time.LocalTime
import com.fernandoyz.lottie.domain.CycleLengthValidation
import com.fernandoyz.lottie.domain.model.InitialSetup
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/** Synthetic values only; never real user data. */
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val setupDate = LocalDate.of(2026, 6, 1)
    private val today = LocalDate.of(2026, 6, 15)
    private val repository = SetupRepository(InMemoryDataStore())
    private val history = HistoryRepository(FakeMenstruationRecordDao(), repository)

    private val reminders = ReminderRepository(InMemoryDataStore())
    private val privacy = PrivacyRepository(InMemoryDataStore())
    private var erased = 0
    private var failErase = false
    private val eraser = DataEraser(clearDatabase = { if (failErase) error("disk") else erased++ }, dataStore = InMemoryDataStore(), cancelReminders = {})
    private val scheduled = mutableListOf<ReminderSettings>()

    private fun viewModel(reminderRepository: ReminderRepository = reminders) =
        SettingsViewModel(repository, history, reminderRepository, privacy, eraser, scheduled::add)

    private suspend fun completeSetup() = repository.save(InitialSetup(setupDate, setupDate.plusDays(3)))

    @Test
    fun `without a manual value the field is empty and the learned length is shown`() = runTest {
        completeSetup()
        history.add(LocalDate.of(2026, 5, 2), LocalDate.of(2026, 5, 5), today)
        history.add(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 4), today) // 30 days later

        val state = viewModel().uiState.value

        assertFalse(state.isLoading)
        assertEquals("", state.cycleLengthInput)
        assertEquals(30, state.learnedCycleLengthDays)
        assertFalse(state.canSave) // unchanged
    }

    @Test
    fun `learned length is the default 28 with fewer than two starts`() = runTest {
        completeSetup()

        assertEquals(28, viewModel().uiState.value.learnedCycleLengthDays)
    }

    @Test
    fun `manual value is saved`() = runTest {
        completeSetup()
        val vm = viewModel()

        vm.onCycleLengthChanged("31")
        vm.onSave()

        assertTrue(vm.uiState.value.isSaved)
        assertEquals(31, repository.manualCycleLengthDays.first())
    }

    @Test
    fun `after saving, editing again allows another save`() = runTest {
        completeSetup()
        val vm = viewModel()
        vm.onCycleLengthChanged("31")
        vm.onSave()

        vm.onCycleLengthChanged("32")

        assertFalse(vm.uiState.value.isSaved)
        assertTrue(vm.uiState.value.canSave)
    }

    @Test
    fun `learned length follows new records and keeps the typed value`() = runTest {
        completeSetup()
        history.add(LocalDate.of(2026, 5, 2), LocalDate.of(2026, 5, 5), today)
        val vm = viewModel()
        vm.onCycleLengthChanged("33")

        history.add(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 4), today) // 30 days later

        assertEquals(30, vm.uiState.value.learnedCycleLengthDays)
        assertEquals("33", vm.uiState.value.cycleLengthInput)
    }

    @Test
    fun `clearing the field goes back to the learned value`() = runTest {
        completeSetup()
        repository.setManualCycleLength(31)
        val vm = viewModel()
        assertEquals("31", vm.uiState.value.cycleLengthInput)

        vm.onCycleLengthChanged("")
        assertNull(vm.uiState.value.cycleLengthError)
        vm.onSave()

        assertNull(repository.manualCycleLengthDays.first())
    }

    @Test
    fun `value outside technical limit is rejected`() = runTest {
        completeSetup()
        val vm = viewModel()

        vm.onCycleLengthChanged("100")

        assertEquals(CycleLengthValidation.OutOfRange, vm.uiState.value.cycleLengthError)
        assertFalse(vm.uiState.value.canSave)
    }

    @Test
    fun `value outside reference range shows warning and can be saved`() = runTest {
        completeSetup()
        val vm = viewModel()

        vm.onCycleLengthChanged("50")

        assertTrue(vm.uiState.value.showReferenceRangeWarning)
        assertTrue(vm.uiState.value.canSave)
    }

    @Test
    fun `save failure is reported and not shown as saved`() = runTest {
        val failing = SetupRepository(FailingDataStore(failReads = false, failWrites = true))
        val vm = SettingsViewModel(failing, HistoryRepository(FakeMenstruationRecordDao(), failing), reminders, privacy, eraser, scheduled::add)

        vm.onCycleLengthChanged("30")
        vm.onSave()

        assertTrue(vm.uiState.value.saveFailed)
        assertFalse(vm.uiState.value.isSaved)
    }

    @Test
    fun `cycle length not longer than the longest recorded bleeding is rejected`() = runTest {
        completeSetup()
        history.add(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 5), today) // 5 days
        val vm = viewModel()

        vm.onCycleLengthChanged("5")

        assertEquals(CycleLengthValidation.NotLongerThanBleeding(5), vm.uiState.value.cycleLengthError)
        assertFalse(vm.uiState.value.canSave)
    }

    @Test
    fun `reminders start off at 09_00`() = runTest {
        completeSetup()

        assertEquals(ReminderSettings(emptySet(), LocalTime.of(9, 0)), viewModel().uiState.value.reminders)
    }

    @Test
    fun `turning a reminder on saves it and schedules the check`() = runTest {
        completeSetup()
        val vm = viewModel()

        vm.onReminderToggled(Reminder.DailyLog, true)

        val expected = ReminderSettings(setOf(Reminder.DailyLog), LocalTime.of(9, 0))
        assertEquals(expected, reminders.settings.first())
        assertEquals(expected, vm.uiState.value.reminders)
        assertEquals(listOf(expected), scheduled)
    }

    @Test
    fun `changing the time saves it and reschedules the check`() = runTest {
        completeSetup()
        reminders.setEnabled(Reminder.PeriodSoon, true)
        val vm = viewModel()

        vm.onReminderTimeChanged(LocalTime.of(20, 15))

        val expected = ReminderSettings(setOf(Reminder.PeriodSoon), LocalTime.of(20, 15))
        assertEquals(expected, reminders.settings.first())
        assertEquals(listOf(expected), scheduled)
    }

    @Test
    fun `a denied permission keeps the reminder off and explains it`() = runTest {
        completeSetup()
        val vm = viewModel()

        vm.onNotificationPermissionDenied()

        assertTrue(vm.uiState.value.notificationPermissionDenied)
        assertEquals(emptySet<Reminder>(), reminders.settings.first().enabled)
        assertTrue(scheduled.isEmpty())

        vm.onReminderToggled(Reminder.DailyLog, true)
        assertFalse(vm.uiState.value.notificationPermissionDenied)
    }

    @Test
    fun `a reminder save failure is reported`() = runTest {
        completeSetup()
        val vm = viewModel(ReminderRepository(FailingDataStore(failReads = false, failWrites = true)))

        vm.onReminderToggled(Reminder.DailyLog, true)

        assertTrue(vm.uiState.value.reminderSaveFailed)
        assertEquals(emptySet<Reminder>(), vm.uiState.value.reminders.enabled)
        assertTrue(scheduled.isEmpty())
    }

    @Test
    fun `the lock switch is stored and a missing screen lock is reported`() = runTest {
        val vm = viewModel()
        assertEquals(false, vm.uiState.value.appLockEnabled)

        vm.onAppLockUnavailable()
        assertEquals(true, vm.uiState.value.appLockUnavailable)

        vm.onAppLockChanged(true)
        assertEquals(true, vm.uiState.value.appLockEnabled)
        assertEquals(false, vm.uiState.value.appLockUnavailable)
        assertEquals(true, privacy.appLockEnabled.first())
    }

    @Test
    fun `erasing all data asks first and reports when it is done`() = runTest {
        val vm = viewModel()

        vm.onEraseRequested()
        assertEquals(true, vm.uiState.value.confirmErase)
        vm.onEraseDismissed()
        assertEquals(false, vm.uiState.value.confirmErase)
        assertEquals(0, erased)

        vm.onEraseRequested()
        vm.onEraseConfirmed()
        assertEquals(1, erased)
        assertEquals(true, vm.uiState.value.erased)
    }

    @Test
    fun `a failed erase is reported`() = runTest {
        failErase = true
        val vm = viewModel()

        vm.onEraseRequested()
        vm.onEraseConfirmed()

        assertEquals(true, vm.uiState.value.eraseFailed)
        assertEquals(false, vm.uiState.value.erased)
    }
}
