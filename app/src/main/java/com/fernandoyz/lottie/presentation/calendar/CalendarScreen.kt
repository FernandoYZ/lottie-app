package com.fernandoyz.lottie.presentation.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fernandoyz.lottie.R
import com.fernandoyz.lottie.domain.DayMark
import com.fernandoyz.lottie.domain.PeriodEstimate
import com.fernandoyz.lottie.domain.model.MenstruationRecord
import com.fernandoyz.lottie.presentation.common.formatLongDate
import com.fernandoyz.lottie.ui.theme.LottieTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val spanish: Locale = Locale.forLanguageTag("es")
private val monthTitleFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", spanish)

/** Stateful container: connects [CalendarViewModel] with the stateless [CalendarScreen]. */
@Composable
fun CalendarRoute(viewModel: CalendarViewModel, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CalendarScreen(
        uiState = uiState,
        onPreviousMonth = viewModel::onPreviousMonth,
        onNextMonth = viewModel::onNextMonth,
        modifier = modifier,
    )
}

@Composable
fun CalendarScreen(
    uiState: CalendarUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
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

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onPreviousMonth) { Text("‹ " + stringResource(R.string.calendar_previous_month)) }
            Text(
                text = uiState.month.format(monthTitleFormatter).replaceFirstChar { it.titlecase(spanish) },
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
            )
            TextButton(onClick = onNextMonth) { Text(stringResource(R.string.calendar_next_month) + " ›") }
        }

        MonthGrid(month = uiState.month, marks = uiState.marks, today = uiState.today)

        Legend()
    }
}

@Composable
private fun MonthGrid(month: YearMonth, marks: Map<LocalDate, DayMark>, today: LocalDate) {
    // Weeks start on Monday (STAGE_3 §3).
    val leadingBlanks = month.atDay(1).dayOfWeek.value - DayOfWeek.MONDAY.value
    val cells: List<LocalDate?> = List(leadingBlanks) { null } + (1..month.lengthOfMonth()).map(month::atDay)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            DayOfWeek.entries.forEach { dayOfWeek ->
                Text(
                    text = dayOfWeek.getDisplayName(TextStyle.NARROW, spanish).uppercase(spanish),
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    Box(modifier = Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                        if (date != null) DayCell(date = date, mark = marks[date], isToday = date == today)
                    }
                }
                repeat(7 - week.size) { Box(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, mark: DayMark?, isToday: Boolean) {
    val primary = MaterialTheme.colorScheme.primary
    val description = when (mark) {
        DayMark.Registered -> stringResource(R.string.calendar_day_registered, formatLongDate(date))
        DayMark.Estimated -> stringResource(R.string.calendar_day_estimated, formatLongDate(date))
        null -> formatLongDate(date)
    }
    val shapeModifier = when (mark) {
        DayMark.Registered -> Modifier.background(primary, CircleShape)
        DayMark.Estimated -> Modifier.border(2.dp, primary, CircleShape)
        null -> Modifier
    }
    Box(
        modifier = Modifier
            .padding(3.dp)
            .fillMaxSize()
            .then(shapeModifier)
            .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            color = if (mark == DayMark.Registered) MaterialTheme.colorScheme.onPrimary else Color.Unspecified,
            style = if (isToday) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun Legend() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LegendRow(mark = DayMark.Registered, label = stringResource(R.string.calendar_legend_registered))
        LegendRow(mark = DayMark.Estimated, label = stringResource(R.string.calendar_legend_estimated))
    }
}

@Composable
private fun LegendRow(mark: DayMark, label: String) {
    val primary = MaterialTheme.colorScheme.primary
    val swatch = if (mark == DayMark.Registered) Modifier.background(primary, CircleShape) else Modifier.border(2.dp, primary, CircleShape)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.size(18.dp).then(swatch))
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
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
                estimate = PeriodEstimate(LocalDate.of(2026, 6, 29), LocalDate.of(2026, 7, 2), false),
            ),
            onPreviousMonth = {},
            onNextMonth = {},
        )
    }
}
