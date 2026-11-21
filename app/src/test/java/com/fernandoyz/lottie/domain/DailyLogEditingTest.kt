package com.fernandoyz.lottie.domain

import com.fernandoyz.lottie.domain.model.DailyLog
import com.fernandoyz.lottie.domain.model.Discharge
import com.fernandoyz.lottie.domain.model.MenstrualFlow
import com.fernandoyz.lottie.domain.model.OvulationTest
import com.fernandoyz.lottie.domain.model.PregnancyTest
import com.fernandoyz.lottie.domain.model.Mood
import com.fernandoyz.lottie.domain.model.SexActivity
import com.fernandoyz.lottie.domain.model.SexDrive
import com.fernandoyz.lottie.domain.model.Symptom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** DAILY_LOG §4. Synthetic data only; never real user data. */
class DailyLogEditingTest {

    private val empty = DailyLog(LocalDate.of(2026, 6, 15))

    @Test
    fun `flow is one level and tapping it again clears it`() {
        val light = DailyLogEditing.toggleFlow(empty, MenstrualFlow.Light)
        val heavy = DailyLogEditing.toggleFlow(light, MenstrualFlow.Heavy)

        assertEquals(MenstrualFlow.Light, light.flow)
        assertEquals(MenstrualFlow.Heavy, heavy.flow)
        assertNull(DailyLogEditing.toggleFlow(heavy, MenstrualFlow.Heavy).flow)
    }

    @Test
    fun `clots are independent of the flow level`() {
        val log = DailyLogEditing.toggleClots(DailyLogEditing.toggleFlow(empty, MenstrualFlow.Medium))

        assertTrue(log.clots)
        assertEquals(MenstrualFlow.Medium, log.flow)
        assertFalse(DailyLogEditing.toggleClots(log).clots)
    }

    @Test
    fun `several moods can be marked and unmarked`() {
        val log = DailyLogEditing.toggleMood(DailyLogEditing.toggleMood(empty, Mood.Calm), Mood.Anxious)

        assertEquals(setOf(Mood.Calm, Mood.Anxious), log.moods)
        assertEquals(setOf(Mood.Anxious), DailyLogEditing.toggleMood(log, Mood.Calm).moods)
    }

    @Test
    fun `all good clears the other symptoms and another symptom clears all good`() {
        val cramps = DailyLogEditing.toggleSymptom(DailyLogEditing.toggleSymptom(empty, Symptom.Cramps), Symptom.Headache)
        val allGood = DailyLogEditing.toggleSymptom(cramps, Symptom.AllGood)
        val acne = DailyLogEditing.toggleSymptom(allGood, Symptom.Acne)

        assertEquals(setOf(Symptom.AllGood), allGood.symptoms)
        assertEquals(setOf(Symptom.Acne), acne.symptoms)
    }

    @Test
    fun `no discharge and no sex are exclusive in their categories`() {
        val discharge = DailyLogEditing.toggleDischarge(DailyLogEditing.toggleDischarge(empty, Discharge.Creamy), Discharge.None)
        val sex = DailyLogEditing.toggleSex(DailyLogEditing.toggleSex(empty, SexActivity.None), SexActivity.Protected)

        assertEquals(setOf(Discharge.None), discharge.discharge)
        assertEquals(setOf(SexActivity.Protected), sex.sex)
    }

    @Test
    fun `sex drive is one level and tapping it again clears it`() {
        val high = DailyLogEditing.toggleSexDrive(empty, SexDrive.High)

        assertEquals(SexDrive.Low, DailyLogEditing.toggleSexDrive(high, SexDrive.Low).sexDrive)
        assertNull(DailyLogEditing.toggleSexDrive(high, SexDrive.High).sexDrive)
    }

    @Test
    fun `note keeps at most 500 characters`() {
        val log = DailyLogEditing.withNote(empty, "a".repeat(600))

        assertEquals(500, log.note.length)
    }

    @Test
    fun `a day without options nor note is empty`() {
        assertTrue(empty.isEmpty)
        assertTrue(DailyLogEditing.withNote(empty, "   ").isEmpty)
        assertFalse(DailyLogEditing.toggleClots(empty).isEmpty)
        assertFalse(DailyLogEditing.withNote(empty, "Nota").isEmpty)
    }

    @Test
    fun `tests are one option and tapping it again clears it`() {
        val positive = DailyLogEditing.togglePregnancyTest(empty, PregnancyTest.Positive)
        assertEquals(PregnancyTest.FaintLine, DailyLogEditing.togglePregnancyTest(positive, PregnancyTest.FaintLine).pregnancyTest)
        assertNull(DailyLogEditing.togglePregnancyTest(positive, PregnancyTest.Positive).pregnancyTest)

        val negative = DailyLogEditing.toggleOvulationTest(empty, OvulationTest.Negative)
        assertEquals(OvulationTest.Negative, negative.ovulationTest)
        assertNull(DailyLogEditing.toggleOvulationTest(negative, OvulationTest.Negative).ovulationTest)
        assertFalse(positive.isEmpty)
    }

    @Test
    fun `water goes up and down by 250 ml and never below zero`() {
        val twice = DailyLogEditing.addWater(DailyLogEditing.addWater(empty))
        assertEquals(500, twice.waterMl)
        assertEquals(250, DailyLogEditing.removeWater(twice).waterMl)
        assertEquals(0, DailyLogEditing.removeWater(empty).waterMl)
        assertTrue(DailyLogEditing.removeWater(DailyLogEditing.addWater(empty)).isEmpty)
    }

    @Test
    fun `weight accepts kg with one decimal between 20 and 300`() {
        assertEquals(ParsedValue.Valid(654), DailyLogEditing.parseWeight("65,4"))
        assertEquals(ParsedValue.Valid(650), DailyLogEditing.parseWeight("65.0"))
        assertEquals(ParsedValue.Valid(200), DailyLogEditing.parseWeight("20"))
        assertEquals(ParsedValue.Valid(3000), DailyLogEditing.parseWeight("300"))
        assertEquals(ParsedValue.Valid(null), DailyLogEditing.parseWeight("  "))
        listOf("19,9", "300,1", "65,45", "abc", "-70", "1e2", "+70", ",5").forEach { assertEquals(it, ParsedValue.Invalid, DailyLogEditing.parseWeight(it)) }
    }

    @Test
    fun `temperature accepts degrees with two decimals between 34 and 42`() {
        assertEquals(ParsedValue.Valid(3652), DailyLogEditing.parseTemperature("36,52"))
        assertEquals(ParsedValue.Valid(3400), DailyLogEditing.parseTemperature("34"))
        assertEquals(ParsedValue.Valid(4200), DailyLogEditing.parseTemperature("42.00"))
        assertEquals(ParsedValue.Valid(null), DailyLogEditing.parseTemperature(""))
        listOf("33,99", "42,01", "36,525", "36,5,2").forEach { assertEquals(it, ParsedValue.Invalid, DailyLogEditing.parseTemperature(it)) }
    }

    @Test
    fun `weight and temperature alone keep the day stored`() {
        assertFalse(empty.copy(weightTenthsKg = 654).isEmpty)
        assertFalse(empty.copy(temperatureHundredthsC = 3652).isEmpty)
    }
}
