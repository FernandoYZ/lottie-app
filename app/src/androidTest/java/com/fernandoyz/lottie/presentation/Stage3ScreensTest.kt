package com.fernandoyz.lottie.presentation

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fernandoyz.lottie.R
import com.fernandoyz.lottie.domain.CycleDayInfo
import com.fernandoyz.lottie.domain.CyclePhase
import com.fernandoyz.lottie.domain.PeriodEstimate
import com.fernandoyz.lottie.domain.Reminder
import com.fernandoyz.lottie.presentation.calendar.DayDetailContent
import com.fernandoyz.lottie.domain.model.DailyLog
import com.fernandoyz.lottie.domain.model.MenstruationRecord
import com.fernandoyz.lottie.domain.model.Mood
import com.fernandoyz.lottie.presentation.calendar.CalendarScreen
import com.fernandoyz.lottie.presentation.calendar.CalendarUiState
import com.fernandoyz.lottie.presentation.common.formatLongDate
import com.fernandoyz.lottie.presentation.home.HomeScreen
import com.fernandoyz.lottie.presentation.history.HistoryUiState
import com.fernandoyz.lottie.presentation.settings.SettingsScreen
import com.fernandoyz.lottie.presentation.settings.SettingsUiState
import com.fernandoyz.lottie.ui.theme.LottieTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** Estimate, calendar and settings screens (STAGE_3, STAGE_4). Synthetic dates only; never real user data. */
@RunWith(AndroidJUnit4::class)
class Stage3ScreensTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun text(@StringRes id: Int) = context.getString(id)

    private val today = LocalDate.of(2026, 6, 15)
    private val record = MenstruationRecord(1, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 4))
    private val estimate = PeriodEstimate(LocalDate.of(2026, 6, 29), LocalDate.of(2026, 7, 2), isPast = false, isApproximate = false)

    private fun setHome(estimate: PeriodEstimate?) {
        composeRule.setContent {
            LottieTheme {
                HomeScreen(
                    uiState = HistoryUiState(today = today, isLoading = false, records = listOf(record), estimate = estimate),
                    onRetry = {},
                    onMainAction = {},
                    onConfirmOneDay = {},
                    onDismissOneDay = {},
                    onOpenDailyLog = {},
                )
            }
        }
    }

    @Test
    fun homeShowsTodayPhaseAndEstimateWithDisclaimer() {
        setHome(estimate)

        composeRule.onNodeWithText(text(R.string.estimate_title)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.estimate_disclaimer)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.estimate_past)).assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.estimate_approximate)).assertDoesNotExist()
        composeRule.onNodeWithText(context.getString(R.string.home_phase_day, text(R.string.phase_ovulation).lowercase(), 15)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.home_headline_ovulation_today)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.home_action_start)).assertIsDisplayed()
    }

    @Test
    fun pastAndApproximateEstimateShowNotes() {
        setHome(estimate.copy(isPast = true, isApproximate = true))

        composeRule.onNodeWithText(text(R.string.estimate_past)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.estimate_approximate)).assertIsDisplayed()
    }

    @Test
    fun noEstimateHidesCard() {
        setHome(null)

        composeRule.onNodeWithText(text(R.string.estimate_title)).assertDoesNotExist()
    }

    private fun setCalendar(state: CalendarUiState, onDayTapped: (LocalDate) -> Unit = {}, onEditToggled: () -> Unit = {}) {
        composeRule.setContent {
            LottieTheme {
                CalendarScreen(
                    uiState = state,
                    onMonthChanged = {},
                    onDayTapped = onDayTapped,
                    onConfirmDelete = {},
                    onDismissDelete = {},
                    onDayLongPressed = {},
                    onDismissDayDetail = {},
                    onEditToggled = onEditToggled,
                )
            }
        }
    }

    @Test
    fun calendarMarksRegisteredUpcomingAndEstimatedDaysWithLegend() {
        val current = MenstruationRecord(2, LocalDate.of(2026, 6, 14), LocalDate.of(2026, 6, 17))
        setCalendar(CalendarUiState(today = today, isLoading = false, records = listOf(record, current), estimate = estimate))

        composeRule.onNodeWithContentDescription(
            context.getString(R.string.calendar_day_registered, formatLongDate(LocalDate.of(2026, 6, 2))),
        ).assertExists()
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.calendar_day_upcoming, formatLongDate(LocalDate.of(2026, 6, 16))),
        ).assertExists()
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.calendar_day_estimated, formatLongDate(LocalDate.of(2026, 6, 29))),
        ).assertExists()
        composeRule.onNodeWithContentDescription(text(R.string.calendar_legend_action)).performClick()
        composeRule.onNodeWithText(text(R.string.calendar_legend_registered)).assertExists()
        composeRule.onNodeWithText(text(R.string.calendar_legend_upcoming)).assertExists()
        composeRule.onNodeWithText(text(R.string.calendar_legend_estimated)).assertExists()
    }

    @Test
    fun tappingADaySendsThatDate() {
        var tapped: LocalDate? = null
        setCalendar(CalendarUiState(today = today, isLoading = false), onDayTapped = { tapped = it })

        composeRule.onNodeWithContentDescription(formatLongDate(LocalDate.of(2026, 6, 10))).performClick()

        assertEquals(LocalDate.of(2026, 6, 10), tapped)
    }

    @Test
    fun calendarMarksTodayAndTogglesEditMode() {
        var toggled = false
        setCalendar(CalendarUiState(today = today, isLoading = false), onEditToggled = { toggled = true })

        composeRule.onNodeWithContentDescription(context.getString(R.string.calendar_day_today, formatLongDate(today))).assertExists()
        composeRule.onNodeWithText(text(R.string.calendar_edit_hint)).assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.calendar_edit)).performClick()
        assertEquals(true, toggled)
    }

    @Test
    fun calendarInEditModeShowsTheHintAndDone() {
        setCalendar(CalendarUiState(today = today, isLoading = false, isEditing = true))

        composeRule.onNodeWithText(text(R.string.calendar_edit_hint)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.calendar_edit_done)).assertExists()
    }

    @Test
    fun tappingAStartDayAsksForConfirmation() {
        setCalendar(CalendarUiState(today = today, isLoading = false, records = listOf(record), pendingDelete = record))

        composeRule.onNodeWithText(text(R.string.calendar_delete_title)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.calendar_delete_confirm)).assertIsDisplayed()
    }

    @Test
    fun dayDetailShowsCycleDayPhaseAndDisclaimer() {
        composeRule.setContent {
            LottieTheme {
                DayDetailContent(
                    date = LocalDate.of(2026, 6, 15),
                    mark = null,
                    info = CycleDayInfo(cycleDay = 15, phase = CyclePhase.Ovulation, basedOnEstimate = true),
                )
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.day_detail_cycle_day, 15)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.day_detail_phase, text(R.string.phase_ovulation))).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.day_detail_based_on_estimate)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.day_detail_disclaimer)).assertIsDisplayed()
    }

    @Test
    fun dayDetailShowsTheDailyLogAndOpensIt() {
        var opened = false
        composeRule.setContent {
            LottieTheme {
                DayDetailContent(
                    date = LocalDate.of(2026, 6, 10),
                    mark = null,
                    info = null,
                    log = DailyLog(LocalDate.of(2026, 6, 10), moods = setOf(Mood.Calm)),
                    onOpenDailyLog = { opened = true },
                )
            }
        }

        composeRule.onNodeWithText(
            context.getString(R.string.day_detail_log_item, text(R.string.daily_log_mood), text(R.string.mood_calm)),
        ).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.day_detail_log_action)).performClick()
        assertEquals(true, opened)
    }

    @Test
    fun settingsRejectsOutOfRange() {
        composeRule.setContent {
            LottieTheme {
                SettingsScreen(
                    uiState = SettingsUiState(isLoading = false, cycleLengthInput = "100"),
                    onCycleLengthChanged = {},
                    onSave = {},
                )
            }
        }

        composeRule.onNodeWithText(text(R.string.setup_cycle_error_out_of_range), substring = true).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.settings_save)).assertIsNotEnabled()
    }

    @Test
    fun settingsShowsRemindersOffAndSendsTheToggle() {
        var toggled: Pair<Reminder, Boolean>? = null
        composeRule.setContent {
            LottieTheme {
                SettingsScreen(
                    uiState = SettingsUiState(isLoading = false, notificationPermissionDenied = true),
                    onCycleLengthChanged = {},
                    onSave = {},
                    onReminderToggled = { reminder, enabled -> toggled = reminder to enabled },
                )
            }
        }

        composeRule.onNodeWithText(text(R.string.reminders_title)).assertExists()
        composeRule.onNodeWithText("09:00", substring = true).assertExists()
        composeRule.onNodeWithText(text(R.string.reminders_permission_denied)).assertExists()
        composeRule.onNodeWithText(text(R.string.reminders_daily_log)).performClick()
        assertEquals(Reminder.DailyLog to true, toggled)
    }

    @Test
    fun settingsShowsLearnedLengthAndAllowsSavingAManualValue() {
        composeRule.setContent {
            LottieTheme {
                SettingsScreen(
                    uiState = SettingsUiState(isLoading = false, learnedCycleLengthDays = 29, learnedIsDefault = false, cycleLengthInput = "30"),
                    onCycleLengthChanged = {},
                    onSave = {},
                )
            }
        }

        composeRule.onNodeWithText(context.resources.getQuantityString(R.plurals.settings_cycle_learned, 29, 29)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.settings_save)).assertIsEnabled()
    }
}
