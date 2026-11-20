package com.fernandoyz.lottie.presentation.onboarding.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import com.fernandoyz.lottie.presentation.common.CycleLengthField
import com.fernandoyz.lottie.presentation.common.formatLongDate
import com.fernandoyz.lottie.ui.theme.LottieTheme
import java.time.LocalDate

/** Stateful container: connects [SetupViewModel] with the stateless [SetupScreen]. */
@Composable
fun SetupRoute(
    viewModel: SetupViewModel,
    onSetupSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onSetupSaved()
    }

    SetupScreen(
        uiState = uiState,
        onDateSelected = viewModel::onDateSelected,
        onEndDateSelected = viewModel::onEndDateSelected,
        onEndDateCleared = viewModel::onEndDateCleared,
        onCycleLengthChanged = viewModel::onCycleLengthChanged,
        onConfirm = viewModel::onConfirm,
        modifier = modifier,
    )
}

private enum class SetupPicker { Start, End }

@Composable
fun SetupScreen(
    uiState: SetupUiState,
    onDateSelected: (LocalDate) -> Unit,
    onEndDateSelected: (LocalDate) -> Unit,
    onEndDateCleared: () -> Unit,
    onCycleLengthChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var picker by rememberSaveable { mutableStateOf<SetupPicker?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = stringResource(R.string.setup_title), style = MaterialTheme.typography.headlineMedium)

        DateField(
            label = stringResource(R.string.setup_date_label),
            value = uiState.lastMenstruationDate?.let(::formatLongDate) ?: stringResource(R.string.setup_date_select),
            error = if (uiState.isDateInFuture) stringResource(R.string.setup_date_error_future) else null,
            onClick = { picker = SetupPicker.Start },
        )

        DateField(
            label = stringResource(R.string.setup_end_date_label),
            value = uiState.lastMenstruationEndDate?.let(::formatLongDate) ?: stringResource(R.string.record_end_none),
            error = when (uiState.endDateError) {
                EndDateError.BeforeStart -> stringResource(R.string.history_error_end_before_start)
                EndDateError.InFuture -> stringResource(R.string.history_error_end_in_future)
                null -> null
            },
            onClick = { picker = SetupPicker.End },
        )
        if (uiState.lastMenstruationEndDate != null) {
            TextButton(onClick = onEndDateCleared) { Text(stringResource(R.string.record_end_clear)) }
        }

        CycleLengthField(
            input = uiState.cycleLengthInput,
            error = uiState.cycleLengthError,
            showWarning = uiState.showReferenceRangeWarning,
            onValueChange = onCycleLengthChanged,
        )

        if (uiState.saveFailed) {
            Text(
                text = stringResource(R.string.setup_save_error),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }

        Button(
            onClick = onConfirm,
            enabled = uiState.canConfirm,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (uiState.isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.setup_save))
            }
        }
    }

    when (picker) {
        SetupPicker.Start -> CalendarDatePickerDialog(
            initialDate = uiState.lastMenstruationDate,
            maxDate = uiState.today,
            onDateSelected = {
                onDateSelected(it)
                picker = null
            },
            onDismiss = { picker = null },
        )
        SetupPicker.End -> CalendarDatePickerDialog(
            initialDate = uiState.lastMenstruationEndDate ?: uiState.today,
            minDate = uiState.lastMenstruationDate,
            maxDate = uiState.today,
            onDateSelected = {
                onEndDateSelected(it)
                picker = null
            },
            onDismiss = { picker = null },
        )
        null -> Unit
    }
}

@Composable
private fun DateField(label: String, value: String, error: String?, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label, style = MaterialTheme.typography.titleMedium)
        OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(value) }
        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SetupScreenPreview() {
    LottieTheme {
        SetupScreen(
            uiState = SetupUiState(
                today = LocalDate.of(2026, 11, 19),
                lastMenstruationDate = LocalDate.of(2026, 11, 2),
                cycleLengthInput = "50",
                cycleLengthEdited = true,
            ),
            onDateSelected = {},
            onEndDateSelected = {},
            onEndDateCleared = {},
            onCycleLengthChanged = {},
            onConfirm = {},
        )
    }
}
