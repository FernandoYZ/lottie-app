package com.fernandoyz.lottie.presentation.onboarding.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fernandoyz.lottie.R
import com.fernandoyz.lottie.domain.CycleLengthValidation
import com.fernandoyz.lottie.presentation.common.CalendarDatePickerDialog
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
        onCycleLengthChanged = viewModel::onCycleLengthChanged,
        onConfirm = viewModel::onConfirm,
        modifier = modifier,
    )
}

@Composable
fun SetupScreen(
    uiState: SetupUiState,
    onDateSelected: (LocalDate) -> Unit,
    onCycleLengthChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

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
            date = uiState.lastMenstruationDate,
            isInFuture = uiState.isDateInFuture,
            onClick = { showDatePicker = true },
        )

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

    if (showDatePicker) {
        CalendarDatePickerDialog(
            initialDate = uiState.lastMenstruationDate,
            maxDate = uiState.today,
            onDateSelected = {
                onDateSelected(it)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }
}

@Composable
private fun DateField(date: LocalDate?, isInFuture: Boolean, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = stringResource(R.string.setup_date_label), style = MaterialTheme.typography.titleMedium)
        OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Text(date?.let(::formatLongDate) ?: stringResource(R.string.setup_date_select))
        }
        if (isInFuture) {
            Text(
                text = stringResource(R.string.setup_date_error_future),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun CycleLengthField(
    input: String,
    error: CycleLengthValidation?,
    showWarning: Boolean,
    onValueChange: (String) -> Unit,
) {
    val supportingText = when (error) {
        CycleLengthValidation.Empty -> stringResource(R.string.setup_cycle_error_empty)
        CycleLengthValidation.NotAWholeNumber -> stringResource(R.string.setup_cycle_error_not_number)
        CycleLengthValidation.OutOfRange -> stringResource(R.string.setup_cycle_error_out_of_range)
        is CycleLengthValidation.Valid, null ->
            if (showWarning) stringResource(R.string.setup_cycle_warning_reference)
            else stringResource(R.string.setup_cycle_hint)
    }

    OutlinedTextField(
        value = input,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.setup_cycle_label)) },
        suffix = { Text(stringResource(R.string.setup_cycle_suffix)) },
        supportingText = { Text(supportingText) },
        isError = error != null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
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
            onCycleLengthChanged = {},
            onConfirm = {},
        )
    }
}
