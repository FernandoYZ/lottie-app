package com.fernandoyz.lottie.data.dailylog

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One logged day (DAILY_LOG §6, §8). Weight and temperature are integers: tenths of kg and hundredths of °C. Option sets are enum names joined by commas; the day is a
 * [java.time.LocalDate.toEpochDay] with no time zone.
 */
@Entity(tableName = "daily_logs")
data class DailyLogEntity(
    @PrimaryKey @ColumnInfo(name = "epoch_day") val epochDay: Long,
    @ColumnInfo(name = "menstrual_flow") val menstrualFlow: String?,
    @ColumnInfo(name = "clots") val clots: Boolean,
    @ColumnInfo(name = "moods") val moods: String,
    @ColumnInfo(name = "symptoms") val symptoms: String,
    @ColumnInfo(name = "discharge") val discharge: String,
    @ColumnInfo(name = "digestion") val digestion: String,
    @ColumnInfo(name = "sex") val sex: String,
    @ColumnInfo(name = "sex_drive") val sexDrive: String?,
    @ColumnInfo(name = "note") val note: String,
    // Added in version 3 (DAILY_LOG §8): nullable or with a default, so old rows stay valid.
    @ColumnInfo(name = "pregnancy_test") val pregnancyTest: String? = null,
    @ColumnInfo(name = "ovulation_test") val ovulationTest: String? = null,
    @ColumnInfo(name = "water_ml", defaultValue = "0") val waterMl: Int = 0,
    @ColumnInfo(name = "weight_tenths_kg") val weightTenthsKg: Int? = null,
    @ColumnInfo(name = "temperature_hundredths_c") val temperatureHundredthsC: Int? = null,
)
