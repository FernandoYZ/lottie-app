package com.fernandoyz.lottie.domain

import com.fernandoyz.lottie.domain.model.MenstruationRecord
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Text in the centre of the cycle ring (HOME §4). */
sealed interface HomeHeadline {
    data object NoRecords : HomeHeadline
    data class MenstruationDay(val day: Int) : HomeHeadline
    data class Delay(val days: Int) : HomeHeadline
    data object PeriodToday : HomeHeadline
    data object OvulationToday : HomeHeadline
    data class OvulationIn(val days: Int) : HomeHeadline
    data class PeriodIn(val days: Int) : HomeHeadline
}

/** Main Home button (HOME §5). */
sealed interface HomeAction {
    /** "Empezó mi menstruación": taps today on the calendar (STAGE_4 §2). */
    data object Start : HomeAction

    /** "Terminó mi menstruación": [record] ends today. */
    data class Finish(val record: MenstruationRecord) : HomeAction

    /** "Editar menstruación": the ongoing menstruation already ends today, so finishing would change nothing (HOME §7.1). */
    data object Edit : HomeAction
}

enum class RingKind { Menstrual, Follicular, Fertile, Luteal, Other }

/** [days] consecutive cycle days starting at the 0-based [firstDay]. */
data class RingSegment(val kind: RingKind, val firstDay: Int, val days: Int)

/** Current cycle drawn as a ring (HOME §3). [todayIndex] is 0-based and wraps on the estimated start. */
data class CycleRing(
    val lengthDays: Int,
    val todayIndex: Int,
    val segments: List<RingSegment>,
    /** Delay: the ring is drawn whole and grey, without phases. */
    val delayed: Boolean,
)

/**
 * Everything Home shows for [today] (HOME, approved 2026-11-21). Phases and fertile days come from
 * [CyclePhases], so the calendar and Home always agree.
 */
data class HomeStatus(
    val headline: HomeHeadline,
    val ring: CycleRing?,
    /** Day of the cycle started by the latest menstruation; null without records. */
    val cycleDay: Int?,
    /** Null during a delay, on the estimated start, or when the cycle is too short. */
    val phase: CyclePhase?,
    /** Current fertile window if it has not ended, otherwise the next cycle's one. */
    val nextFertileWindow: Pair<LocalDate, LocalDate>?,
    val action: HomeAction,
    /** Length of the current cycle, the M of "Día N de M del ciclo"; null during a delay (HOME §7.3). */
    val cycleLengthDays: Int? = null,
    /** Days until the estimated start; null once it has passed (HOME §7.4). */
    val daysToNextPeriod: Int? = null,
) {
    companion object {

        fun of(records: List<MenstruationRecord>, estimate: PeriodEstimate?, today: LocalDate): HomeStatus {
            val latest = records.maxByOrNull { it.startDate }
            if (latest == null || estimate == null) {
                return HomeStatus(HomeHeadline.NoRecords, null, null, null, null, HomeAction.Start)
            }

            val ongoing = records.firstOrNull { !today.isBefore(it.startDate) && !today.isAfter(it.shownEnd(today)) }
            val action = when {
                ongoing == null -> HomeAction.Start
                ongoing.endDate == today -> HomeAction.Edit
                else -> HomeAction.Finish(ongoing)
            }
            val cycleDay = days(latest.startDate, today) + 1
            val delayed = ongoing == null && today.isAfter(estimate.start)
            val length = days(latest.startDate, estimate.start)

            if (delayed || length <= 0) {
                val ring = if (length > 0) CycleRing(length, length - 1, emptyList(), delayed = true) else null
                val headline = ongoing?.let { HomeHeadline.MenstruationDay(days(it.startDate, today) + 1) }
                    ?: HomeHeadline.Delay(days(estimate.start, today))
                return HomeStatus(headline, ring, cycleDay, null, null, action, daysToNextPeriod = days(today, estimate.start).takeIf { it >= 0 })
            }

            val cycleDays = (0 until length).map { latest.startDate.plusDays(it.toLong()) }
            val infos = cycleDays.map { CyclePhases.dayInfo(it, records, estimate, today) }
            val ovulation = cycleDays.zip(infos).firstOrNull { it.second?.phase == CyclePhase.Ovulation }?.first

            val headline = when {
                ongoing != null -> HomeHeadline.MenstruationDay(days(ongoing.startDate, today) + 1)
                today == estimate.start -> HomeHeadline.PeriodToday
                today == ovulation -> HomeHeadline.OvulationToday
                ovulation != null && today.isBefore(ovulation) -> HomeHeadline.OvulationIn(days(today, ovulation))
                else -> HomeHeadline.PeriodIn(days(today, estimate.start))
            }
            val phase = if (today.isBefore(estimate.start)) CyclePhases.dayInfo(today, records, estimate, today)?.phase else null

            return HomeStatus(
                headline = headline,
                ring = CycleRing(length, days(latest.startDate, today) % length, segments(infos), delayed = false),
                cycleDay = cycleDay,
                phase = phase,
                nextFertileWindow = nextFertileWindow(cycleDays, infos, estimate, length, records, today),
                action = action,
                cycleLengthDays = length,
                daysToNextPeriod = days(today, estimate.start),
            )
        }

        private fun segments(infos: List<CycleDayInfo?>): List<RingSegment> {
            val kinds = infos.map { info ->
                when {
                    info?.phase == CyclePhase.Menstrual -> RingKind.Menstrual
                    info?.inFertileWindow == true -> RingKind.Fertile
                    info?.phase == CyclePhase.Follicular -> RingKind.Follicular
                    info?.phase == CyclePhase.Luteal -> RingKind.Luteal
                    else -> RingKind.Other
                }
            }
            val segments = mutableListOf<RingSegment>()
            kinds.forEachIndexed { index, kind ->
                val last = segments.lastOrNull()
                if (last != null && last.kind == kind) {
                    segments[segments.lastIndex] = last.copy(days = last.days + 1)
                } else {
                    segments += RingSegment(kind, firstDay = index, days = 1)
                }
            }
            return segments
        }

        private fun nextFertileWindow(
            cycleDays: List<LocalDate>,
            infos: List<CycleDayInfo?>,
            estimate: PeriodEstimate,
            length: Int,
            records: List<MenstruationRecord>,
            today: LocalDate,
        ): Pair<LocalDate, LocalDate>? {
            val current = cycleDays.filterIndexed { index, _ -> infos[index]?.inFertileWindow == true }
            if (current.isNotEmpty() && !current.last().isBefore(today)) return current.first() to current.last()
            val next = (0 until length)
                .map { estimate.start.plusDays(it.toLong()) }
                .filter { CyclePhases.dayInfo(it, records, estimate, today)?.inFertileWindow == true }
            return if (next.isEmpty()) null else next.first() to next.last()
        }

        private fun days(from: LocalDate, to: LocalDate): Int = ChronoUnit.DAYS.between(from, to).toInt()
    }
}
