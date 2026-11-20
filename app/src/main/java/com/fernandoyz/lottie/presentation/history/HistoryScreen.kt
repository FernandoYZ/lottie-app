package com.fernandoyz.lottie.presentation.history

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fernandoyz.lottie.R
import com.fernandoyz.lottie.domain.PeriodEstimate
import com.fernandoyz.lottie.domain.model.MenstruationRecord
import com.fernandoyz.lottie.presentation.common.formatShortDate
import com.fernandoyz.lottie.ui.theme.LottieTheme
import java.time.LocalDate

/** Stateful container: connects [HistoryViewModel] with the stateless [HistoryScreen]. */
@Composable
fun HistoryRoute(
    viewModel: HistoryViewModel,
    onOpenCalendar: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryScreen(
        uiState = uiState,
        onOpenCalendar = onOpenCalendar,
        onOpenSettings = onOpenSettings,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

@Composable
fun HistoryScreen(
    uiState: HistoryUiState,
    onOpenCalendar: () -> Unit,
    onOpenSettings: () -> Unit,
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

        else -> HistoryContent(uiState, onOpenCalendar, onOpenSettings, modifier)
    }
}

@Composable
private fun HistoryContent(
    uiState: HistoryUiState,
    onOpenCalendar: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(text = stringResource(R.string.home_title), style = MaterialTheme.typography.headlineMedium)
        }

        uiState.estimate?.let { estimate ->
            item { EstimateCard(estimate) }
        }

        item {
            Button(onClick = onOpenCalendar, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.home_open_calendar))
            }
        }

        item {
            OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.home_open_settings))
            }
        }

        item {
            Text(text = stringResource(R.string.history_section), style = MaterialTheme.typography.titleMedium)
        }

        if (uiState.records.isEmpty()) {
            item { Text(stringResource(R.string.history_empty)) }
        } else {
            items(uiState.records, key = { it.id }) { record ->
                RecordRow(record = record, today = uiState.today)
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun EstimateCard(estimate: PeriodEstimate) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = stringResource(R.string.estimate_title), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.estimate_range, formatShortDate(estimate.start), formatShortDate(estimate.end)),
                style = MaterialTheme.typography.bodyLarge,
            )
            if (estimate.isPast) {
                Text(text = stringResource(R.string.estimate_past), style = MaterialTheme.typography.bodyMedium)
            }
            if (estimate.isApproximate) {
                Text(text = stringResource(R.string.estimate_approximate), style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                text = stringResource(R.string.estimate_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RecordRow(record: MenstruationRecord, today: LocalDate) {
    val end = record.endDate
    Column(
        modifier = Modifier
            .fillMaxWidth()
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
            text = when {
                end == null -> stringResource(R.string.history_ongoing)
                end.isAfter(today) -> stringResource(R.string.history_in_progress_until, formatShortDate(end))
                else -> pluralStringResource(R.plurals.history_duration_days, record.durationDays!!, record.durationDays)
            },
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
                    MenstruationRecord(2, LocalDate.of(2026, 6, 12), LocalDate.of(2026, 6, 17)),
                    MenstruationRecord(1, LocalDate.of(2026, 5, 14), LocalDate.of(2026, 5, 17)),
                ),
                estimate = PeriodEstimate(LocalDate.of(2026, 7, 10), LocalDate.of(2026, 7, 14), isPast = false, isApproximate = false),
            ),
            onOpenCalendar = {},
            onOpenSettings = {},
            onRetry = {},
        )
    }
}
