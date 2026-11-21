package com.fernandoyz.lottie.domain

import com.fernandoyz.lottie.domain.model.DailyLog
import com.fernandoyz.lottie.domain.model.Digestion
import com.fernandoyz.lottie.domain.model.Discharge
import com.fernandoyz.lottie.domain.model.MenstrualFlow
import com.fernandoyz.lottie.domain.model.OvulationTest
import com.fernandoyz.lottie.domain.model.PregnancyTest
import com.fernandoyz.lottie.domain.model.Mood
import com.fernandoyz.lottie.domain.model.SexActivity
import com.fernandoyz.lottie.domain.model.SexDrive
import com.fernandoyz.lottie.domain.model.Symptom
import java.math.BigDecimal

/** Result of reading a typed number: [Valid] holds the stored integer, or null when the field is empty. */
sealed interface ParsedValue {
    data class Valid(val value: Int?) : ParsedValue
    data object Invalid : ParsedValue
}

/**
 * What tapping a chip does (DAILY_LOG §4). Single-choice values clear when tapped again.
 * "Todo va bien", "Nada de flujo" and "No he practicado sexo" exclude the rest of their category.
 */
object DailyLogEditing {

    const val NOTE_MAX_LENGTH = 500

    fun toggleFlow(log: DailyLog, flow: MenstrualFlow) = log.copy(flow = if (log.flow == flow) null else flow)

    fun toggleClots(log: DailyLog) = log.copy(clots = !log.clots)

    fun toggleMood(log: DailyLog, mood: Mood) = log.copy(moods = log.moods.toggle(mood, exclusive = null))

    fun toggleSymptom(log: DailyLog, symptom: Symptom) = log.copy(symptoms = log.symptoms.toggle(symptom, Symptom.AllGood))

    fun toggleDischarge(log: DailyLog, discharge: Discharge) = log.copy(discharge = log.discharge.toggle(discharge, Discharge.None))

    fun toggleDigestion(log: DailyLog, digestion: Digestion) = log.copy(digestion = log.digestion.toggle(digestion, exclusive = null))

    fun toggleSex(log: DailyLog, activity: SexActivity) = log.copy(sex = log.sex.toggle(activity, SexActivity.None))

    fun toggleSexDrive(log: DailyLog, drive: SexDrive) = log.copy(sexDrive = if (log.sexDrive == drive) null else drive)

    fun withNote(log: DailyLog, note: String) = log.copy(note = note.take(NOTE_MAX_LENGTH))

    // DAILY_LOG §8.
    const val WATER_STEP_ML = 250

    fun togglePregnancyTest(log: DailyLog, test: PregnancyTest) = log.copy(pregnancyTest = if (log.pregnancyTest == test) null else test)

    fun toggleOvulationTest(log: DailyLog, test: OvulationTest) = log.copy(ovulationTest = if (log.ovulationTest == test) null else test)

    fun addWater(log: DailyLog) = log.copy(waterMl = log.waterMl + WATER_STEP_ML)

    fun removeWater(log: DailyLog) = log.copy(waterMl = (log.waterMl - WATER_STEP_ML).coerceAtLeast(0))

    /** Kg with up to one decimal, 20 to 300, as tenths of kg. */
    fun parseWeight(text: String): ParsedValue = parse(text, decimals = 1, min = BigDecimal(20), max = BigDecimal(300))

    /** °C with up to two decimals, 34 to 42, as hundredths of °C. */
    fun parseTemperature(text: String): ParsedValue = parse(text, decimals = 2, min = BigDecimal(34), max = BigDecimal(42))

    /** Accepts a comma or a dot as decimal separator. */
    private fun parse(text: String, decimals: Int, min: BigDecimal, max: BigDecimal): ParsedValue {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return ParsedValue.Valid(null)
        val number = trimmed.replace(',', '.').toBigDecimalOrNull() ?: return ParsedValue.Invalid
        if (number.stripTrailingZeros().scale() > decimals || number < min || number > max) return ParsedValue.Invalid
        return ParsedValue.Valid(number.movePointRight(decimals).intValueExact())
    }

    private fun <T> Set<T>.toggle(item: T, exclusive: T?): Set<T> = when {
        item in this -> this - item
        item == exclusive -> setOf(item)
        else -> this - setOfNotNull(exclusive) + item
    }
}
