package com.fernandoyz.lottie.presentation.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fernandoyz.lottie.R
import com.fernandoyz.lottie.domain.CycleSummary
import com.fernandoyz.lottie.domain.model.MenstruationRecord
import com.fernandoyz.lottie.presentation.common.formatShortDate
import com.fernandoyz.lottie.ui.theme.LottieTheme
import java.time.LocalDate

/** Stateful container: connects [HistoryViewModel] with the stateless [HistoryScreen]. */
@Composable
fun HistoryRoute(viewModel: HistoryViewModel, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryScreen(uiState = uiState, onRetry = viewModel::retry, modifier = modifier)
}

/** History tab: cycle averages and one row per cycle, newest first (STATISTICS §2-§3). */
@Composable
fun HistoryScreen(uiState: HistoryUiState, onRetry: () -> Unit, modifier: Modifier = Modifier) {
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

        else -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(text = stringResource(R.string.history_section), style = MaterialTheme.typography.headlineMedium)
            }
            if (uiState.records.isEmpty()) {
                item { Text(stringResource(R.string.history_empty)) }
            } else {
                item { Summary(uiState.averageCycleLengthDays, uiState.averagePeriodLengthDays) }
                val longest = maxOf(
                    uiState.cycles.maxOf { it.lengthDays ?: it.dayOfCycle ?: 1 },
                    uiState.averageCycleLengthDays ?: 1,
                )
                items(uiState.cycles, key = { it.start.toEpochDay() }) { cycle ->
                    CycleRow(cycle = cycle, longestDays = longest, meanDays = uiState.averageCycleLengthDays)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun Summary(averageCycle: Int?, averagePeriod: Int?) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = averageCycle?.let { pluralStringResource(R.plurals.stats_average_cycle, it, it) }
                ?: stringResource(R.string.stats_average_cycle_missing),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = averagePeriod?.let { pluralStringResource(R.plurals.stats_average_period, it, it) }
                ?: stringResource(R.string.stats_average_period_missing),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

/** One cycle with its dates, menstruation and a bar to compare lengths (STATISTICS §3). */
@Composable
private fun CycleRow(cycle: CycleSummary, longestDays: Int, meanDays: Int?) {
    val start = formatShortDate(cycle.start)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = when {
                !cycle.isCurrent -> pluralStringResource(
                    R.plurals.stats_past_cycle, cycle.lengthDays!!, start, formatShortDate(cycle.end!!), cycle.lengthDays,
                )
                cycle.end != null -> stringResource(R.string.stats_current_range, start, formatShortDate(cycle.end))
                else -> stringResource(R.string.stats_current_since, start)
            },
            style = MaterialTheme.typography.bodyLarge,
        )
        cycle.dayOfCycle?.let {
            Text(text = stringResource(R.string.stats_current_day, it), style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            text = cycle.menstruationDays?.let { pluralStringResource(R.plurals.stats_menstruation, it, it) }
                ?: stringResource(R.string.stats_menstruation_ongoing),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        CycleBar(
            cycleDays = cycle.lengthDays ?: cycle.dayOfCycle ?: 1,
            // While the menstruation is in progress, only the days already lived are coloured.
            menstruationDays = cycle.menstruationDays ?: cycle.dayOfCycle ?: 0,
            longestDays = longestDays,
            meanDays = meanDays,
        )
    }
}

/** Decorative: the texts above already say the same for screen readers. */
@Composable
private fun CycleBar(cycleDays: Int, menstruationDays: Int, longestDays: Int, meanDays: Int?) {
    val colors = MaterialTheme.colorScheme
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(16.dp).clearAndSetSemantics {}) {
        val dayWidth = maxWidth / longestDays
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(dayWidth * cycleDays)
                .height(10.dp)
                .background(colors.surfaceVariant, CircleShape),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(dayWidth * menstruationDays.coerceAtMost(cycleDays))
                .height(10.dp)
                .background(colors.primary, CircleShape),
        )
        if (meanDays != null) {
            Box(
                modifier = Modifier
                    .offset(x = dayWidth * meanDays - 1.dp)
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(colors.onSurface),
            )
        }
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
            ),
            onRetry = {},
        )
    }
}
