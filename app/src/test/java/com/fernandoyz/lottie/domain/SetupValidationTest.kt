package com.fernandoyz.lottie.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SetupValidationTest {

    private val today = LocalDate.of(2026, 11, 19)

    @Test
    fun `today is accepted as last menstruation date`() {
        assertTrue(SetupValidation.isLastMenstruationDateValid(today, today))
    }

    @Test
    fun `past date is accepted as last menstruation date`() {
        assertTrue(SetupValidation.isLastMenstruationDateValid(today.minusDays(10), today))
    }

    @Test
    fun `future date is rejected as last menstruation date`() {
        assertFalse(SetupValidation.isLastMenstruationDateValid(today.plusDays(1), today))
    }

    @Test
    fun `empty cycle length is rejected`() {
        assertEquals(CycleLengthValidation.Empty, SetupValidation.validateCycleLength(""))
        assertEquals(CycleLengthValidation.Empty, SetupValidation.validateCycleLength("   "))
    }

    @Test
    fun `non numeric cycle length is rejected`() {
        assertEquals(CycleLengthValidation.NotAWholeNumber, SetupValidation.validateCycleLength("abc"))
        assertEquals(CycleLengthValidation.NotAWholeNumber, SetupValidation.validateCycleLength("28.5"))
    }

    @Test
    fun `cycle length inside reference range is accepted without warning`() {
        assertEquals(CycleLengthValidation.Valid(21, outsideReferenceRange = false), SetupValidation.validateCycleLength("21"))
        assertEquals(CycleLengthValidation.Valid(28, outsideReferenceRange = false), SetupValidation.validateCycleLength(" 28 "))
        assertEquals(CycleLengthValidation.Valid(45, outsideReferenceRange = false), SetupValidation.validateCycleLength("45"))
    }

    @Test
    fun `cycle length inside technical limit but outside reference range is accepted with warning`() {
        assertEquals(CycleLengthValidation.Valid(1, outsideReferenceRange = true), SetupValidation.validateCycleLength("1"))
        assertEquals(CycleLengthValidation.Valid(20, outsideReferenceRange = true), SetupValidation.validateCycleLength("20"))
        assertEquals(CycleLengthValidation.Valid(46, outsideReferenceRange = true), SetupValidation.validateCycleLength("46"))
        assertEquals(CycleLengthValidation.Valid(99, outsideReferenceRange = true), SetupValidation.validateCycleLength("99"))
    }

    @Test
    fun `cycle length outside technical limit is rejected`() {
        assertEquals(CycleLengthValidation.OutOfRange, SetupValidation.validateCycleLength("0"))
        assertEquals(CycleLengthValidation.OutOfRange, SetupValidation.validateCycleLength("-5"))
        assertEquals(CycleLengthValidation.OutOfRange, SetupValidation.validateCycleLength("100"))
        assertEquals(CycleLengthValidation.OutOfRange, SetupValidation.validateCycleLength("99999999999"))
    }

    @Test
    fun `cycle length not longer than the bleeding is rejected`() {
        assertEquals(
            CycleLengthValidation.NotLongerThanBleeding(4),
            SetupValidation.validateCycleLength("4", longestBleedingDays = 4),
        )
        assertEquals(
            CycleLengthValidation.NotLongerThanBleeding(4),
            SetupValidation.validateCycleLength("3", longestBleedingDays = 4),
        )
    }

    @Test
    fun `cycle length longer than the bleeding is accepted`() {
        assertEquals(
            CycleLengthValidation.Valid(5, outsideReferenceRange = true),
            SetupValidation.validateCycleLength("5", longestBleedingDays = 4),
        )
        assertEquals(
            CycleLengthValidation.Valid(28, outsideReferenceRange = false),
            SetupValidation.validateCycleLength("28", longestBleedingDays = 4),
        )
    }
}
