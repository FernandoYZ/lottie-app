package com.fernandoyz.lottie.domain.model

import java.time.LocalDate

/**
 * Values declared by the user during the initial setup.
 * [lastMenstruationDate] is a local calendar date, not an instant.
 */
data class InitialSetup(
    val lastMenstruationDate: LocalDate,
    val usualCycleLengthDays: Int,
    /** Optional end of that menstruation (added 2026-11-20); null when unknown or ongoing. */
    val lastMenstruationEndDate: LocalDate? = null,
)
