package com.fernandoyz.lottie.data.history

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Dates are calendar days stored as [java.time.LocalDate.toEpochDay], with no time zone. */
@Entity(
    tableName = "menstruation_records",
    indices = [Index(value = ["start_epoch_day"], unique = true)],
)
data class MenstruationRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "start_epoch_day") val startEpochDay: Long,
    @ColumnInfo(name = "end_epoch_day") val endEpochDay: Long?,
)
