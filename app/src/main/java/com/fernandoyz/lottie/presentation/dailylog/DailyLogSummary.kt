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
        R.string.daily_log_pregnancy_test to listOfNotNull(log.pregnancyTest?.let { stringResource(it.labelRes()) }),
        R.string.daily_log_ovulation_test to listOfNotNull(log.ovulationTest?.let { stringResource(it.labelRes()) }),
        R.string.daily_log_water to listOf(log.waterMl).filter { it > 0 }.map { stringResource(R.string.daily_log_water_amount, formatDecimal(it, 3)) },
        R.string.daily_log_weight to listOfNotNull(log.weightTenthsKg?.let { stringResource(R.string.daily_log_weight_value, formatDecimal(it, 1)) }),
        R.string.daily_log_temperature to listOfNotNull(
            log.temperatureHundredthsC?.let { stringResource(R.string.daily_log_temperature_value, formatDecimal(it, 2)) },
        ),
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
