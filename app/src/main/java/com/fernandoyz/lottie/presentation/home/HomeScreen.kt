package com.fernandoyz.lottie.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fernandoyz.lottie.R
import com.fernandoyz.lottie.domain.PeriodEstimate
import com.fernandoyz.lottie.domain.model.MenstruationRecord
import com.fernandoyz.lottie.presentation.calendar.nameRes
import com.fernandoyz.lottie.presentation.common.formatShortDate
import com.fernandoyz.lottie.presentation.history.HistoryUiState
import com.fernandoyz.lottie.presentation.history.HistoryViewModel
import com.fernandoyz.lottie.ui.theme.LottieTheme
import java.time.LocalDate

/** Stateful container: connects [HistoryViewModel] with the stateless [HomeScreen]. */
@Composable
fun HomeRoute(viewModel: HistoryViewModel, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(uiState = uiState, onRetry = viewModel::retry, modifier = modifier)
}

/** Home tab: today's cycle day and phase, and the next estimate (bottom-bar S1-S2). */
@Composable
fun HomeScreen(uiState: HistoryUiState, onRetry: () -> Unit, modifier: Modifier = Modifier) {
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

        else -> Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = stringResource(R.string.home_title), style = MaterialTheme.typography.headlineMedium)

            uiState.todayInfo?.let { info ->
                Text(
                    text = info.phase?.let {
                        stringResource(R.string.home_today_phase, info.cycleDay, stringResource(it.nameRes()).lowercase())
                    } ?: stringResource(R.string.home_today_cycle_day, info.cycleDay),
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            uiState.estimate?.let { EstimateCard(it) }

            if (uiState.records.isEmpty()) {
                Text(text = stringResource(R.string.home_empty), style = MaterialTheme.typography.bodyLarge)
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

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    LottieTheme {
        HomeScreen(
            uiState = HistoryUiState(
                today = LocalDate.of(2026, 6, 15),
                isLoading = false,
                records = listOf(MenstruationRecord(1, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 4))),
                estimate = PeriodEstimate(LocalDate.of(2026, 6, 29), LocalDate.of(2026, 7, 2), isPast = false, isApproximate = true),
            ),
            onRetry = {},
        )
    }
}
