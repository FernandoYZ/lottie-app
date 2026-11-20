package com.fernandoyz.lottie.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fernandoyz.lottie.R
import com.fernandoyz.lottie.domain.DayMark
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val spanish: Locale = Locale.forLanguageTag("es")
private val monthTitleFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", spanish)

/**
 * Month grid with previous/next navigation, shared by the initial setup and the calendar screen.
 * Weeks start on Monday (STAGE_3 §3). Days are tappable when [onDayClick] is not null (STAGE_4 §2).
 */
@Composable
fun MonthCalendar(
    month: YearMonth,
    marks: Map<LocalDate, DayMark>,
    today: LocalDate,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDayClick: ((LocalDate) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onPreviousMonth) { Text("‹ " + stringResource(R.string.calendar_previous_month)) }
            Text(
                text = month.format(monthTitleFormatter).replaceFirstChar { it.titlecase(spanish) },
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
            )
            TextButton(onClick = onNextMonth) { Text(stringResource(R.string.calendar_next_month) + " ›") }
        }
        MonthGrid(month = month, marks = marks, today = today, onDayClick = onDayClick)
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    marks: Map<LocalDate, DayMark>,
    today: LocalDate,
    onDayClick: ((LocalDate) -> Unit)?,
) {
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
                        if (date != null) {
                            DayCell(
                                date = date,
                                mark = marks[date],
                                isToday = date == today,
                                onClick = onDayClick?.let { click -> { click(date) } },
                            )
                        }
                    }
                }
                repeat(7 - week.size) { Box(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, mark: DayMark?, isToday: Boolean, onClick: (() -> Unit)?) {
    val primary = MaterialTheme.colorScheme.primary
    val description = when (mark) {
        DayMark.Registered -> stringResource(R.string.calendar_day_registered, formatLongDate(date))
        DayMark.Upcoming -> stringResource(R.string.calendar_day_upcoming, formatLongDate(date))
        DayMark.Estimated -> stringResource(R.string.calendar_day_estimated, formatLongDate(date))
        null -> formatLongDate(date)
    }
    Box(
        modifier = Modifier
            .padding(3.dp)
            .fillMaxSize()
            .dayMark(mark, primary, MaterialTheme.colorScheme.primaryContainer)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .clearAndSetSemantics {
                contentDescription = description
                if (onClick != null) {
                    role = Role.Button
                    onClick { onClick(); true }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            color = when (mark) {
                DayMark.Registered -> MaterialTheme.colorScheme.onPrimary
                DayMark.Upcoming -> MaterialTheme.colorScheme.onPrimaryContainer
                else -> Color.Unspecified
            },
            style = if (isToday) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun Modifier.dayMark(mark: DayMark?, primary: Color, primaryContainer: Color): Modifier = when (mark) {
    DayMark.Registered -> background(primary, CircleShape)
    DayMark.Upcoming -> background(primaryContainer, CircleShape).border(2.dp, primary, CircleShape)
    DayMark.Estimated -> border(2.dp, primary, CircleShape)
    null -> this
}

/** Legend for the marks the calendar can show. */
@Composable
fun CalendarLegend(modifier: Modifier = Modifier, showEstimated: Boolean = true) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LegendRow(mark = DayMark.Registered, label = stringResource(R.string.calendar_legend_registered))
        LegendRow(mark = DayMark.Upcoming, label = stringResource(R.string.calendar_legend_upcoming))
        if (showEstimated) LegendRow(mark = DayMark.Estimated, label = stringResource(R.string.calendar_legend_estimated))
    }
}

@Composable
private fun LegendRow(mark: DayMark, label: String) {
    val swatch = Modifier.dayMark(mark, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.size(18.dp).then(swatch))
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}
