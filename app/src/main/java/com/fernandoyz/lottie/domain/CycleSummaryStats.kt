package com.fernandoyz.lottie.domain

import com.fernandoyz.lottie.domain.model.DailyLog
import com.fernandoyz.lottie.domain.model.Digestion
import com.fernandoyz.lottie.domain.model.Discharge
import com.fernandoyz.lottie.domain.model.MenstrualFlow
import com.fernandoyz.lottie.domain.model.MenstruationRecord
import com.fernandoyz.lottie.domain.model.Mood
import com.fernandoyz.lottie.domain.model.OvulationTest
import com.fernandoyz.lottie.domain.model.PregnancyTest
import com.fernandoyz.lottie.domain.model.SexActivity
import com.fernandoyz.lottie.domain.model.SexDrive
import com.fernandoyz.lottie.domain.model.Symptom
import java.time.LocalDate

/** Tabs of the cycle summary (CYCLE_SUMMARY §2). */
enum class SummaryRange { WholeCycle, Menstrual, Follicular, Ovulation, Luteal }

/** "Coágulos de sangre" is a checkbox, not an enum value, so it gets its own key in the ranking. */
enum class ExtraOption { Clots }

data class DayLevels(val date: LocalDate, val mood: Int?, val energy: Int?)

data class RangeSummary(
    /** Days of the range up to today, in order. */
    val days: List<LocalDate>,
    val loggedDays: Int,
    /** One entry per day of [days]; null levels where nothing was logged. */
    val levels: List<DayLevels>,
    /** Up to [CycleSummaryStats.TOP_OPTIONS] options with how many days they were marked, highest first. */
    val topOptions: List<Pair<Any, Int>>,
)

/**
 * The user's own logs of one cycle, whole or by phase (CYCLE_SUMMARY §2). Counts only: no comparisons,
 * no interpretations. Phases come from [CyclePhases.dayInfo], as in the day detail.
 */
object CycleSummaryStats {

    const val TOP_OPTIONS = 5

    fun summarize(
        cycle: CycleSummary,
        range: SummaryRange,
        records: List<MenstruationRecord>,
        estimate: PeriodEstimate?,
        logs: Map<LocalDate, DailyLog>,
        today: LocalDate,
    ): RangeSummary {
        val last = minOf(cycle.end ?: today, today)
        val cycleDays = generateSequence(cycle.start) { it.plusDays(1) }.takeWhile { !it.isAfter(last) }.toList()
        val days = if (range == SummaryRange.WholeCycle) {
            cycleDays
        } else {
            cycleDays.filter { CyclePhases.dayInfo(it, records, estimate, today)?.phase == range.phase() }
        }
        val dayLogs = days.mapNotNull { logs[it] }

        // Stable sort: ties keep the log screen order built by options().
        val counts = linkedMapOf<Any, Int>()
        dayLogs.forEach { log -> options(log).forEach { counts[it] = (counts[it] ?: 0) + 1 } }
        val ranked = counts.entries.sortedWith(compareByDescending<Map.Entry<Any, Int>> { it.value }.thenBy { order(it.key) })

        return RangeSummary(
            days = days,
            loggedDays = dayLogs.size,
            levels = days.map { DayLevels(it, logs[it]?.moodLevel, logs[it]?.energyLevel) },
            topOptions = ranked.take(TOP_OPTIONS).map { it.key to it.value },
        )
    }

    private fun SummaryRange.phase(): CyclePhase? = when (this) {
        SummaryRange.WholeCycle -> null
        SummaryRange.Menstrual -> CyclePhase.Menstrual
        SummaryRange.Follicular -> CyclePhase.Follicular
        SummaryRange.Ovulation -> CyclePhase.Ovulation
        SummaryRange.Luteal -> CyclePhase.Luteal
    }

    /** Marked options of one day, in the log screen order (DAILY_LOG §4, §8). Levels are charted, not ranked. */
    private fun options(log: DailyLog): List<Any> = buildList {
        log.flow?.let(::add)
        if (log.clots) add(ExtraOption.Clots)
        addAll(log.moods.sortedBy { it.ordinal })
        addAll(log.symptoms.sortedBy { it.ordinal })
        addAll(log.discharge.sortedBy { it.ordinal })
        addAll(log.digestion.sortedBy { it.ordinal })
        addAll(log.sex.sortedBy { it.ordinal })
        log.sexDrive?.let(::add)
        log.pregnancyTest?.let(::add)
        log.ovulationTest?.let(::add)
    }

    private val CATEGORY_ORDER = listOf(
        MenstrualFlow::class, ExtraOption::class, Mood::class,
        Symptom::class, Discharge::class,
        Digestion::class, SexActivity::class,
        SexDrive::class, PregnancyTest::class,
        OvulationTest::class,
    )

    private fun order(option: Any): Int = CATEGORY_ORDER.indexOf(option::class) * 100 + (option as Enum<*>).ordinal
}
