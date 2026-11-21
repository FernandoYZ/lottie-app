package com.fernandoyz.lottie.presentation.home

import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fernandoyz.lottie.R
import com.fernandoyz.lottie.domain.CycleRing
import com.fernandoyz.lottie.domain.HomeAction
import com.fernandoyz.lottie.domain.HomeHeadline
import com.fernandoyz.lottie.domain.HomeStatus
import com.fernandoyz.lottie.domain.PeriodEstimate
import com.fernandoyz.lottie.domain.RingKind
import com.fernandoyz.lottie.domain.model.MenstruationRecord
import com.fernandoyz.lottie.presentation.calendar.nameRes
import com.fernandoyz.lottie.presentation.common.formatDayHeader
import com.fernandoyz.lottie.presentation.common.formatShortDate
import com.fernandoyz.lottie.presentation.history.HistoryUiState
import com.fernandoyz.lottie.presentation.history.HistoryViewModel
import com.fernandoyz.lottie.presentation.history.messageRes
import com.fernandoyz.lottie.ui.theme.LottieTheme
import java.time.LocalDate
import kotlin.math.cos
import kotlin.math.sin

/** Stateful container: connects [HistoryViewModel] with the stateless [HomeScreen]. */
@Composable
fun HomeRoute(viewModel: HistoryViewModel, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        uiState = uiState,
        onRetry = viewModel::retry,
        onMainAction = viewModel::onMainActionTapped,
        onConfirmOneDay = viewModel::onConfirmOneDay,
        onDismissOneDay = viewModel::onDismissOneDay,
        modifier = modifier,
    )
}

/** Home tab: cycle ring, today's phase, main action and next dates (HOME §2-§5). */
@Composable
fun HomeScreen(
    uiState: HistoryUiState,
    onRetry: () -> Unit,
    onMainAction: () -> Unit,
    onConfirmOneDay: () -> Unit,
    onDismissOneDay: () -> Unit,
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

        else -> HomeContent(uiState, onMainAction, modifier)
    }

    if (uiState.pendingOneDay != null) {
        AlertDialog(
            onDismissRequest = onDismissOneDay,
            text = { Text(stringResource(R.string.home_one_day_body)) },
            confirmButton = { TextButton(onClick = onConfirmOneDay) { Text(stringResource(R.string.home_one_day_confirm)) } },
            dismissButton = { TextButton(onClick = onDismissOneDay) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun HomeContent(uiState: HistoryUiState, onMainAction: () -> Unit, modifier: Modifier) {
    val status = uiState.homeStatus
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = formatDayHeader(uiState.today),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth(),
        )

        Box(modifier = Modifier.size(240.dp), contentAlignment = Alignment.Center) {
            status.ring?.let { CycleRingCanvas(it, Modifier.fillMaxSize()) }
            Text(
                text = headlineText(status.headline),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(40.dp),
            )
        }

        status.cycleDay?.let { day ->
            Text(
                text = status.phase?.let { stringResource(R.string.home_phase_day, stringResource(it.nameRes()).lowercase(), day) }
                    ?: stringResource(R.string.day_detail_cycle_day, day),
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Button(onClick = onMainAction, enabled = !uiState.isWriting, modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(
                    if (status.action is HomeAction.Finish) R.string.home_action_finish else R.string.home_action_start,
                ),
            )
        }
        uiState.rejection?.let { Text(stringResource(it.messageRes()), color = MaterialTheme.colorScheme.error) }
        if (uiState.writeFailed) {
            Text(stringResource(R.string.home_write_error), color = MaterialTheme.colorScheme.error)
        }

        if (uiState.records.isEmpty()) {
            Text(text = stringResource(R.string.home_empty), style = MaterialTheme.typography.bodyLarge)
        }

        uiState.estimate?.let { NextDatesCard(it, status) }

        Text(
            text = stringResource(R.string.estimate_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun headlineText(headline: HomeHeadline): String = when (headline) {
    HomeHeadline.NoRecords -> stringResource(R.string.home_headline_no_records)
    is HomeHeadline.MenstruationDay -> stringResource(R.string.home_headline_menstruation_day, headline.day)
    is HomeHeadline.Delay -> pluralStringResource(R.plurals.home_headline_delay, headline.days, headline.days)
    HomeHeadline.PeriodToday -> stringResource(R.string.home_headline_period_today)
    HomeHeadline.OvulationToday -> stringResource(R.string.home_headline_ovulation_today)
    is HomeHeadline.OvulationIn -> pluralStringResource(R.plurals.home_headline_ovulation_in, headline.days, headline.days)
    is HomeHeadline.PeriodIn -> pluralStringResource(R.plurals.home_headline_period_in, headline.days, headline.days)
}

/** One arc per phase, starting at the top and going clockwise; a dot marks today (HOME §3). */
@Composable
private fun CycleRingCanvas(ring: CycleRing, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val kindColor: (RingKind) -> Color = { kind ->
        when (kind) {
            RingKind.Menstrual -> colors.primary
            RingKind.Fertile -> colors.tertiary
            RingKind.Follicular, RingKind.Luteal -> colors.primaryContainer
            RingKind.Other -> colors.surfaceVariant
        }
    }
    val delayColor = colors.outline
    val markerColor = colors.onSurface
    val markerBorder = colors.surface

    Canvas(modifier = modifier) {
        val strokeWidth = 18.dp.toPx()
        val inset = strokeWidth / 2 + 4.dp.toPx()
        val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
        val topLeft = Offset(inset, inset)
        val degreesPerDay = 360f / ring.lengthDays

        if (ring.delayed) {
            drawArc(delayColor, -90f, 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(strokeWidth))
            return@Canvas
        }

        ring.segments.forEach { segment ->
            val gap = if (ring.segments.size > 1) 1.5f else 0f
            drawArc(
                color = kindColor(segment.kind),
                startAngle = -90f + segment.firstDay * degreesPerDay + gap / 2,
                sweepAngle = segment.days * degreesPerDay - gap,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(strokeWidth),
            )
        }

        val angle = Math.toRadians((-90f + (ring.todayIndex + 0.5f) * degreesPerDay).toDouble())
        val radius = arcSize.width / 2
        val marker = Offset(center.x + radius * cos(angle).toFloat(), center.y + radius * sin(angle).toFloat())
        drawCircle(markerBorder, radius = strokeWidth / 2 + 2.dp.toPx(), center = marker)
        drawCircle(markerColor, radius = strokeWidth / 2 - 2.dp.toPx(), center = marker)
    }
}

@Composable
private fun NextDatesCard(estimate: PeriodEstimate, status: HomeStatus) {
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
            status.nextFertileWindow?.let { (start, end) ->
                Text(text = stringResource(R.string.calendar_legend_fertile), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.estimate_range, formatShortDate(start), formatShortDate(end)),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    LottieTheme {
        HomeScreen(
            uiState = HistoryUiState(
                today = LocalDate.of(2026, 6, 11),
                isLoading = false,
                records = listOf(MenstruationRecord(1, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 4))),
                estimate = PeriodEstimate(LocalDate.of(2026, 6, 29), LocalDate.of(2026, 7, 2), isPast = false, isApproximate = true),
            ),
            onRetry = {},
            onMainAction = {},
            onConfirmOneDay = {},
            onDismissOneDay = {},
        )
    }
}
