package com.fernandoyz.lottie.presentation.dailylog

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fernandoyz.lottie.R
import com.fernandoyz.lottie.domain.DailyLogEditing
import com.fernandoyz.lottie.domain.model.DailyLog
import com.fernandoyz.lottie.domain.model.Digestion
import com.fernandoyz.lottie.domain.model.Discharge
import com.fernandoyz.lottie.domain.model.MenstrualFlow
import com.fernandoyz.lottie.domain.model.Mood
import com.fernandoyz.lottie.domain.model.SexActivity
import com.fernandoyz.lottie.domain.model.SexDrive
import com.fernandoyz.lottie.domain.model.Symptom
import com.fernandoyz.lottie.presentation.common.formatLongDate

/** Stateful container: connects [DailyLogViewModel] with the stateless [DailyLogScreen]. */
@Composable
fun DailyLogRoute(viewModel: DailyLogViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DailyLogScreen(
        uiState = uiState,
        onBack = onBack,
        onPreviousDay = viewModel::onPreviousDay,
        onNextDay = viewModel::onNextDay,
        onRetry = viewModel::retry,
        onEdit = viewModel::onEdit,
        modifier = modifier,
    )
}

/** Log of one day with toggle chips per category (DAILY_LOG §3-§4). Every change is saved at once. */
@Composable
fun DailyLogScreen(
    uiState: DailyLogUiState,
    onBack: () -> Unit,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onRetry: () -> Unit,
    onEdit: ((DailyLog) -> DailyLog) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        IconButton(onClick = onBack, modifier = Modifier.padding(8.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.daily_log_back))
        }
        DayHeader(uiState, onPreviousDay, onNextDay)

        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            uiState.loadFailed -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(R.string.daily_log_load_error))
                Button(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
            }
            // key: a new day starts with its own note text.
            else -> key(uiState.date) { Categories(uiState, onEdit) }
        }
    }
}

@Composable
private fun DayHeader(uiState: DailyLogUiState, onPreviousDay: () -> Unit, onNextDay: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPreviousDay) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.daily_log_previous_day))
        }
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (uiState.date == uiState.today) {
                    stringResource(R.string.daily_log_today)
                } else {
                    formatLongDate(uiState.date).replaceFirstChar { it.uppercase() }
                },
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            uiState.cycleDay?.let {
                Text(
                    text = stringResource(R.string.day_detail_cycle_day, it),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onNextDay, enabled = uiState.canGoNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.daily_log_next_day))
        }
    }
}

@Composable
private fun Categories(uiState: DailyLogUiState, onEdit: ((DailyLog) -> DailyLog) -> Unit) {
    val log = uiState.log
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (uiState.writeFailed) {
            Text(stringResource(R.string.daily_log_write_error), color = MaterialTheme.colorScheme.error)
        }

        CategoryCard(R.string.daily_log_flow) {
            MenstrualFlow.entries.forEach { flow ->
                OptionChip(flow.labelRes(), log.flow == flow) { onEdit { DailyLogEditing.toggleFlow(it, flow) } }
            }
            OptionChip(R.string.flow_clots, log.clots) { onEdit(DailyLogEditing::toggleClots) }
        }
        CategoryCard(R.string.daily_log_mood) {
            Mood.entries.forEach { mood ->
                OptionChip(mood.labelRes(), mood in log.moods) { onEdit { DailyLogEditing.toggleMood(it, mood) } }
            }
        }
        CategoryCard(R.string.daily_log_symptoms) {
            Symptom.entries.forEach { symptom ->
                OptionChip(symptom.labelRes(), symptom in log.symptoms) { onEdit { DailyLogEditing.toggleSymptom(it, symptom) } }
            }
        }
        CategoryCard(R.string.daily_log_discharge) {
            Discharge.entries.forEach { discharge ->
                OptionChip(discharge.labelRes(), discharge in log.discharge) { onEdit { DailyLogEditing.toggleDischarge(it, discharge) } }
            }
        }
        CategoryCard(R.string.daily_log_digestion) {
            Digestion.entries.forEach { digestion ->
                OptionChip(digestion.labelRes(), digestion in log.digestion) { onEdit { DailyLogEditing.toggleDigestion(it, digestion) } }
            }
        }
        CategoryCard(R.string.daily_log_sex) {
            SexActivity.entries.forEach { activity ->
                OptionChip(activity.labelRes(), activity in log.sex) { onEdit { DailyLogEditing.toggleSex(it, activity) } }
            }
            SexDrive.entries.forEach { drive ->
                OptionChip(drive.labelRes(), log.sexDrive == drive) { onEdit { DailyLogEditing.toggleSexDrive(it, drive) } }
            }
        }
        NoteCard(log.note) { text -> onEdit { DailyLogEditing.withNote(it, text) } }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryCard(@StringRes title: Int, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { content() }
        }
    }
}

@Composable
private fun OptionChip(@StringRes label: Int, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(stringResource(label)) })
}

@Composable
private fun NoteCard(storedNote: String, onNoteChange: (String) -> Unit) {
    // Local text so typing is not interrupted while each change is saved.
    var text by remember { mutableStateOf(storedNote) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.daily_log_note),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it.take(DailyLogEditing.NOTE_MAX_LENGTH)
                    onNoteChange(text)
                },
                placeholder = { Text(stringResource(R.string.daily_log_note_hint)) },
                supportingText = {
                    Text(stringResource(R.string.daily_log_note_counter, text.length, DailyLogEditing.NOTE_MAX_LENGTH))
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
