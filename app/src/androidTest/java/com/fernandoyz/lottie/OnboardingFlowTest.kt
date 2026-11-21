package com.fernandoyz.lottie

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fernandoyz.lottie.data.dailylog.DailyLogRepository
import com.fernandoyz.lottie.data.reminders.ReminderRepository
import com.fernandoyz.lottie.data.history.HistoryRepository
import com.fernandoyz.lottie.data.privacy.DataEraser
import com.fernandoyz.lottie.data.privacy.PrivacyRepository
import com.fernandoyz.lottie.data.history.LottieDatabase
import com.fernandoyz.lottie.data.repository.SetupRepository
import com.fernandoyz.lottie.domain.HistoryValidationError
import com.fernandoyz.lottie.domain.model.InitialSetup
import com.fernandoyz.lottie.domain.model.MenstruationRecord
import com.fernandoyz.lottie.presentation.common.formatLongDate
import com.fernandoyz.lottie.presentation.onboarding.setup.SetupScreen
import com.fernandoyz.lottie.presentation.onboarding.setup.SetupUiState
import com.fernandoyz.lottie.presentation.start.LottieApp
import com.fernandoyz.lottie.ui.theme.LottieTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

/** Synthetic dates only; never real user data. */
@RunWith(AndroidJUnit4::class)
class OnboardingFlowTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val dataStoreScope = CoroutineScope(Dispatchers.IO + Job())
    private val dataStoreFile = File(context.cacheDir, "onboarding_flow_test.preferences_pb").apply { delete() }
    private val dataStore = PreferenceDataStoreFactory.create(scope = dataStoreScope, produceFile = { dataStoreFile })
    private val repository = SetupRepository(dataStore)
    private val reminderRepository = ReminderRepository(dataStore)

    private val database = Room.inMemoryDatabaseBuilder(context, LottieDatabase::class.java).build()
    private val historyRepository = HistoryRepository(database.menstruationRecordDao(), repository)
    private val dailyLogRepository = DailyLogRepository(database.dailyLogDao())

    private val today = LocalDate.of(2026, 11, 19)
    private val zone = ZoneId.of("America/Lima")
    private val clock = Clock.fixed(today.atStartOfDay(zone).plusHours(10).toInstant(), zone)

    private fun text(@StringRes id: Int) = context.getString(id)

    @After
    fun tearDown() {
        database.close()
        dataStoreScope.cancel()
        dataStoreFile.delete()
    }

    private fun setApp() {
        composeRule.setContent { LottieTheme { LottieApp(repository, historyRepository, dailyLogRepository, reminderRepository, onRemindersChanged = {}, clock = clock, privacyRepository = PrivacyRepository(dataStore), dataEraser = DataEraser({}, dataStore, {})) } }
    }

    @Test
    fun freshInstall_showsWelcome_andStartOpensCalendarSetup() {
        setApp()

        composeRule.onNodeWithText(text(R.string.welcome_title)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.welcome_start)).performClick()

        composeRule.onNodeWithText(text(R.string.setup_title)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.setup_calendar_instructions)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.setup_save)).performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun completedSetup_skipsOnboarding() {
        runBlocking { repository.save(InitialSetup(today.minusDays(5), today.minusDays(2))) }

        setApp()

        composeRule.onNodeWithText(text(R.string.home_next_period).substringBefore('%').trim(), substring = true).assertIsDisplayed() // Home tab content
        composeRule.onNodeWithText(text(R.string.welcome_title)).assertDoesNotExist()
    }

    @Test
    fun tappingStart_fillsSixDays_andSavingOpensHome() {
        setApp()
        composeRule.onNodeWithText(text(R.string.welcome_start)).performClick()

        composeRule.onNodeWithContentDescription(formatLongDate(LocalDate.of(2026, 11, 2))).performClick()

        composeRule.onNodeWithContentDescription(
            context.getString(R.string.calendar_day_registered, formatLongDate(LocalDate.of(2026, 11, 7))),
        ).assertExists()
        composeRule.onNodeWithText(text(R.string.setup_save)).performScrollTo().assertIsEnabled().performClick()

        composeRule.onNodeWithText(text(R.string.home_next_period).substringBefore('%').trim(), substring = true).assertIsDisplayed() // Home tab content
    }

    @Test
    fun futureStart_showsError() {
        composeRule.setContent {
            LottieTheme {
                SetupScreen(
                    uiState = SetupUiState(
                        today = today,
                        rejection = HistoryValidationError.StartInFuture,
                    ),
                    onMonthChanged = {},
                    onDayTapped = {},
                    onConfirm = {},
                )
            }
        }

        composeRule.onNodeWithText(text(R.string.history_error_start_in_future)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.setup_save)).performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun saveFailure_showsError_andStaysOnSetup() {
        composeRule.setContent {
            LottieTheme {
                SetupScreen(
                    uiState = SetupUiState(today = today, menstruations = listOf(MenstruationRecord(1, today.minusDays(3), today)), saveFailed = true),
                    onMonthChanged = {},
                    onDayTapped = {},
                    onConfirm = {},
                )
            }
        }

        composeRule.onNodeWithText(text(R.string.setup_save_error)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.setup_title)).assertExists()
    }
}
