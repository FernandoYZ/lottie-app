package com.fernandoyz.lottie.data.history

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.fernandoyz.lottie.data.dailylog.DailyLogDao
import com.fernandoyz.lottie.data.dailylog.DailyLogEntity

/**
 * Version 2 adds `daily_logs` (DAILY_LOG §6). The automatic migration only creates that table:
 * `menstruation_records` is not touched. Version 3 adds nullable columns (and `water_ml` with default 0) to
 * `daily_logs` (DAILY_LOG §8); existing rows keep their values. Version 4 adds the nullable `mood_level` and `energy_level` (CYCLE_SUMMARY §1). Exported schemas live in `app/schemas/`.
 */
@Database(
    entities = [MenstruationRecordEntity::class, DailyLogEntity::class],
    version = 4,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3), AutoMigration(from = 3, to = 4)],
)
abstract class LottieDatabase : RoomDatabase() {
    abstract fun menstruationRecordDao(): MenstruationRecordDao

    abstract fun dailyLogDao(): DailyLogDao
}
