package com.fernandoyz.lottie.data.dailylog

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One logged day (DAILY_LOG §6). Option sets are enum names joined by commas; the day is a
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
)
