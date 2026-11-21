package com.fernandoyz.lottie.presentation.dailylog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fernandoyz.lottie.R
import com.fernandoyz.lottie.domain.model.DailyLog

/** Logged options grouped by category, one line each, for the day detail (DAILY_LOG §5). */
@Composable
fun DailyLogSummary(log: DailyLog, modifier: Modifier = Modifier) {
    val flow = listOfNotNull(log.flow?.let { stringResource(it.labelRes()) }, if (log.clots) stringResource(R.string.flow_clots) else null)
    val sex = log.sex.map { stringResource(it.labelRes()) } + listOfNotNull(log.sexDrive?.let { stringResource(it.labelRes()) })
    val lines = listOf(
        R.string.daily_log_flow to flow,
        R.string.daily_log_mood to log.moods.map { stringResource(it.labelRes()) },
        R.string.daily_log_symptoms to log.symptoms.map { stringResource(it.labelRes()) },
        R.string.daily_log_discharge to log.discharge.map { stringResource(it.labelRes()) },
        R.string.daily_log_digestion to log.digestion.map { stringResource(it.labelRes()) },
        R.string.daily_log_sex to sex,
        R.string.daily_log_note to listOf(log.note.trim()).filter { it.isNotEmpty() },
    ).filter { it.second.isNotEmpty() }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        lines.forEach { (category, options) ->
            Text(
                text = stringResource(R.string.day_detail_log_item, stringResource(category), options.joinToString(", ")),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
