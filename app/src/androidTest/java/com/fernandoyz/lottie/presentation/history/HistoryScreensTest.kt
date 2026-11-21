package com.fernandoyz.lottie.presentation.history

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fernandoyz.lottie.R
import com.fernandoyz.lottie.domain.SummaryRange
import com.fernandoyz.lottie.domain.model.DailyLog
import com.fernandoyz.lottie.domain.model.MenstruationRecord
import com.fernandoyz.lottie.domain.model.Symptom
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
    fun historyShowsAveragesAndOneRowPerCycle() {
        setHistory(HistoryUiState(today = today, isLoading = false, records = listOf(current, closed)))

        composeRule.onNodeWithText(context.resources.getQuantityString(R.plurals.stats_average_cycle, 31, 31)).assertIsDisplayed()
        composeRule.onNodeWithText(context.resources.getQuantityString(R.plurals.stats_average_period, 4, 4)).assertIsDisplayed()
        composeRule.onNodeWithText(
            context.resources.getQuantityString(
                R.plurals.stats_past_cycle, 31, formatShortDate(closed.startDate), formatShortDate(LocalDate.of(2026, 6, 13)), 31,
            ),
        ).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.stats_current_since, formatShortDate(current.startDate))).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.stats_menstruation_ongoing)).assertIsDisplayed()
        composeRule.onNodeWithText(context.resources.getQuantityString(R.plurals.stats_menstruation, 4, 4)).assertIsDisplayed()
    }

    @Test
    fun emptyHistoryShowsEmptyMessage() {
        setHistory(HistoryUiState(today = today, isLoading = false))

        composeRule.onNodeWithText(text(R.string.history_empty)).assertIsDisplayed()
    }

    @Test
    fun cycleSummaryShowsLoggedDaysAndTheMostLoggedOption() {
        val log = DailyLog(LocalDate.of(2026, 5, 15), symptoms = setOf(Symptom.Cramps), moodLevel = 4)
        composeRule.setContent {
            LottieTheme {
                CycleSummaryScreen(
                    uiState = HistoryUiState(today = today, isLoading = false, records = listOf(current, closed), logs = mapOf(log.date to log)),
                    start = closed.startDate,
                    range = SummaryRange.WholeCycle,
                    onCycleChanged = {},
                    onRangeChanged = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText(context.resources.getQuantityString(R.plurals.summary_logged_days, 31, 1, 31)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.summary_top_item, text(R.string.symptom_cramps), 1)).assertExists()
    }
}
