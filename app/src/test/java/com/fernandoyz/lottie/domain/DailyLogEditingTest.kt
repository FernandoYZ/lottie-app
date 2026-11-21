package com.fernandoyz.lottie.domain

import com.fernandoyz.lottie.domain.model.DailyLog
import com.fernandoyz.lottie.domain.model.Discharge
import com.fernandoyz.lottie.domain.model.MenstrualFlow
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
}
