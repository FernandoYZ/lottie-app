package com.fernandoyz.lottie.presentation.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fernandoyz.lottie.R
import com.fernandoyz.lottie.presentation.common.CalendarDatePickerDialog
import com.fernandoyz.lottie.presentation.common.formatLongDate
import com.fernandoyz.lottie.ui.theme.LottieTheme
import java.time.LocalDate

private enum class PickerTarget { Start, End }

/** Stateful container: connects [RecordFormViewModel] with the stateless [RecordFormScreen]. */
@Composable
fun RecordFormRoute(
    viewModel: RecordFormViewModel,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isDone) {
        if (uiState.isDone) onDone()
    }

    RecordFormScreen(
        uiState = uiState,
        onStartSelected = viewModel::onStartSelected,
        onEndSelected = viewModel::onEndSelected,
        onEndCleared = viewModel::onEndCleared,
        onSave = viewModel::onSave,
        onDelete = viewModel::onDelete,
        modifier = modifier,
    )
}

@Composable
fun RecordFormScreen(
    uiState: RecordFormUiState,
    onStartSelected: (LocalDate) -> Unit,
    onEndSelected: (LocalDate) -> Unit,
    onEndCleared: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (uiState.isLoading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    if (uiState.loadFailed) {
        Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.record_load_error))
        }
        return
    }

    var pickerTarget by rememberSaveable { mutableStateOf<PickerTarget?>(null) }
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(if (uiState.isEditing) R.string.record_title_edit else R.string.record_title_new),
            style = MaterialTheme.typography.headlineMedium,
        )

        LabeledDateButton(
            label = stringResource(R.string.record_start_label),
            value = uiState.startDate?.let(::formatLongDate) ?: stringResource(R.string.setup_date_select),
            onClick = { pickerTarget = PickerTarget.Start },
        )

        LabeledDateButton(
            label = stringResource(R.string.record_end_label),
            value = uiState.endDate?.let(::formatLongDate) ?: stringResource(R.string.record_end_none),
            onClick = { pickerTarget = PickerTarget.End },
        )
        if (uiState.endDate != null) {
            TextButton(onClick = onEndCleared) { Text(stringResource(R.string.record_end_clear)) }
        }

        val message = uiState.validationError?.let { stringResource(it.messageRes()) }
            ?: when {
                uiState.saveFailed -> stringResource(R.string.record_save_error)
                uiState.deleteFailed -> stringResource(R.string.record_delete_error)
                else -> null
            }
        if (message != null) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }

        Button(onClick = onSave, enabled = uiState.canSave, modifier = Modifier.fillMaxWidth()) {
            if (uiState.isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.record_save))
            }
        }

        if (uiState.isEditing) {
            OutlinedButton(
                onClick = { showDeleteConfirmation = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.record_delete)) }
        }
    }

    when (pickerTarget) {
        PickerTarget.Start -> CalendarDatePickerDialog(
            initialDate = uiState.startDate,
            maxDate = uiState.today,
            onDateSelected = {
                onStartSelected(it)
                pickerTarget = null
            },
            onDismiss = { pickerTarget = null },
        )
        PickerTarget.End -> CalendarDatePickerDialog(
            initialDate = uiState.endDate ?: uiState.today,
            minDate = uiState.startDate,
            maxDate = uiState.today,
            onDateSelected = {
                onEndSelected(it)
                pickerTarget = null
            },
            onDismiss = { pickerTarget = null },
        )
        null -> Unit
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.record_delete_confirm_title)) },
            text = { Text(stringResource(R.string.record_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmation = false
                    onDelete()
                }) { Text(stringResource(R.string.record_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun LabeledDateButton(label: String, value: String, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label, style = MaterialTheme.typography.titleMedium)
        OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(value) }
    }
}

@Preview(showBackground = true)
@Composable
private fun RecordFormScreenPreview() {
    LottieTheme {
        RecordFormScreen(
            uiState = RecordFormUiState(
                today = LocalDate.of(2026, 6, 15),
                recordId = 1,
                isLoading = false,
                startDate = LocalDate.of(2026, 6, 12),
            ),
            onStartSelected = {},
            onEndSelected = {},
            onEndCleared = {},
            onSave = {},
            onDelete = {},
        )
    }
}
