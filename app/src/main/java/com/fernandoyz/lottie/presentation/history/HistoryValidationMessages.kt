package com.fernandoyz.lottie.presentation.history

import androidx.annotation.StringRes
import com.fernandoyz.lottie.R
import com.fernandoyz.lottie.domain.HistoryValidationError

@StringRes
fun HistoryValidationError.messageRes(): Int = when (this) {
    HistoryValidationError.StartInFuture -> R.string.history_error_start_in_future
    HistoryValidationError.EndBeforeStart -> R.string.history_error_end_before_start
    HistoryValidationError.EndInFuture -> R.string.history_error_end_in_future
    HistoryValidationError.Overlap -> R.string.history_error_overlap
    HistoryValidationError.MissingEndForOlderRecord -> R.string.history_error_missing_end
    HistoryValidationError.PreviousRecordOpen -> R.string.history_error_previous_open
}
