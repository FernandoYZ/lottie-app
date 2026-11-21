package com.fernandoyz.lottie.domain

import com.fernandoyz.lottie.domain.model.DailyLog
import com.fernandoyz.lottie.domain.model.Digestion
import com.fernandoyz.lottie.domain.model.Discharge
import com.fernandoyz.lottie.domain.model.MenstrualFlow
import com.fernandoyz.lottie.domain.model.Mood
import com.fernandoyz.lottie.domain.model.SexActivity
import com.fernandoyz.lottie.domain.model.SexDrive
import com.fernandoyz.lottie.domain.model.Symptom

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

    private fun <T> Set<T>.toggle(item: T, exclusive: T?): Set<T> = when {
        item in this -> this - item
        item == exclusive -> setOf(item)
        else -> this - setOfNotNull(exclusive) + item
    }
}
