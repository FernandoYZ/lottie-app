package com.fernandoyz.lottie.presentation.onboarding.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fernandoyz.lottie.R
import com.fernandoyz.lottie.presentation.common.CalendarLegend
import com.fernandoyz.lottie.presentation.common.MonthCalendar
import com.fernandoyz.lottie.presentation.common.formatShortDate
import com.fernandoyz.lottie.presentation.history.messageRes
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
        onPreviousMonth = viewModel::onPreviousMonth,
        onNextMonth = viewModel::onNextMonth,
        onDayTapped = viewModel::onDayTapped,
        onConfirm = viewModel::onConfirm,
        modifier = modifier,
    )
}

@Composable
fun SetupScreen(
    uiState: SetupUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDayTapped: (LocalDate) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = stringResource(R.string.setup_title), style = MaterialTheme.typography.headlineMedium)
        Text(text = stringResource(R.string.setup_calendar_instructions), style = MaterialTheme.typography.bodyMedium)

        MonthCalendar(
            month = uiState.month,
            marks = uiState.marks,
            today = uiState.today,
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth,
            onDayClick = onDayTapped,
        )

        val start = uiState.start
        val end = uiState.end
        if (start != null && end != null) {
            Text(
                text = stringResource(R.string.setup_selected_range, formatShortDate(start), formatShortDate(end)),
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        val error = uiState.rejection?.let { stringResource(it.messageRes()) }
            ?: if (uiState.saveFailed) stringResource(R.string.setup_save_error) else null
        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }

        CalendarLegend(showEstimated = false)

        Button(onClick = onConfirm, enabled = uiState.canConfirm, modifier = Modifier.fillMaxWidth()) {
            if (uiState.isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.setup_save))
            }
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
                start = LocalDate.of(2026, 11, 2),
                end = LocalDate.of(2026, 11, 6),
            ),
            onPreviousMonth = {},
            onNextMonth = {},
            onDayTapped = {},
            onConfirm = {},
        )
    }
}
