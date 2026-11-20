package com.fernandoyz.lottie.presentation

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fernandoyz.lottie.R
import com.fernandoyz.lottie.domain.PeriodEstimate
import com.fernandoyz.lottie.domain.model.MenstruationRecord
import com.fernandoyz.lottie.presentation.calendar.CalendarScreen
import com.fernandoyz.lottie.presentation.calendar.CalendarUiState
import com.fernandoyz.lottie.presentation.common.formatLongDate
import com.fernandoyz.lottie.presentation.history.HistoryScreen
import com.fernandoyz.lottie.presentation.history.HistoryUiState
import com.fernandoyz.lottie.presentation.settings.SettingsScreen
import com.fernandoyz.lottie.presentation.settings.SettingsUiState
import com.fernandoyz.lottie.ui.theme.LottieTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** Synthetic dates only; never real user data. */
@RunWith(AndroidJUnit4::class)
class Stage3ScreensTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun text(@StringRes id: Int) = context.getString(id)

    private val today = LocalDate.of(2026, 6, 15)
    private val record = MenstruationRecord(1, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 4))

    private fun setHome(estimate: PeriodEstimate?) {
        composeRule.setContent {
            LottieTheme {
                HistoryScreen(
                    uiState = HistoryUiState(today = today, isLoading = false, records = listOf(record), estimate = estimate),
                    onAddRecord = {}, onOpenRecord = {}, onOpenCalendar = {}, onOpenSettings = {}, onMarkEnd = {}, onRetry = {},
                )
            }
        }
    }

    @Test
    fun homeShowsEstimateWithDisclaimerAndNavigationButtons() {
        setHome(PeriodEstimate(LocalDate.of(2026, 6, 29), LocalDate.of(2026, 7, 2), isPast = false))

        composeRule.onNodeWithText(text(R.string.estimate_title)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.estimate_disclaimer)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.estimate_past)).assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.home_open_calendar)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.home_open_settings)).assertIsDisplayed()
    }

    @Test
    fun pastEstimateShowsNote() {
        setHome(PeriodEstimate(LocalDate.of(2026, 6, 10), null, isPast = true))

        composeRule.onNodeWithText(text(R.string.estimate_past)).assertIsDisplayed()
    }

    @Test
    fun noEstimateHidesCard() {
        setHome(null)

        composeRule.onNodeWithText(text(R.string.estimate_title)).assertDoesNotExist()
    }

    @Test
    fun calendarMarksRegisteredAndEstimatedDaysWithLegend() {
        composeRule.setContent {
            LottieTheme {
                CalendarScreen(
                    uiState = CalendarUiState(
                        today = today,
                        isLoading = false,
                        records = listOf(record),
                        estimate = PeriodEstimate(LocalDate.of(2026, 6, 29), LocalDate.of(2026, 7, 2), false),
                    ),
                    onPreviousMonth = {},
                    onNextMonth = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription(
            context.getString(R.string.calendar_day_registered, formatLongDate(LocalDate.of(2026, 6, 2))),
        ).assertExists()
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.calendar_day_estimated, formatLongDate(LocalDate.of(2026, 6, 29))),
        ).assertExists()
        composeRule.onNodeWithText(text(R.string.calendar_legend_registered)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.calendar_legend_estimated)).assertIsDisplayed()
    }

    @Test
    fun settingsRejectsOutOfRangeAndAllowsValidChange() {
        composeRule.setContent {
            LottieTheme {
                SettingsScreen(
                    uiState = SettingsUiState(isLoading = false, savedCycleLengthDays = 28, cycleLengthInput = "100"),
                    onCycleLengthChanged = {},
                    onSave = {},
                )
            }
        }

        composeRule.onNodeWithText(text(R.string.setup_cycle_error_out_of_range), substring = true).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.settings_save)).assertIsNotEnabled()
    }

    @Test
    fun settingsAllowsSavingAValidNewValue() {
        composeRule.setContent {
            LottieTheme {
                SettingsScreen(
                    uiState = SettingsUiState(isLoading = false, savedCycleLengthDays = 28, cycleLengthInput = "30"),
                    onCycleLengthChanged = {},
                    onSave = {},
                )
            }
        }

        composeRule.onNodeWithText(text(R.string.settings_save)).assertIsEnabled()
    }
}
