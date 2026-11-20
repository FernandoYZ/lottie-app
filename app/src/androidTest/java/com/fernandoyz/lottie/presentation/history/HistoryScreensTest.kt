package com.fernandoyz.lottie.presentation.history

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fernandoyz.lottie.R
import com.fernandoyz.lottie.domain.model.MenstruationRecord
import com.fernandoyz.lottie.ui.theme.LottieTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    private val open = MenstruationRecord(2, LocalDate.of(2026, 6, 12), null)
    private val closed = MenstruationRecord(1, LocalDate.of(2026, 5, 14), LocalDate.of(2026, 5, 17))

    private fun setHistory(state: HistoryUiState, onOpen: (Long) -> Unit = {}) {
        composeRule.setContent {
            LottieTheme {
                HistoryScreen(uiState = state, onAddRecord = {}, onOpenRecord = onOpen, onMarkEnd = {}, onRetry = {})
            }
        }
    }

    @Test
    fun historyShowsOpenRecordDurationAndMarkEnd() {
        setHistory(HistoryUiState(today = today, isLoading = false, records = listOf(open, closed)))

        composeRule.onNodeWithText(text(R.string.history_ongoing)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.history_mark_end)).assertIsDisplayed()
        composeRule.onNodeWithText(context.resources.getQuantityString(R.plurals.history_duration_days, 4, 4))
            .assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.history_add)).assertIsDisplayed()
    }

    @Test
    fun emptyHistoryShowsEmptyMessage() {
        setHistory(HistoryUiState(today = today, isLoading = false))

        composeRule.onNodeWithText(text(R.string.history_empty)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.history_mark_end)).assertDoesNotExist()
    }

    @Test
    fun tappingRecordOpensIt() {
        var opened: Long? = null
        setHistory(HistoryUiState(today = today, isLoading = false, records = listOf(closed)), onOpen = { opened = it })

        composeRule.onNodeWithText(context.resources.getQuantityString(R.plurals.history_duration_days, 4, 4))
            .performClick()

        assertEquals(closed.id, opened)
    }

    @Test
    fun formShowsBrokenRuleAndDisablesSave() {
        composeRule.setContent {
            LottieTheme {
                RecordFormScreen(
                    uiState = RecordFormUiState(
                        today = today,
                        isLoading = false,
                        startDate = LocalDate.of(2026, 5, 15),
                        endDate = LocalDate.of(2026, 5, 18),
                        records = listOf(closed),
                    ),
                    onStartSelected = {}, onEndSelected = {}, onEndCleared = {}, onSave = {}, onDelete = {},
                )
            }
        }

        composeRule.onNodeWithText(text(R.string.history_error_overlap)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.record_save)).assertIsNotEnabled()
    }

    @Test
    fun deleteAsksForConfirmationBeforeDeleting() {
        var deleted = false
        composeRule.setContent {
            LottieTheme {
                RecordFormScreen(
                    uiState = RecordFormUiState(today = today, recordId = 1, isLoading = false, startDate = closed.startDate),
                    onStartSelected = {}, onEndSelected = {}, onEndCleared = {}, onSave = {}, onDelete = { deleted = true },
                )
            }
        }

        composeRule.onNodeWithText(text(R.string.record_delete)).performClick()
        composeRule.onNodeWithText(text(R.string.record_delete_confirm_title)).assertIsDisplayed()
        assertTrue(!deleted)
    }
}
