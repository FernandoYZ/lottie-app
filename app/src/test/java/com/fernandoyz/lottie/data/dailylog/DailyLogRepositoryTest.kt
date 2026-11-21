package com.fernandoyz.lottie.data.dailylog

import com.fernandoyz.lottie.domain.model.DailyLog
import com.fernandoyz.lottie.domain.model.Digestion
import com.fernandoyz.lottie.domain.model.Discharge
import com.fernandoyz.lottie.domain.model.MenstrualFlow
import com.fernandoyz.lottie.domain.model.Mood
import com.fernandoyz.lottie.domain.model.OvulationTest
import com.fernandoyz.lottie.domain.model.PregnancyTest
import com.fernandoyz.lottie.domain.model.SexActivity
import com.fernandoyz.lottie.domain.model.SexDrive
import com.fernandoyz.lottie.domain.model.Symptom
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** DAILY_LOG §6. Synthetic data only; never real user data. */
class DailyLogRepositoryTest {

    private val dao = FakeDailyLogDao()
    private val repository = DailyLogRepository(dao)
    private val day = LocalDate.of(2026, 6, 15)

    private val full = DailyLog(
        date = day,
        flow = MenstrualFlow.Heavy,
        clots = true,
        moods = setOf(Mood.Calm, Mood.SelfCritical),
        symptoms = setOf(Symptom.Cramps, Symptom.VaginalDryness),
        discharge = setOf(Discharge.EggWhite),
        digestion = setOf(Digestion.Bloating),
        sex = setOf(SexActivity.Protected, SexActivity.Orgasm),
        sexDrive = SexDrive.Neutral,
        note = "Nota de prueba",
        pregnancyTest = PregnancyTest.FaintLine,
        ovulationTest = OvulationTest.Positive,
        waterMl = 1250,
        weightTenthsKg = 654,
        temperatureHundredthsC = 3652,
    )

    @Test
    fun `every field survives a save and read`() = runTest {
        repository.save(full)

        assertEquals(mapOf(day to full), repository.logs.first())
    }

    @Test
    fun `saving again replaces the day`() = runTest {
        repository.save(full)
        repository.save(full.copy(flow = MenstrualFlow.Light, moods = emptySet()))

        assertEquals(MenstrualFlow.Light, repository.logs.first().getValue(day).flow)
        assertEquals(1, dao.rows.value.size)
    }

    @Test
    fun `an empty day is not stored and clearing a day deletes it`() = runTest {
        repository.save(DailyLog(day))
        assertTrue(dao.rows.value.isEmpty())

        repository.save(full)
        repository.save(DailyLog(day, note = "  "))

        assertTrue(dao.rows.value.isEmpty())
    }

    @Test
    fun `unknown stored values are ignored`() = runTest {
        dao.upsert(DailyLogEntity(day.toEpochDay(), "Unknown", false, "Calm,Removed", "", "", "", "", null, ""))

        val log = repository.logs.first().getValue(day)

        assertEquals(null, log.flow)
        assertEquals(setOf(Mood.Calm), log.moods)
    }
}
