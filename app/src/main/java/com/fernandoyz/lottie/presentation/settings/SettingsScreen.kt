package com.fernandoyz.lottie.presentation.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fernandoyz.lottie.R
import com.fernandoyz.lottie.security.DeviceAuthenticator
import com.fernandoyz.lottie.domain.Reminder
import com.fernandoyz.lottie.presentation.common.CycleLengthField
import com.fernandoyz.lottie.ui.theme.LottieTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Stateful container: connects [SettingsViewModel] with the stateless [SettingsScreen]. */
@Composable
fun SettingsRoute(viewModel: SettingsViewModel, modifier: Modifier = Modifier, authenticator: DeviceAuthenticator? = null) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Reminder waiting for the answer to the notification permission (REMINDERS §2).
    var pendingReminder by rememberSaveable { mutableStateOf<String?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val reminder = Reminder.entries.firstOrNull { it.name == pendingReminder }
        pendingReminder = null
        if (granted && reminder != null) viewModel.onReminderToggled(reminder, true) else viewModel.onNotificationPermissionDenied()
    }

    SettingsScreen(
        uiState = uiState,
        onCycleLengthChanged = viewModel::onCycleLengthChanged,
        onSave = viewModel::onSave,
        onReminderToggled = { reminder, enabled ->
            val needsPermission = enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            if (needsPermission) {
                pendingReminder = reminder.name
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.onReminderToggled(reminder, enabled)
            }
        },
        onReminderTimeChanged = viewModel::onReminderTimeChanged,
        // Turning the lock on needs the phone lock first; turning it off does not (SECURITY §1).
        onAppLockToggled = { enabled ->
            when {
                !enabled -> viewModel.onAppLockChanged(false)
                authenticator == null || !authenticator.isAvailable() -> viewModel.onAppLockUnavailable()
                else -> authenticator.authenticate { confirmed -> if (confirmed) viewModel.onAppLockChanged(true) }
            }
        },
        modifier = modifier,
    )
}

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onCycleLengthChanged: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    onReminderToggled: (Reminder, Boolean) -> Unit = { _, _ -> },
    onReminderTimeChanged: (LocalTime) -> Unit = {},
    onAppLockToggled: (Boolean) -> Unit = {},
) {
    if (uiState.isLoading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    if (uiState.loadFailed) {
        Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.settings_load_error))
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium)
        Text(text = stringResource(R.string.settings_cycle_explanation), style = MaterialTheme.typography.bodyMedium)
        Text(
            text = if (uiState.learnedIsDefault) {
                pluralStringResource(R.plurals.settings_cycle_learned_default, uiState.learnedCycleLengthDays, uiState.learnedCycleLengthDays)
            } else {
                pluralStringResource(R.plurals.settings_cycle_learned, uiState.learnedCycleLengthDays, uiState.learnedCycleLengthDays)
            },
            style = MaterialTheme.typography.bodyMedium,
        )

        CycleLengthField(
            input = uiState.cycleLengthInput,
            error = uiState.cycleLengthError,
            showWarning = uiState.showReferenceRangeWarning,
            onValueChange = onCycleLengthChanged,
        )

        if (uiState.isSaved) {
            Text(
                text = stringResource(R.string.settings_saved),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }

        if (uiState.saveFailed) ErrorText(stringResource(R.string.settings_save_error))

        Button(onClick = onSave, enabled = uiState.canSave, modifier = Modifier.fillMaxWidth()) {
            if (uiState.isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.settings_save))
            }
        }

        HorizontalDivider()
        RemindersSection(uiState, onReminderToggled, onReminderTimeChanged)
        HorizontalDivider()
        PrivacySection(uiState, onAppLockToggled)
    }
}

/** "Privacidad" (SECURITY §1). */
@Composable
private fun PrivacySection(uiState: SettingsUiState, onAppLockToggled: (Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.privacy_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = uiState.appLockEnabled, role = Role.Switch, onValueChange = onAppLockToggled)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.privacy_app_lock), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Switch(checked = uiState.appLockEnabled, onCheckedChange = null)
        }
        if (uiState.appLockUnavailable) ErrorText(stringResource(R.string.privacy_app_lock_unavailable))
        if (uiState.appLockSaveFailed) ErrorText(stringResource(R.string.settings_save_error))
    }
}

/** One switch per reminder and one time for all (REMINDERS §2). */
@Composable
private fun RemindersSection(
    uiState: SettingsUiState,
    onReminderToggled: (Reminder, Boolean) -> Unit,
    onReminderTimeChanged: (LocalTime) -> Unit,
) {
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    val time = uiState.reminders.time

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.reminders_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        Reminder.entries.forEach { reminder ->
            val checked = reminder in uiState.reminders.enabled
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(value = checked, role = Role.Switch, onValueChange = { onReminderToggled(reminder, it) })
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(reminder.labelRes()), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Switch(checked = checked, onCheckedChange = null)
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.reminders_time), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = { showTimePicker = true }) {
                Text(time.format(DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm a")))
            }
        }

        if (uiState.notificationPermissionDenied) ErrorText(stringResource(R.string.reminders_permission_denied))
        if (uiState.reminderSaveFailed) ErrorText(stringResource(R.string.settings_save_error))
    }

    if (showTimePicker) {
        ReminderTimeDialog(
            initial = time,
            is24Hour = is24Hour,
            onConfirm = {
                showTimePicker = false
                onReminderTimeChanged(it)
            },
            onDismiss = { showTimePicker = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimeDialog(initial: LocalTime, is24Hour: Boolean, onConfirm: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = is24Hour)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reminders_time_dialog_title)) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) { Text(stringResource(R.string.settings_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun ErrorText(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@StringRes
private fun Reminder.labelRes(): Int = when (this) {
    Reminder.PeriodSoon -> R.string.reminders_period_soon
    Reminder.FertileWindow -> R.string.reminders_fertile_window
    Reminder.DailyLog -> R.string.reminders_daily_log
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    LottieTheme {
        SettingsScreen(
            uiState = SettingsUiState(isLoading = false, learnedCycleLengthDays = 29, learnedIsDefault = false, cycleLengthInput = "31"),
            onCycleLengthChanged = {},
            onSave = {},
        )
    }
}
