package com.fernandoyz.lottie.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
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
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlinx.coroutines.launch

private val spanish: Locale = Locale.forLanguageTag("es")
private val monthTitleFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", spanish)

private val firstPageMonth: YearMonth = YearMonth.of(1900, 1)
private const val PAGE_COUNT = 300 * 12 // 1900-2199

private fun pageOf(month: YearMonth): Int = (ChronoUnit.MONTHS.between(firstPageMonth, month).toInt()).coerceIn(0, PAGE_COUNT - 1)
private fun monthOf(page: Int): YearMonth = firstPageMonth.plusMonths(page.toLong())

/**
 * Month grid shared by the initial setup and the calendar screen. Weeks start on Monday (STAGE_3 §3).
 * Swiping changes month like book pages: finger to the right shows the previous month (STAGE_4 §6);
 * the arrows do the same. Days are tappable when [onDayClick] is not null (STAGE_4 §2). Today has a ring and
 * "Hoy" (CALENDAR_VIEW §1); [cycleDaysFor] adds the cycle day of each day (CALENDAR_VIEW §4).
 * [initialMonth] is read once; [onMonthChanged] reports the month that settles on screen.
 */
@Composable
fun MonthCalendar(
    initialMonth: YearMonth,
    today: LocalDate,
    marksFor: (YearMonth) -> Map<LocalDate, DayMark>,
    onMonthChanged: (YearMonth) -> Unit,
    onDayClick: ((LocalDate) -> Unit)?,
    modifier: Modifier = Modifier,
    onDayLongClick: ((LocalDate) -> Unit)? = null,
    ovulationDaysFor: (YearMonth) -> Set<LocalDate> = { emptySet() },
    fertileDaysFor: (YearMonth) -> Set<LocalDate> = { emptySet() },
    loggedDays: Set<LocalDate> = emptySet(),
    cycleDaysFor: (YearMonth) -> Map<LocalDate, Int> = { emptyMap() },
) {
    val pagerState = rememberPagerState(initialPage = pageOf(initialMonth)) { PAGE_COUNT }
    val scope = rememberCoroutineScope()
    val currentOnMonthChanged by rememberUpdatedState(onMonthChanged)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { currentOnMonthChanged(monthOf(it)) }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.calendar_previous_month))
            }
            Text(
                text = monthOf(pagerState.currentPage).format(monthTitleFormatter).replaceFirstChar { it.titlecase(spanish) },
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
            )
            IconButton(onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.calendar_next_month))
            }
        }
        WeekdayHeader()
        HorizontalPager(state = pagerState, verticalAlignment = Alignment.Top) { page ->
            val month = monthOf(page)
            MonthGrid(
                month = month,
                marks = marksFor(month),
                ovulationDays = ovulationDaysFor(month),
                fertileDays = fertileDaysFor(month),
                loggedDays = loggedDays,
                today = today,
                cycleDays = cycleDaysFor(month),
                onDayClick = onDayClick,
                onDayLongClick = onDayLongClick,
            )
        }
    }
}

@Composable
private fun WeekdayHeader() {
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
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    marks: Map<LocalDate, DayMark>,
    ovulationDays: Set<LocalDate>,
    fertileDays: Set<LocalDate>,
    loggedDays: Set<LocalDate>,
    today: LocalDate,
    cycleDays: Map<LocalDate, Int>,
    onDayClick: ((LocalDate) -> Unit)?,
    onDayLongClick: ((LocalDate) -> Unit)?,
) {
    val leadingBlanks = month.atDay(1).dayOfWeek.value - DayOfWeek.MONDAY.value
    val cells: List<LocalDate?> = List(leadingBlanks) { null } + (1..month.lengthOfMonth()).map(month::atDay)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    Box(modifier = Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                        if (date != null) {
                            DayCell(
                                date = date,
                                mark = marks[date],
                                isToday = date == today,
                                isOvulation = date in ovulationDays,
                                isFertile = date in fertileDays,
                                isLogged = date in loggedDays,
                                cycleDay = cycleDays[date],
                                onClick = onDayClick?.let { click -> { click(date) } },
                                onLongClick = onDayLongClick?.let { longClick -> { longClick(date) } },
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
private fun DayCell(
    date: LocalDate,
    mark: DayMark?,
    isToday: Boolean,
    isOvulation: Boolean,
    isFertile: Boolean,
    isLogged: Boolean,
    cycleDay: Int?,
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
) {
    val primary = MaterialTheme.colorScheme.primary
    val markDescription = when (mark) {
        DayMark.Registered -> stringResource(R.string.calendar_day_registered, formatLongDate(date))
        DayMark.Upcoming -> stringResource(R.string.calendar_day_upcoming, formatLongDate(date))
        DayMark.Estimated -> stringResource(R.string.calendar_day_estimated, formatLongDate(date))
        null -> formatLongDate(date)
    }
    val phaseDescription = when {
        isOvulation -> stringResource(R.string.calendar_day_ovulation, markDescription)
        isFertile -> stringResource(R.string.calendar_day_fertile, markDescription)
        else -> markDescription
    }
    val loggedDescription = if (isLogged) stringResource(R.string.calendar_day_logged, phaseDescription) else phaseDescription
    val description = if (isToday) stringResource(R.string.calendar_day_today, loggedDescription) else loggedDescription
    val fertileBackground = MaterialTheme.colorScheme.tertiaryContainer
    val detailLabel = stringResource(R.string.calendar_day_detail_action)
    val interactive = onClick != null || onLongClick != null
    val textColor = when (mark) {
        DayMark.Registered -> MaterialTheme.colorScheme.onPrimary
        DayMark.Upcoming -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> Color.Unspecified
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (interactive) {
                    Modifier.combinedClickable(onClick = { onClick?.invoke() }, onLongClick = onLongClick)
                } else {
                    Modifier
                },
            )
            .clearAndSetSemantics {
                contentDescription = description
                if (onClick != null) {
                    role = Role.Button
                    onClick { onClick(); true }
                }
                if (onLongClick != null) onLongClick(label = detailLabel) { onLongClick(); true }
            },
        contentAlignment = Alignment.Center,
    ) {
        // Ring around today, outside any other mark (CALENDAR_VIEW §1).
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp)
                .then(if (isToday) Modifier.border(2.dp, MaterialTheme.colorScheme.outline, CircleShape) else Modifier)
                .padding(3.dp)
                .then(if (isFertile && mark == null) Modifier.background(fertileBackground, CircleShape) else Modifier)
                .dayMark(mark, primary, MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isToday) {
                    Text(text = stringResource(R.string.calendar_today), color = textColor, style = MaterialTheme.typography.labelSmall)
                }
                Text(text = date.dayOfMonth.toString(), color = textColor, style = MaterialTheme.typography.bodyMedium)
            }
            Row(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (isLogged) Dot(MaterialTheme.colorScheme.secondary)
                if (isOvulation) Dot(MaterialTheme.colorScheme.tertiary)
            }
        }
        if (cycleDay != null) {
            Text(
                text = cycleDay.toString(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.TopStart),
            )
        }
    }
}

@Composable
private fun Dot(color: Color) {
    Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
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
        if (showEstimated) {
            LegendRow(mark = DayMark.Estimated, label = stringResource(R.string.calendar_legend_estimated))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(18.dp), contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.tertiary, CircleShape))
                }
                Text(text = stringResource(R.string.calendar_legend_ovulation), style = MaterialTheme.typography.bodyMedium)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(18.dp).background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape))
                Text(text = stringResource(R.string.calendar_legend_fertile), style = MaterialTheme.typography.bodyMedium)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(18.dp), contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.secondary, CircleShape))
                }
                Text(text = stringResource(R.string.calendar_legend_logged), style = MaterialTheme.typography.bodyMedium)
            }
        }
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
