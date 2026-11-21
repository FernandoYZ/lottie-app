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
) {
    /** Nothing marked and no note: the day is not stored (DAILY_LOG §6). */
    val isEmpty: Boolean
        get() = flow == null && !clots && moods.isEmpty() && symptoms.isEmpty() && discharge.isEmpty() &&
            digestion.isEmpty() && sex.isEmpty() && sexDrive == null && note.isBlank()
}
