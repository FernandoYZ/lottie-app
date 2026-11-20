package com.fernandoyz.lottie.domain

import com.fernandoyz.lottie.domain.model.MenstruationRecord
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/**
 * Values learned from the user's own history (STAGE_4 §3, approved 2026-11-20).
 * Means are rounded half up; defaults apply while there is not enough data.
 */
object CycleStatistics {

    const val DEFAULT_CYCLE_LENGTH_DAYS = 28
    const val DEFAULT_PERIOD_LENGTH_DAYS = 6

    /** Mean days between consecutive starts; null with fewer than two records. */
    fun averageCycleLengthDays(records: List<MenstruationRecord>): Int? {
        val starts = records.map { it.startDate }.sorted()
        if (starts.size < 2) return null
        return starts.zipWithNext { a, b -> ChronoUnit.DAYS.between(a, b) }.average().roundToInt()
    }

    /**
     * Mean duration of finished records. A record whose end is after [today] still holds auto-filled
     * days that may be unmarked, so it is not counted yet.
     */
    fun averagePeriodLengthDays(records: List<MenstruationRecord>, today: LocalDate): Int? {
        val durations = records
            .filter { it.endDate != null && !it.endDate.isAfter(today) }
            .mapNotNull { it.durationDays }
        return if (durations.isEmpty()) null else durations.average().roundToInt()
    }

    /** Days filled when a start is tapped, and the estimated duration. */
    fun periodLengthDays(records: List<MenstruationRecord>, today: LocalDate): Int =
        averagePeriodLengthDays(records, today) ?: DEFAULT_PERIOD_LENGTH_DAYS
}
