package com.fernandoyz.lottie.domain

import com.fernandoyz.lottie.domain.model.MenstruationRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/** Synthetic dates only; never real user data. */
class HistoryValidationTest {

    private val today = LocalDate.of(2026, 6, 15)
    private fun day(month: Int, dayOfMonth: Int) = LocalDate.of(2026, month, dayOfMonth)

    private val closedMarch = MenstruationRecord(id = 1, startDate = day(3, 1), endDate = day(3, 5))
    private val closedApril = MenstruationRecord(id = 2, startDate = day(4, 2), endDate = day(4, 6))

    private fun validate(
        start: LocalDate,
        end: LocalDate?,
        existing: List<MenstruationRecord> = listOf(closedMarch, closedApril),
        editingId: Long? = null,
    ) = HistoryValidation.validate(start, end, existing, today, editingId)

    // Rule 1
    @Test
    fun `start today is accepted`() = assertNull(validate(today, null))

    @Test
    fun `start in the future is rejected`() =
        assertEquals(HistoryValidationError.StartInFuture, validate(today.plusDays(1), null))

    // Rule 2
    @Test
    fun `end on the same day as start is accepted`() = assertNull(validate(day(5, 3), day(5, 3)))

    @Test
    fun `end before start is rejected`() =
        assertEquals(HistoryValidationError.EndBeforeStart, validate(day(5, 3), day(5, 2)))

    @Test
    fun `end in the future is accepted for auto-filled days`() =
        assertNull(validate(today, today.plusDays(5))) // STAGE_4 rule 2

    // Rule 3
    @Test
    fun `record overlapping an existing one is rejected`() =
        assertEquals(HistoryValidationError.Overlap, validate(day(3, 4), day(3, 8)))

    @Test
    fun `record sharing a single day with an existing one is rejected`() =
        assertEquals(HistoryValidationError.Overlap, validate(day(3, 5), day(3, 7)))

    @Test
    fun `record covering an existing one is rejected`() =
        assertEquals(HistoryValidationError.Overlap, validate(day(2, 25), day(3, 10)))

    @Test
    fun `record right after an existing one is accepted`() = assertNull(validate(day(3, 6), day(3, 8)))

    @Test
    fun `record between existing ones is accepted`() = assertNull(validate(day(3, 20), day(3, 24)))

    @Test
    fun `editing a record does not overlap with itself`() =
        assertNull(validate(day(3, 2), day(3, 6), editingId = closedMarch.id))

    @Test
    fun `editing a record into another one is rejected`() =
        assertEquals(HistoryValidationError.Overlap, validate(day(4, 1), day(4, 3), editingId = closedMarch.id))

    // Rule 4
    @Test
    fun `most recent record without end is accepted`() = assertNull(validate(day(5, 10), null))

    @Test
    fun `older record without end is rejected`() =
        assertEquals(HistoryValidationError.MissingEndForOlderRecord, validate(day(3, 20), null))

    @Test
    fun `new record while an earlier one is still open is rejected`() {
        val open = MenstruationRecord(id = 3, startDate = day(5, 10), endDate = null)

        assertEquals(
            HistoryValidationError.PreviousRecordOpen,
            validate(day(6, 8), day(6, 11), existing = listOf(closedMarch, closedApril, open)),
        )
    }

    @Test
    fun `open record overlapping by start is rejected as overlap`() {
        val open = MenstruationRecord(id = 3, startDate = day(5, 10), endDate = null)

        assertEquals(
            HistoryValidationError.Overlap,
            validate(day(5, 8), day(5, 12), existing = listOf(closedMarch, open)),
        )
    }

    @Test
    fun `closing the open record is accepted`() {
        val open = MenstruationRecord(id = 3, startDate = day(5, 10), endDate = null)

        assertNull(validate(day(5, 10), day(5, 13), existing = listOf(closedMarch, open), editingId = open.id))
    }

    @Test
    fun `first record in an empty history is accepted`() = assertNull(validate(day(1, 1), null, existing = emptyList()))

    // Calculations (STAGE_2 §4)
    @Test
    fun `duration counts both start and end days`() {
        assertEquals(5, closedMarch.durationDays)
        assertEquals(1, MenstruationRecord(id = 9, startDate = day(1, 1), endDate = day(1, 1)).durationDays)
    }

    @Test
    fun `duration is unknown while the record is open`() =
        assertNull(MenstruationRecord(id = 9, startDate = day(1, 1), endDate = null).durationDays)
}
