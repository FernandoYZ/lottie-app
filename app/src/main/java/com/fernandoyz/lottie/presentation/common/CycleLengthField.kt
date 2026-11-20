package com.fernandoyz.lottie.presentation.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.fernandoyz.lottie.R
import com.fernandoyz.lottie.domain.CycleLengthValidation

/** Usual cycle length input with STAGE_1 §3 errors and reference range warning. Used by setup and settings. */
@Composable
fun CycleLengthField(
    input: String,
    error: CycleLengthValidation?,
    showWarning: Boolean,
    onValueChange: (String) -> Unit,
) {
    // Error or warning first; the hint that defines cycle length is always kept below it.
    val message = when (error) {
        CycleLengthValidation.Empty -> stringResource(R.string.setup_cycle_error_empty)
        CycleLengthValidation.NotAWholeNumber -> stringResource(R.string.setup_cycle_error_not_number)
        CycleLengthValidation.OutOfRange -> stringResource(R.string.setup_cycle_error_out_of_range)
        is CycleLengthValidation.NotLongerThanBleeding -> pluralStringResource(
            R.plurals.setup_cycle_error_not_longer_than_bleeding,
            error.bleedingDays,
            error.bleedingDays,
        )
        is CycleLengthValidation.Valid, null ->
            if (showWarning) stringResource(R.string.setup_cycle_warning_reference) else null
    }
    val hint = stringResource(R.string.setup_cycle_hint)
    val supportingText = if (message == null) hint else "$message\n$hint"

    OutlinedTextField(
        value = input,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.setup_cycle_label)) },
        suffix = { Text(stringResource(R.string.setup_cycle_suffix)) },
        supportingText = { Text(supportingText) },
        isError = error != null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}
