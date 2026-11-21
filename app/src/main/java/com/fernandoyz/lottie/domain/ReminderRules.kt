package com.fernandoyz.lottie.domain

import com.fernandoyz.lottie.domain.model.MenstruationRecord
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Stored by name in the settings: never rename the entries. */
enum class Reminder { PeriodSoon, FertileWindow, DailyLog }

/** All reminders start off at 09:00 (REMINDERS §2). */
data class ReminderSettings(
    val enabled: Set<Reminder> = emptySet(),
    val time: LocalTime = DEFAULT_TIME,
) {
    companion object {
        val DEFAULT_TIME: LocalTime = LocalTime.of(9, 0)
    }
}

/**
 * When each reminder is sent (REMINDERS §1, approved 2026-11-21). Uses the same estimate as Home
 * and Calendar; adds no rule of its own beyond the 2 days of notice.
 */
object ReminderRules {

    const val PERIOD_DAYS_BEFORE = 2L

    /** A run woken this close before the chosen time counts as that run; WorkManager does not run early, so 1 minute is enough. */
    private const val EARLY_MARGIN_MINUTES = 1L

    fun due(
        enabled: Set<Reminder>,
        records: List<MenstruationRecord>,
        manualCycleLengthDays: Int?,
        loggedToday: Boolean,
        today: LocalDate,
    ): Set<Reminder> {
        val estimate = PeriodEstimation.estimate(records, manualCycleLengthDays, today)
        val ongoing = records.any { !today.isBefore(it.startDate) && !today.isAfter(it.shownEnd(today)) }
        fun fertile(date: LocalDate) = CyclePhases.dayInfo(date, records, estimate, today)?.inFertileWindow == true

        return buildSet {
            if (estimate != null && !ongoing && estimate.start == today.plusDays(PERIOD_DAYS_BEFORE)) add(Reminder.PeriodSoon)
            if (fertile(today) && !fertile(today.minusDays(1))) add(Reminder.FertileWindow)
            if (!loggedToday) add(Reminder.DailyLog)
        }.intersect(enabled)
    }

    /** Next check at [time]: today if it is still ahead, otherwise tomorrow. */
    fun nextRun(now: LocalDateTime, time: LocalTime): LocalDateTime {
        val today = now.toLocalDate().atTime(time)
        return if (now.plusMinutes(EARLY_MARGIN_MINUTES).isBefore(today)) today else today.plusDays(1)
    }
}
