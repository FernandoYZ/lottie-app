package com.fernandoyz.lottie.presentation.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fernandoyz.lottie.R
import com.fernandoyz.lottie.domain.CyclePhase
import com.fernandoyz.lottie.domain.CycleSummary
import com.fernandoyz.lottie.domain.CycleSummaryStats
import com.fernandoyz.lottie.domain.DayLevels
import com.fernandoyz.lottie.domain.RangeSummary
import com.fernandoyz.lottie.domain.SummaryRange
import com.fernandoyz.lottie.presentation.calendar.nameRes
import com.fernandoyz.lottie.presentation.common.formatShortDate
import com.fernandoyz.lottie.presentation.dailylog.optionLabelRes
import java.time.LocalDate

/** Stateful container: the summary of the cycle that starts on [initialStart] (CYCLE_SUMMARY §2). */
@Composable
fun CycleSummaryRoute(viewModel: HistoryViewModel, initialStart: LocalDate, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var start by rememberSaveable { mutableLongStateOf(initialStart.toEpochDay()) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    CycleSummaryScreen(
        uiState = uiState,
        start = LocalDate.ofEpochDay(start),
        range = SummaryRange.entries[tab],
        onCycleChanged = { start = it.toEpochDay() },
        onRangeChanged = { tab = it.ordinal },
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
fun CycleSummaryScreen(
    uiState: HistoryUiState,
    start: LocalDate,
    range: SummaryRange,
    onCycleChanged: (LocalDate) -> Unit,
    onRangeChanged: (SummaryRange) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        IconButton(onClick = onBack, modifier = Modifier.padding(8.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.daily_log_back))
        }
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return
        }
        // Newest first, as in History.
        val cycles = uiState.cycles
        val index = cycles.indexOfFirst { it.start == start }
        val cycle = cycles.getOrNull(index)
        if (cycle == null) {
            Text(stringResource(R.string.history_empty), modifier = Modifier.padding(24.dp))
            return
        }
        val summary = CycleSummaryStats.summarize(cycle, range, uiState.records, uiState.estimate, uiState.logs, uiState.today)
        // "Registraste N de M días" is for the whole cycle, next to its dates (CYCLE_SUMMARY §2).
        val whole = CycleSummaryStats.summarize(cycle, SummaryRange.WholeCycle, uiState.records, uiState.estimate, uiState.logs, uiState.today)

        Header(
            cycle = cycle,
            onPrevious = cycles.getOrNull(index + 1)?.let { older -> { onCycleChanged(older.start) } },
            onNext = cycles.getOrNull(index - 1)?.let { newer -> { onCycleChanged(newer.start) } },
        )
        Text(
            text = pluralStringResource(R.plurals.summary_logged_days, whole.days.size, whole.loggedDays, whole.days.size),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )
        PrimaryScrollableTabRow(selectedTabIndex = range.ordinal, edgePadding = 16.dp) {
            SummaryRange.entries.forEach { option ->
                Tab(selected = option == range, onClick = { onRangeChanged(option) }, text = { Text(stringResource(option.labelRes())) })
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (summary.loggedDays == 0) {
                Text(stringResource(R.string.summary_empty), style = MaterialTheme.typography.bodyMedium)
            } else {
                LevelsSection(summary)
                TopOptionsSection(summary)
            }
        }
    }
}

@Composable
private fun Header(cycle: CycleSummary, onPrevious: (() -> Unit)?, onNext: (() -> Unit)?) {
    val start = formatShortDate(cycle.start)
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onPrevious?.invoke() }, enabled = onPrevious != null) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.summary_previous_cycle))
        }
        Text(
            text = when {
                !cycle.isCurrent -> pluralStringResource(
                    R.plurals.stats_past_cycle, cycle.lengthDays!!, start, formatShortDate(cycle.end!!), cycle.lengthDays,
                )
                cycle.end != null -> stringResource(R.string.stats_current_range, start, formatShortDate(cycle.end))
                else -> stringResource(R.string.stats_current_since, start)
            },
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        IconButton(onClick = { onNext?.invoke() }, enabled = onNext != null) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.summary_next_cycle))
        }
    }
}

/** One dot per logged day on 5 levels; days without a level stay blank (CYCLE_SUMMARY §2). */
@Composable
private fun LevelsSection(summary: RangeSummary) {
    val moodColor = MaterialTheme.colorScheme.primary
    val energyColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val moodLabels = stringArrayResource(R.array.mood_levels)
    val energyLabels = stringArrayResource(R.array.energy_levels)
    val moodSpoken = stringResource(R.string.summary_spoken_mood)
    val energySpoken = stringResource(R.string.summary_spoken_energy)
    // The chart is drawn, so screen readers get the same values as text.
    val spoken = summary.levels.filter { it.mood != null || it.energy != null }.joinToString(". ") { day ->
        listOfNotNull(
            formatShortDate(day.date),
            day.mood?.let { moodSpoken.format(moodLabels[it - 1]) },
            day.energy?.let { energySpoken.format(energyLabels[it - 1]) },
        ).joinToString(", ")
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.summary_levels_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        if (spoken.isEmpty()) {
            Text(stringResource(R.string.summary_levels_empty), style = MaterialTheme.typography.bodyMedium)
            return@Column
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .semantics { contentDescription = spoken },
        ) {
            val rows = 5
            val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
            val rowHeight = size.height / (rows - 1)
            repeat(rows) { row ->
                val y = row * rowHeight
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), pathEffect = dash)
            }
            val step = size.width / summary.levels.size
            val radius = 5.dp.toPx()
            summary.levels.forEachIndexed { index, day: DayLevels ->
                val x = step * index + step / 2
                // Level 5 at the top, level 1 at the bottom; energy is drawn slightly to the right so both show.
                day.mood?.let { drawCircle(moodColor, radius, Offset(x - radius / 2, (rows - it) * rowHeight)) }
                day.energy?.let { drawCircle(energyColor, radius, Offset(x + radius / 2, (rows - it) * rowHeight)) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendDot(moodColor, stringResource(R.string.daily_log_mood_level))
            LegendDot(energyColor, stringResource(R.string.daily_log_energy_level))
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun TopOptionsSection(summary: RangeSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.summary_top_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        if (summary.topOptions.isEmpty()) {
            Text(stringResource(R.string.summary_top_empty), style = MaterialTheme.typography.bodyMedium)
        }
        summary.topOptions.forEach { (option, count) ->
            Text(stringResource(R.string.summary_top_item, stringResource(optionLabelRes(option)), count), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

private fun SummaryRange.labelRes(): Int = when (this) {
    SummaryRange.WholeCycle -> R.string.summary_tab_whole
    SummaryRange.Menstrual -> CyclePhase.Menstrual.nameRes()
    SummaryRange.Follicular -> CyclePhase.Follicular.nameRes()
    SummaryRange.Ovulation -> CyclePhase.Ovulation.nameRes()
    SummaryRange.Luteal -> CyclePhase.Luteal.nameRes()
}
