package com.fernandoyz.lottie.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fernandoyz.lottie.R
import com.fernandoyz.lottie.ui.theme.LottieTheme

/** Placeholder home for Stage 1 (STAGE_1 §6). No predictions or dashboard yet. */
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = stringResource(R.string.home_title), style = MaterialTheme.typography.headlineMedium)
        Text(text = stringResource(R.string.home_body), style = MaterialTheme.typography.bodyLarge)
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    LottieTheme { HomeScreen() }
}
