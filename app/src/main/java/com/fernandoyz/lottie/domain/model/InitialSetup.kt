package com.fernandoyz.lottie.domain.model

import java.time.LocalDate

/**
 * The last menstruation marked during the initial setup (STAGE_4 §4).
 * Dates are local calendar days, not instants.
 */
data class InitialSetup(
    val lastMenstruationDate: LocalDate,
    /** End of that menstruation; null only in data saved by older versions. */
    val lastMenstruationEndDate: LocalDate? = null,
)
