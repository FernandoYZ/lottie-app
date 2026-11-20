package com.fernandoyz.lottie.presentation.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.fernandoyz.lottie.domain.model.MenstruationRecord
import com.fernandoyz.lottie.presentation.common.CalendarDatePickerDialog
import com.fernandoyz.lottie.presentation.common.formatLongDate
import com.fernandoyz.lottie.presentation.common.formatShortDate
import com.fernandoyz.lottie.ui.theme.LottieTheme
import java.time.LocalDate

/** Stateful container: connects [HistoryViewModel] with the stateless [HistoryScreen]. */
@Composable
fun HistoryRoute(
    viewModel: HistoryViewModel,
    onAddRecord: () -> Unit,
    onOpenRecord: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryScreen(
        uiState = uiState,
        onAddRecord = onAddRecord,
        onOpenRecord = onOpenRecord,
        onMarkEnd = viewModel::onMarkEnd,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

@Composable
fun HistoryScreen(
    uiState: HistoryUiState,
    onAddRecord: () -> Unit,
    onOpenRecord: (Long) -> Unit,
    onMarkEnd: (LocalDate) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        uiState.isLoading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        uiState.loadFailed -> Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.history_load_error))
            Button(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
        }

        else -> HistoryContent(uiState, onAddRecord, onOpenRecord, onMarkEnd, modifier)
    }
}

@Composable
private fun HistoryContent(
    uiState: HistoryUiState,
    onAddRecord: () -> Unit,
    onOpenRecord: (Long) -> Unit,
    onMarkEnd: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMarkEndPicker by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(text = stringResource(R.string.home_title), style = MaterialTheme.typography.headlineMedium)
        }

        uiState.openRecord?.let { open ->
            item {
                OpenRecordCard(
                    record = open,
                    errorText = uiState.markEndError?.let { stringResource(it.messageRes()) }
                        ?: if (uiState.markEndFailed) stringResource(R.string.history_mark_end_failed) else null,
                    onMarkEnd = { showMarkEndPicker = true },
                )
            }
        }

        item {
            Button(onClick = onAddRecord, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.history_add))
            }
        }

        item {
            Text(text = stringResource(R.string.history_section), style = MaterialTheme.typography.titleMedium)
        }

        if (uiState.records.isEmpty()) {
            item { Text(stringResource(R.string.history_empty)) }
        } else {
            items(uiState.records, key = { it.id }) { record ->
                RecordRow(record = record, onClick = { onOpenRecord(record.id) })
                HorizontalDivider()
            }
        }
    }

    val open = uiState.openRecord
    if (showMarkEndPicker && open != null) {
        CalendarDatePickerDialog(
            initialDate = uiState.today,
            minDate = open.startDate,
            maxDate = uiState.today,
            onDateSelected = {
                onMarkEnd(it)
                showMarkEndPicker = false
            },
            onDismiss = { showMarkEndPicker = false },
        )
    }
}

@Composable
private fun OpenRecordCard(record: MenstruationRecord, errorText: String?, onMarkEnd: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.history_open_since, formatLongDate(record.startDate)))
            OutlinedButton(onClick = onMarkEnd) { Text(stringResource(R.string.history_mark_end)) }
            if (errorText != null) {
                Text(
                    text = errorText,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }
    }
}

@Composable
private fun RecordRow(record: MenstruationRecord, onClick: () -> Unit) {
    val end = record.endDate
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = if (end == null) {
                formatShortDate(record.startDate)
            } else {
                stringResource(R.string.history_range, formatShortDate(record.startDate), formatShortDate(end))
            },
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = record.durationDays?.let { pluralStringResource(R.plurals.history_duration_days, it, it) }
                ?: stringResource(R.string.history_ongoing),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HistoryScreenPreview() {
    LottieTheme {
        HistoryScreen(
            uiState = HistoryUiState(
                today = LocalDate.of(2026, 6, 15),
                isLoading = false,
                records = listOf(
                    MenstruationRecord(2, LocalDate.of(2026, 6, 12), null),
                    MenstruationRecord(1, LocalDate.of(2026, 5, 14), LocalDate.of(2026, 5, 17)),
                ),
            ),
            onAddRecord = {},
            onOpenRecord = {},
            onMarkEnd = {},
            onRetry = {},
        )
    }
}
