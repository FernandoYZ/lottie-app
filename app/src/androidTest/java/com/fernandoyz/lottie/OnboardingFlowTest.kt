package com.fernandoyz.lottie

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.room.Room
import com.fernandoyz.lottie.data.history.HistoryRepository
import com.fernandoyz.lottie.data.history.LottieDatabase
import com.fernandoyz.lottie.data.repository.SetupRepository
import com.fernandoyz.lottie.domain.model.InitialSetup
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

@RunWith(AndroidJUnit4::class)
class OnboardingFlowTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val dataStoreScope = CoroutineScope(Dispatchers.IO + Job())
    private val dataStoreFile = File(context.cacheDir, "onboarding_flow_test.preferences_pb").apply { delete() }
    private val repository = SetupRepository(
        PreferenceDataStoreFactory.create(scope = dataStoreScope, produceFile = { dataStoreFile }),
    )

    private val database = Room.inMemoryDatabaseBuilder(context, LottieDatabase::class.java).build()
    private val historyRepository = HistoryRepository(database.menstruationRecordDao(), repository)

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
        composeRule.setContent { LottieTheme { LottieApp(setupRepository = repository, historyRepository = historyRepository, clock = clock) } }
    }

    @Test
    fun freshInstall_showsWelcome_andStartOpensSetupWithBothFields() {
        setApp()

        composeRule.onNodeWithText(text(R.string.welcome_title)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.welcome_start)).performClick()

        composeRule.onNodeWithText(text(R.string.setup_title)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.setup_date_label)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.setup_cycle_label)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.setup_save)).assertIsNotEnabled()
    }

    @Test
    fun completedSetup_skipsOnboarding() {
        runBlocking { repository.save(InitialSetup(today.minusDays(5), 28)) }

        setApp()

        composeRule.onNodeWithText(text(R.string.home_title)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.welcome_title)).assertDoesNotExist()
    }

    @Test
    fun invalidCycleLength_showsError_andCannotConfirm() {
        setApp()
        composeRule.onNodeWithText(text(R.string.welcome_start)).performClick()

        composeRule.onNodeWithText(text(R.string.setup_cycle_label)).performTextInput("abc")

        composeRule.onNodeWithText(text(R.string.setup_cycle_error_not_number), substring = true).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.setup_save)).assertIsNotEnabled()
    }

    @Test
    fun futureDate_showsError_andCannotConfirm() {
        composeRule.setContent {
            LottieTheme {
                SetupScreen(
                    uiState = SetupUiState(
                        today = today,
                        lastMenstruationDate = today.plusDays(1),
                        cycleLengthInput = "28",
                        cycleLengthEdited = true,
                    ),
                    onDateSelected = {},
                    onEndDateSelected = {},
                    onEndDateCleared = {},
                    onCycleLengthChanged = {},
                    onConfirm = {},
                )
            }
        }

        composeRule.onNodeWithText(text(R.string.setup_date_error_future)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.setup_save)).assertIsNotEnabled()
    }

    @Test
    fun cycleLengthOutsideReferenceRange_showsWarning_andCanConfirm() {
        composeRule.setContent {
            LottieTheme {
                SetupScreen(
                    uiState = SetupUiState(
                        today = today,
                        lastMenstruationDate = today,
                        cycleLengthInput = "50",
                        cycleLengthEdited = true,
                    ),
                    onDateSelected = {},
                    onEndDateSelected = {},
                    onEndDateCleared = {},
                    onCycleLengthChanged = {},
                    onConfirm = {},
                )
            }
        }

        composeRule.onNodeWithText(text(R.string.setup_cycle_warning_reference), substring = true).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.setup_cycle_hint), substring = true).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.setup_save)).assertIsEnabled()
    }

    @Test
    fun saveFailure_showsError_andStaysOnSetup() {
        composeRule.setContent {
            LottieTheme {
                SetupScreen(
                    uiState = SetupUiState(
                        today = today,
                        lastMenstruationDate = today,
                        cycleLengthInput = "28",
                        cycleLengthEdited = true,
                        saveFailed = true,
                    ),
                    onDateSelected = {},
                    onEndDateSelected = {},
                    onEndDateCleared = {},
                    onCycleLengthChanged = {},
                    onConfirm = {},
                )
            }
        }

        composeRule.onNodeWithText(text(R.string.setup_save_error)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.setup_title)).assertIsDisplayed()
    }
}
