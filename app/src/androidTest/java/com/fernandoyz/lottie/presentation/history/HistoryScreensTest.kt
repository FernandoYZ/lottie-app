package com.fernandoyz.lottie.presentation.history

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fernandoyz.lottie.R
import com.fernandoyz.lottie.domain.model.MenstruationRecord
import com.fernandoyz.lottie.presentation.common.formatShortDate
import com.fernandoyz.lottie.ui.theme.LottieTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** Synthetic dates only; never real user data. */
@RunWith(AndroidJUnit4::class)
class HistoryScreensTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun text(@StringRes id: Int) = context.getString(id)

    private val today = LocalDate.of(2026, 6, 15)
    private val current = MenstruationRecord(2, LocalDate.of(2026, 6, 14), LocalDate.of(2026, 6, 19))
    private val closed = MenstruationRecord(1, LocalDate.of(2026, 5, 14), LocalDate.of(2026, 5, 17))

    private fun setHistory(state: HistoryUiState) {
        composeRule.setContent {
            LottieTheme {
                HistoryScreen(uiState = state, onRetry = {})
            }
        }
    }

    @Test
    fun historyShowsDurationAndDaysStillExpected() {
        setHistory(HistoryUiState(today = today, isLoading = false, records = listOf(current, closed)))

        composeRule.onNodeWithText(context.resources.getQuantityString(R.plurals.history_duration_days, 4, 4))
            .assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.history_in_progress_until, formatShortDate(current.endDate!!)))
            .assertIsDisplayed()
    }

    @Test
    fun emptyHistoryShowsEmptyMessage() {
        setHistory(HistoryUiState(today = today, isLoading = false))

        composeRule.onNodeWithText(text(R.string.history_empty)).assertIsDisplayed()
    }
}
