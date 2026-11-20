package com.fernandoyz.lottie.domain

import com.fernandoyz.lottie.domain.model.MenstruationRecord
import java.time.LocalDate

enum class HistoryValidationError {
    StartInFuture,
    EndBeforeStart,
    Overlap,

    /** The candidate has no end date but a later record exists. */
    MissingEndForOlderRecord,

    /** The candidate would be the latest record while an earlier one has no end date. */
    PreviousRecordOpen,
}

/**
 * Validation rules between history records (STAGE_2 §3, approved 2026-11-20).
 */
object HistoryValidation {

    /**
     * Returns the first rule the candidate breaks, or null when it is valid.
     * [editingId] identifies the record being edited, so it is not compared with itself.
     */
    fun validate(
        start: LocalDate,
        end: LocalDate?,
        existing: List<MenstruationRecord>,
        today: LocalDate,
        editingId: Long? = null,
    ): HistoryValidationError? {
        if (start.isAfter(today)) return HistoryValidationError.StartInFuture
        if (end != null && end.isBefore(start)) return HistoryValidationError.EndBeforeStart
        // An end after today is allowed: auto-filled days may be in the future (STAGE_4 rule 2).

        val others = existing.filter { it.id != editingId }
        if (others.any { overlaps(start, end ?: start, it.startDate, it.endDate ?: it.startDate) }) {
            return HistoryValidationError.Overlap
        }

        return checkOnlyLatestIsOpen(start, end, others)
    }

    private fun overlaps(aStart: LocalDate, aEnd: LocalDate, bStart: LocalDate, bEnd: LocalDate): Boolean =
        !aStart.isAfter(bEnd) && !bStart.isAfter(aEnd)

    private fun checkOnlyLatestIsOpen(
        start: LocalDate,
        end: LocalDate?,
        others: List<MenstruationRecord>,
    ): HistoryValidationError? {
        val candidateIsLatest = others.none { it.startDate.isAfter(start) }
        if (end == null && !candidateIsLatest) return HistoryValidationError.MissingEndForOlderRecord
        if (candidateIsLatest && others.any { it.endDate == null }) return HistoryValidationError.PreviousRecordOpen
        return null
    }
}
