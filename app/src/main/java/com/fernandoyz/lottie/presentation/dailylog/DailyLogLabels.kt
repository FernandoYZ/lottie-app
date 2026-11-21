package com.fernandoyz.lottie.presentation.dailylog

import androidx.annotation.StringRes
import com.fernandoyz.lottie.R
import com.fernandoyz.lottie.domain.model.Digestion
import com.fernandoyz.lottie.domain.model.Discharge
import com.fernandoyz.lottie.domain.model.MenstrualFlow
import com.fernandoyz.lottie.domain.model.Mood
import com.fernandoyz.lottie.domain.model.SexActivity
import com.fernandoyz.lottie.domain.model.SexDrive
import com.fernandoyz.lottie.domain.model.Symptom

// Visible names of the DAILY_LOG §4 options, shared by the log screen and the day detail.

@StringRes
fun MenstrualFlow.labelRes(): Int = when (this) {
    MenstrualFlow.Light -> R.string.flow_light
    MenstrualFlow.Medium -> R.string.flow_medium
    MenstrualFlow.Heavy -> R.string.flow_heavy
}

@StringRes
fun Mood.labelRes(): Int = when (this) {
    Mood.Calm -> R.string.mood_calm
    Mood.Happy -> R.string.mood_happy
    Mood.Energetic -> R.string.mood_energetic
    Mood.Active -> R.string.mood_active
    Mood.MoodSwings -> R.string.mood_swings
    Mood.Irritated -> R.string.mood_irritated
    Mood.Sad -> R.string.mood_sad
    Mood.Anxious -> R.string.mood_anxious
    Mood.Depressed -> R.string.mood_depressed
    Mood.Guilty -> R.string.mood_guilty
    Mood.ObsessiveThoughts -> R.string.mood_obsessive_thoughts
    Mood.LowEnergy -> R.string.mood_low_energy
    Mood.Apathetic -> R.string.mood_apathetic
    Mood.Confused -> R.string.mood_confused
    Mood.SelfCritical -> R.string.mood_self_critical
}

@StringRes
fun Symptom.labelRes(): Int = when (this) {
    Symptom.AllGood -> R.string.symptom_all_good
    Symptom.Cramps -> R.string.symptom_cramps
    Symptom.TenderBreasts -> R.string.symptom_tender_breasts
    Symptom.Headache -> R.string.symptom_headache
    Symptom.Acne -> R.string.symptom_acne
    Symptom.BackPain -> R.string.symptom_back_pain
    Symptom.Fatigue -> R.string.symptom_fatigue
    Symptom.Cravings -> R.string.symptom_cravings
    Symptom.Insomnia -> R.string.symptom_insomnia
    Symptom.AbdominalPain -> R.string.symptom_abdominal_pain
    Symptom.VaginalItching -> R.string.symptom_vaginal_itching
    Symptom.VaginalDryness -> R.string.symptom_vaginal_dryness
}

@StringRes
fun Discharge.labelRes(): Int = when (this) {
    Discharge.None -> R.string.discharge_none
    Discharge.Creamy -> R.string.discharge_creamy
    Discharge.Watery -> R.string.discharge_watery
    Discharge.Sticky -> R.string.discharge_sticky
    Discharge.EggWhite -> R.string.discharge_egg_white
    Discharge.Spotting -> R.string.discharge_spotting
    Discharge.Unusual -> R.string.discharge_unusual
    Discharge.WhiteClumpy -> R.string.discharge_white_clumpy
    Discharge.Gray -> R.string.discharge_gray
}

@StringRes
fun Digestion.labelRes(): Int = when (this) {
    Digestion.Nausea -> R.string.digestion_nausea
    Digestion.Bloating -> R.string.digestion_bloating
    Digestion.Constipation -> R.string.digestion_constipation
    Digestion.Diarrhea -> R.string.digestion_diarrhea
}

@StringRes
fun SexActivity.labelRes(): Int = when (this) {
    SexActivity.None -> R.string.sex_none
    SexActivity.Protected -> R.string.sex_protected
    SexActivity.Unprotected -> R.string.sex_unprotected
    SexActivity.Oral -> R.string.sex_oral
    SexActivity.Anal -> R.string.sex_anal
    SexActivity.Masturbation -> R.string.sex_masturbation
    SexActivity.Touching -> R.string.sex_touching
    SexActivity.Toys -> R.string.sex_toys
    SexActivity.Orgasm -> R.string.sex_orgasm
}

@StringRes
fun SexDrive.labelRes(): Int = when (this) {
    SexDrive.High -> R.string.sex_drive_high
    SexDrive.Neutral -> R.string.sex_drive_neutral
    SexDrive.Low -> R.string.sex_drive_low
}
