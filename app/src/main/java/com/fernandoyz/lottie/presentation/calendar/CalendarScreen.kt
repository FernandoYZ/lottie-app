package com.fernandoyz.lottie.presentation.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fernandoyz.lottie.R
import com.fernandoyz.lottie.domain.PeriodEstimate
import com.fernandoyz.lottie.domain.model.MenstruationRecord
import com.fernandoyz.lottie.presentation.common.CalendarLegend
import com.fernandoyz.lottie.presentation.common.MonthCalendar
import com.fernandoyz.lottie.presentation.common.formatLongDate
import com.fernandoyz.lottie.presentation.history.messageRes
import com.fernandoyz.lottie.ui.theme.LottieTheme
import java.time.LocalDate
import java.time.YearMonth

/** Stateful container: connects [CalendarViewModel] with the stateless [CalendarScreen]. */
@Composable
fun CalendarRoute(viewModel: CalendarViewModel, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CalendarScreen(
        uiState = uiState,
        onMonthChanged = viewModel::onMonthChanged,
        onDayTapped = viewModel::onDayTapped,
        onConfirmDelete = viewModel::onConfirmDelete,
        onDismissDelete = viewModel::onDismissDelete,
        modifier = modifier,
    )
}

@Composable
fun CalendarScreen(
    uiState: CalendarUiState,
    onMonthChanged: (YearMonth) -> Unit,
    onDayTapped: (LocalDate) -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (uiState.isLoading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    if (uiState.loadFailed) {
        Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.calendar_load_error))
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = stringResource(R.string.calendar_title), style = MaterialTheme.typography.headlineMedium)
        Text(
            text = pluralStringResource(R.plurals.calendar_instructions, uiState.periodLengthDays, uiState.periodLengthDays),
            style = MaterialTheme.typography.bodyMedium,
        )

        MonthCalendar(
            initialMonth = uiState.month,
            marksFor = uiState::marksFor,
            today = uiState.today,
            onMonthChanged = onMonthChanged,
            onDayClick = onDayTapped,
        )

        val message = uiState.rejection?.let { stringResource(it.messageRes()) }
            ?: if (uiState.writeFailed) stringResource(R.string.calendar_write_error) else null
        if (message != null) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }

        CalendarLegend()
    }

    uiState.pendingDelete?.let { record ->
        AlertDialog(
            onDismissRequest = onDismissDelete,
            title = { Text(stringResource(R.string.calendar_delete_title)) },
            text = { Text(stringResource(R.string.calendar_delete_body, formatLongDate(record.startDate))) },
            confirmButton = { TextButton(onClick = onConfirmDelete) { Text(stringResource(R.string.calendar_delete_confirm)) } },
            dismissButton = { TextButton(onClick = onDismissDelete) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CalendarScreenPreview() {
    LottieTheme {
        CalendarScreen(
            uiState = CalendarUiState(
                today = LocalDate.of(2026, 6, 15),
                isLoading = false,
                records = listOf(MenstruationRecord(1, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 4))),
                estimate = PeriodEstimate(LocalDate.of(2026, 6, 29), LocalDate.of(2026, 7, 2), isPast = false, isApproximate = true),
            ),
            onMonthChanged = {},
            onDayTapped = {},
            onConfirmDelete = {},
            onDismissDelete = {},
        )
    }
}
