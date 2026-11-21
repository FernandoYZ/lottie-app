package com.fernandoyz.lottie.domain.model

import java.time.LocalDate

// Option lists of DAILY_LOG §4 (approved 2026-11-21). Enum names are stored in Room: never rename them.

enum class MenstrualFlow { Light, Medium, Heavy }

enum class Mood {
    Calm, Happy, Energetic, Active, MoodSwings, Irritated, Sad, Anxious, Depressed, Guilty,
    ObsessiveThoughts, LowEnergy, Apathetic, Confused, SelfCritical,
}

enum class Symptom {
    AllGood, Cramps, TenderBreasts, Headache, Acne, BackPain, Fatigue, Cravings, Insomnia,
    AbdominalPain, VaginalItching, VaginalDryness,
}

enum class Discharge { None, Creamy, Watery, Sticky, EggWhite, Spotting, Unusual, WhiteClumpy, Gray }

enum class Digestion { Nausea, Bloating, Constipation, Diarrhea }

enum class SexActivity { None, Protected, Unprotected, Oral, Anal, Masturbation, Touching, Toys, Orgasm }

enum class SexDrive { High, Neutral, Low }

// DAILY_LOG §8 (approved 2026-11-21).
enum class PregnancyTest { NotTaken, Positive, Negative, FaintLine }

enum class OvulationTest { NotTaken, Positive, Negative }

/** What the user logged for one calendar day. Independent from menstruation records (DAILY_LOG §4). */
data class DailyLog(
    val date: LocalDate,
    val flow: MenstrualFlow? = null,
    val clots: Boolean = false,
    val moods: Set<Mood> = emptySet(),
    val symptoms: Set<Symptom> = emptySet(),
    val discharge: Set<Discharge> = emptySet(),
    val digestion: Set<Digestion> = emptySet(),
    val sex: Set<SexActivity> = emptySet(),
    val sexDrive: SexDrive? = null,
    val note: String = "",
    val pregnancyTest: PregnancyTest? = null,
    val ovulationTest: OvulationTest? = null,
    /** Water in ml, in steps of 250 (DAILY_LOG §8). */
    val waterMl: Int = 0,
    /** Weight in tenths of kg: 654 is 65,4 kg. Integers avoid rounding errors. */
    val weightTenthsKg: Int? = null,
    /** Basal temperature in hundredths of °C: 3652 is 36,52 °C. */
    val temperatureHundredthsC: Int? = null,
    /** "¿Cómo te sientes?" (CYCLE_SUMMARY §1): 1 (muy bajo) to 5 (muy bueno), null when not logged. */
    val moodLevel: Int? = null,
    /** 1 (muy baja) to 5 (muy alta), null when not logged. */
    val energyLevel: Int? = null,
) {
    /** Nothing marked and no note: the day is not stored (DAILY_LOG §6). */
    val isEmpty: Boolean
        get() = flow == null && !clots && moods.isEmpty() && symptoms.isEmpty() && discharge.isEmpty() &&
            digestion.isEmpty() && sex.isEmpty() && sexDrive == null && note.isBlank() &&
            pregnancyTest == null && ovulationTest == null && waterMl == 0 && weightTenthsKg == null && temperatureHundredthsC == null &&
            moodLevel == null && energyLevel == null
}
