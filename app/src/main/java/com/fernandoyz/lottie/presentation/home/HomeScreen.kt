package com.fernandoyz.lottie.presentation.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.fernandoyz.lottie.presentation.calendar.DayDetailSheet
import com.fernandoyz.lottie.presentation.common.WeekStrip
import com.fernandoyz.lottie.presentation.common.formatShortDate
import com.fernandoyz.lottie.presentation.history.HistoryUiState
import com.fernandoyz.lottie.presentation.history.HistoryViewModel
import com.fernandoyz.lottie.presentation.history.messageRes
import com.fernandoyz.lottie.ui.theme.LottieTheme
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.cos
import kotlin.math.sin

/** Stateful container: connects [HistoryViewModel] with the stateless [HomeScreen]. */
@Composable
fun HomeRoute(
    viewModel: HistoryViewModel,
    onOpenDailyLog: (LocalDate) -> Unit,
    onEditMenstruations: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        uiState = uiState,
        onRetry = viewModel::retry,
        onMainAction = viewModel::onMainActionTapped,
        onConfirmOneDay = viewModel::onConfirmOneDay,
        onDismissOneDay = viewModel::onDismissOneDay,
        onOpenDailyLog = { onOpenDailyLog(uiState.today) },
        onEditMenstruations = onEditMenstruations,
        onDayTapped = viewModel::onDayTapped,
        onDismissDayDetail = viewModel::onDismissDayDetail,
        onOpenDailyLogFor = { date ->
            viewModel.onDismissDayDetail()
            onOpenDailyLog(date)
        },
        modifier = modifier,
    )
}

/** Home tab: week, cycle ring, cycle day, main action and next dates (HOME §2-§5, §7). */
@Composable
fun HomeScreen(
    uiState: HistoryUiState,
    onRetry: () -> Unit,
    onMainAction: () -> Unit,
    onConfirmOneDay: () -> Unit,
    onDismissOneDay: () -> Unit,
    onOpenDailyLog: () -> Unit,
    modifier: Modifier = Modifier,
    onEditMenstruations: () -> Unit = {},
    onDayTapped: (LocalDate) -> Unit = {},
    onDismissDayDetail: () -> Unit = {},
    onOpenDailyLogFor: (LocalDate) -> Unit = {},
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

        else -> HomeContent(uiState, onMainAction, onOpenDailyLog, onEditMenstruations, onDayTapped, modifier)
    }

    uiState.selectedDay?.let { date ->
        DayDetailSheet(
            date = date,
            mark = uiState.marksFor(YearMonth.from(date))[date],
            info = uiState.selectedDayInfo,
            log = uiState.selectedDayLog,
            onOpenDailyLog = if (date.isAfter(uiState.today)) null else { { onOpenDailyLogFor(date) } },
            onDismiss = onDismissDayDetail,
        )
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
private fun HomeContent(
    uiState: HistoryUiState,
    onMainAction: () -> Unit,
    onOpenDailyLog: () -> Unit,
    onEditMenstruations: () -> Unit,
    onDayTapped: (LocalDate) -> Unit,
    modifier: Modifier,
) {
    val status = uiState.homeStatus
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        WeekStrip(
            days = uiState.week,
            today = uiState.today,
            marksFor = uiState::marksFor,
            onDayClick = onDayTapped,
            ovulationDaysFor = uiState::ovulationDaysFor,
            fertileDaysFor = uiState::fertileDaysFor,
            loggedDays = uiState.logs.keys,
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
                text = status.cycleLengthDays?.let { length -> stringResource(R.string.home_cycle_day_of, day, length) }
                    ?: stringResource(R.string.day_detail_cycle_day, day),
                style = MaterialTheme.typography.titleMedium,
            )
        }

        // "Editar menstruación" opens the calendar in edit mode (HOME §7.1).
        Button(
            onClick = if (status.action == HomeAction.Edit) onEditMenstruations else onMainAction,
            enabled = !uiState.isWriting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                stringResource(
                    when (status.action) {
                        is HomeAction.Finish -> R.string.home_action_finish
                        HomeAction.Edit -> R.string.home_action_edit
                        HomeAction.Start -> R.string.home_action_start
                    },
                ),
            )
        }
        OutlinedButton(onClick = onOpenDailyLog, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(if (uiState.hasTodayLog) R.string.home_edit_log_today else R.string.home_log_today))
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
    var showInfo by rememberSaveable { mutableStateOf(false) }
    val start = formatShortDate(estimate.start)
    val days = status.daysToNextPeriod
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp)) {
            Column(
                modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = when {
                        estimate.isPast || days == null || days < 0 -> stringResource(R.string.home_next_period, start)
                        days == 0 -> stringResource(R.string.home_next_period_today, start)
                        else -> pluralStringResource(R.plurals.home_next_period_in, days, start, days)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (estimate.isPast) {
                    Text(text = stringResource(R.string.estimate_past), style = MaterialTheme.typography.bodyMedium)
                }
                status.nextFertileWindow?.let { (fertileStart, fertileEnd) ->
                    Text(
                        text = stringResource(R.string.home_fertile_window, formatShortDate(fertileStart), formatShortDate(fertileEnd)),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            // The approximate note moves behind an info icon (HOME §7.4).
            if (estimate.isApproximate) {
                IconButton(onClick = { showInfo = true }) {
                    Icon(Icons.Outlined.Info, contentDescription = stringResource(R.string.home_estimate_info_action))
                }
            }
        }
    }
    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            text = { Text(stringResource(R.string.estimate_approximate)) },
            confirmButton = { TextButton(onClick = { showInfo = false }) { Text(stringResource(R.string.action_close)) } },
        )
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
            onOpenDailyLog = {},
        )
    }
}
