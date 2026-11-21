package com.fernandoyz.lottie.presentation.start

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fernandoyz.lottie.R
import com.fernandoyz.lottie.core.navigation.LottieDestinations
import com.fernandoyz.lottie.core.navigation.LottieNavHost
import com.fernandoyz.lottie.data.dailylog.DailyLogRepository
import com.fernandoyz.lottie.data.history.HistoryRepository
import com.fernandoyz.lottie.data.repository.SetupRepository
import java.time.Clock

/** App root: waits for the persisted setup before choosing the start destination. */
@Composable
fun LottieApp(
    setupRepository: SetupRepository,
    historyRepository: HistoryRepository,
    dailyLogRepository: DailyLogRepository,
    clock: Clock,
) {
    val startViewModel: StartViewModel = viewModel(factory = StartViewModel.factory(setupRepository))
    val startState by startViewModel.startState.collectAsStateWithLifecycle()

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)
        when (startState) {
            StartState.Loading -> LoadingContent(contentModifier)
            StartState.Error -> ErrorContent(onRetry = startViewModel::retry, modifier = contentModifier)
            StartState.NeedsOnboarding -> LottieNavHost(
                startDestination = LottieDestinations.WELCOME,
                setupRepository = setupRepository,
                historyRepository = historyRepository,
                dailyLogRepository = dailyLogRepository,
                clock = clock,
                modifier = contentModifier,
            )
            StartState.SetupCompleted -> LottieNavHost(
                startDestination = LottieDestinations.HOME,
                setupRepository = setupRepository,
                historyRepository = historyRepository,
                dailyLogRepository = dailyLogRepository,
                clock = clock,
                modifier = contentModifier,
            )
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.start_loading)
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = description })
    }
}

@Composable
private fun ErrorContent(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.start_load_error))
        Button(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
    }
}
