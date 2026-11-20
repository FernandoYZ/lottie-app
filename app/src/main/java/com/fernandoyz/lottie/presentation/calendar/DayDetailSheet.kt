package com.fernandoyz.lottie.presentation.calendar

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.fernandoyz.lottie.R
import com.fernandoyz.lottie.domain.CycleDayInfo
import com.fernandoyz.lottie.domain.CyclePhase
import com.fernandoyz.lottie.domain.DayMark
import com.fernandoyz.lottie.presentation.common.formatLongDate
import java.time.LocalDate

/** Detail of a calendar day, opened with a long press (day-detail S1-S4). Read-only for now. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailSheet(date: LocalDate, mark: DayMark?, info: CycleDayInfo?, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        DayDetailContent(date = date, mark = mark, info = info)
    }
}

@Composable
fun DayDetailContent(date: LocalDate, mark: DayMark?, info: CycleDayInfo?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = formatLongDate(date).replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        mark?.let { Text(text = stringResource(it.statusRes()), style = MaterialTheme.typography.bodyMedium) }

        if (info == null) {
            Text(text = stringResource(R.string.day_detail_no_cycle), style = MaterialTheme.typography.bodyLarge)
            return@Column
        }
        Text(text = stringResource(R.string.day_detail_cycle_day, info.cycleDay), style = MaterialTheme.typography.bodyLarge)
        Text(
            text = info.phase?.let { stringResource(R.string.day_detail_phase, stringResource(it.nameRes())) }
                ?: stringResource(R.string.day_detail_phase_unavailable),
            style = MaterialTheme.typography.bodyLarge,
        )
        if (info.inFertileWindow) {
            Text(text = stringResource(R.string.day_detail_fertile), style = MaterialTheme.typography.bodyLarge)
        }
        if (info.basedOnEstimate && info.phase != null && info.phase != CyclePhase.Menstrual) {
            Text(text = stringResource(R.string.day_detail_based_on_estimate), style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            text = stringResource(R.string.day_detail_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@StringRes
fun CyclePhase.nameRes(): Int = when (this) {
    CyclePhase.Menstrual -> R.string.phase_menstrual
    CyclePhase.Follicular -> R.string.phase_follicular
    CyclePhase.Ovulation -> R.string.phase_ovulation
    CyclePhase.Luteal -> R.string.phase_luteal
}

@StringRes
private fun DayMark.statusRes(): Int = when (this) {
    DayMark.Registered -> R.string.calendar_legend_registered
    DayMark.Upcoming -> R.string.calendar_legend_upcoming
    DayMark.Estimated -> R.string.calendar_legend_estimated
}
