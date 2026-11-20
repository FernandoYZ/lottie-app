package com.fernandoyz.lottie.domain

import java.time.LocalDate

sealed interface CycleLengthValidation {
    data object Empty : CycleLengthValidation
    data object NotAWholeNumber : CycleLengthValidation
    data object OutOfRange : CycleLengthValidation

    /** Accepted value. [outsideReferenceRange] triggers an informative, non-blocking warning. */
    data class Valid(val days: Int, val outsideReferenceRange: Boolean) : CycleLengthValidation
}

/**
 * Validation rules for the initial setup (STAGE_1 §3, approved 2026-11-19).
 */
object SetupValidation {

    /** Hard technical limit: values outside it are rejected. */
    val CYCLE_LENGTH_LIMIT_DAYS = 1..99

    /** Reference range: values outside it are accepted with a warning. */
    val CYCLE_LENGTH_REFERENCE_DAYS = 21..45

    fun isLastMenstruationDateValid(date: LocalDate, today: LocalDate): Boolean = !date.isAfter(today)

    fun validateCycleLength(input: String): CycleLengthValidation {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return CycleLengthValidation.Empty

        val days = trimmed.toIntOrNull()
            ?: return if (trimmed.removePrefix("-").all(Char::isDigit)) {
                // Whole number too large to fit in an Int.
                CycleLengthValidation.OutOfRange
            } else {
                CycleLengthValidation.NotAWholeNumber
            }

        if (days !in CYCLE_LENGTH_LIMIT_DAYS) return CycleLengthValidation.OutOfRange
        return CycleLengthValidation.Valid(days, outsideReferenceRange = days !in CYCLE_LENGTH_REFERENCE_DAYS)
    }
}
